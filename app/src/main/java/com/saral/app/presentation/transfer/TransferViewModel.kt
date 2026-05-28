package com.saral.app.presentation.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saral.app.domain.models.Beneficiary
import com.saral.app.domain.usecases.GetBalanceUseCase
import com.saral.app.domain.usecases.GetBeneficiariesUseCase
import com.saral.app.domain.usecases.TransferMoneyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val getBeneficiariesUseCase: GetBeneficiariesUseCase,
    private val transferMoneyUseCase: TransferMoneyUseCase,
    private val getBalanceUseCase: GetBalanceUseCase
) : ViewModel() {

    private val _step = MutableStateFlow<TransferStep>(TransferStep.SelectingBeneficiary)
    val step: StateFlow<TransferStep> = _step.asStateFlow()

    private val _assistantMessage = MutableStateFlow("")
    val assistantMessage: StateFlow<String> = _assistantMessage.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _beneficiaries = MutableStateFlow<List<Beneficiary>>(emptyList())
    val beneficiaries: StateFlow<List<Beneficiary>> = _beneficiaries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var speakCallback: ((String) -> Unit)? = null
    private var introSpoken = false

    init {
        viewModelScope.launch {
            _beneficiaries.value = getBeneficiariesUseCase()
        }
    }

    fun setSpeakCallback(callback: (String) -> Unit) {
        speakCallback = callback
    }

    // Called once from the composable's LaunchedEffect when beneficiaries are available
    fun onScreenLoad() {
        if (introSpoken || _beneficiaries.value.isEmpty()) return
        introSpoken = true
        val bens = _beneficiaries.value
        val listText = bens.mapIndexed { i, b ->
            "${i + 1}. ${b.name}, ${b.bankName}"
        }.joinToString(". ")
        speak("Would you like to transfer money to one of your beneficiaries? Here is your list. $listText. Please say the name of the beneficiary.")
    }

    fun onVoiceInput(text: String) {
        if (text.isBlank()) return
        _recognizedText.value = text
        when (val current = _step.value) {
            is TransferStep.SelectingBeneficiary -> handleBeneficiarySelection(text)
            is TransferStep.EnterAmount -> handleAmountInput(text, current.beneficiary)
            is TransferStep.ConfirmTransfer -> handleConfirmation(text, current)
            else -> { /* AwaitingBiometric, Complete, Failed — ignore voice */ }
        }
    }

    fun onBiometricSuccess() {
        val current = _step.value
        if (current is TransferStep.AwaitingBiometric) {
            executeTransfer(TransferStep.ConfirmTransfer(current.beneficiary, current.amount))
        }
    }

    fun onBiometricFailed() {
        val current = _step.value
        if (current is TransferStep.AwaitingBiometric) {
            speak("Authentication failed. Please tap the fingerprint button to try again.")
        }
    }

    private fun handleBeneficiarySelection(text: String) {
        val matched = matchBeneficiary(text, _beneficiaries.value)
        if (matched == null) {
            speak("Sorry, I could not find that beneficiary. Please say one of the names from the list.")
            return
        }
        _step.value = TransferStep.EnterAmount(matched)
        speak("You selected ${matched.name}, ${matched.bankName} account ending ${matched.accountLast4}. How much would you like to transfer?")
    }

    private fun handleAmountInput(text: String, beneficiary: Beneficiary) {
        val amount = parseAmount(text)
        if (amount == null || amount <= 0.0) {
            speak("Sorry, I could not understand the amount. Please say the amount. For example, say 500 rupees.")
            return
        }
        _step.value = TransferStep.ConfirmTransfer(beneficiary, amount)
        val formatted = formatAmount(amount)
        speak("You are about to transfer $formatted rupees to ${beneficiary.name}, ${beneficiary.bankName} account ending ${beneficiary.accountLast4}. Say yes to confirm or no to cancel.")
    }

    private fun handleConfirmation(text: String, confirmState: TransferStep.ConfirmTransfer) {
        val lower = text.lowercase().trim()
        val isYes = lower in setOf("yes", "yeah", "yep", "haan", "ha", "ji", "confirm", "ok", "okay", "proceed") ||
                lower.startsWith("yes ") || lower.startsWith("haan ")
        val isNo = lower in setOf("no", "nope", "cancel", "nahi", "stop") ||
                lower.startsWith("no ") || lower.startsWith("nahi ")
        when {
            isYes -> {
                _step.value = TransferStep.AwaitingBiometric(confirmState.beneficiary, confirmState.amount)
                speak("Please authenticate with your fingerprint to authorize this transfer.")
            }
            isNo -> {
                speak("Transfer cancelled. Please say a beneficiary name to start a new transfer.")
                _step.value = TransferStep.SelectingBeneficiary
            }
            else -> speak("Please say yes to confirm or no to cancel the transfer.")
        }
    }

    private fun executeTransfer(state: TransferStep.ConfirmTransfer) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = transferMoneyUseCase(state.amount, state.beneficiary.name)
            if (result.success) {
                val account = getBalanceUseCase()
                val amountFormatted = formatAmount(state.amount)
                val balanceFormatted = formatAmount(account.balance)
                _step.value = TransferStep.Complete(
                    beneficiary = state.beneficiary,
                    amount = state.amount,
                    remainingBalance = account.balance,
                    txnId = result.txnId
                )
                speak(
                    "Transfer successful! $amountFormatted rupees have been sent to ${state.beneficiary.name}. " +
                    "Your account balance is now $balanceFormatted rupees. " +
                    "Transaction reference number ${result.txnId}."
                )
            } else {
                _step.value = TransferStep.Failed
                speak("Transfer failed. Please try again later.")
            }
            _isLoading.value = false
        }
    }

    fun reset() {
        _step.value = TransferStep.SelectingBeneficiary
        _recognizedText.value = ""
        _assistantMessage.value = ""
        introSpoken = false
    }

    private fun matchBeneficiary(text: String, beneficiaries: List<Beneficiary>): Beneficiary? {
        val lower = text.lowercase()
        // Full name match first, then first-name match (min 3 chars to avoid false positives)
        return beneficiaries.firstOrNull { b ->
            lower.contains(b.name.lowercase())
        } ?: beneficiaries.firstOrNull { b ->
            b.name.split(" ").any { part -> part.length > 2 && lower.contains(part.lowercase()) }
        }
    }

    private val amountRegex = Regex("(\\d+(?:,\\d+)*(?:\\.\\d+)?)")

    private fun parseAmount(text: String): Double? =
        amountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) String.format("%,d", amount.toLong())
        else String.format("%,.2f", amount)

    private fun speak(text: String) {
        _assistantMessage.value = text
        speakCallback?.invoke(text)
    }
}
