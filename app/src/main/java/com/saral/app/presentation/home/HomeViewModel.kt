package com.saral.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saral.app.domain.models.Transaction
import com.saral.app.domain.models.VoiceIntent
import com.saral.app.domain.usecases.GetBalanceUseCase
import com.saral.app.domain.usecases.GetRecentTransactionsUseCase
import com.saral.app.domain.usecases.RequestChequeBookUseCase
import com.saral.app.domain.usecases.TransferMoneyUseCase
import com.saral.app.voice.VoiceIntentParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val responseText: String = "",
    val isProcessing: Boolean = false,
    val recentCommands: List<String> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val showTransactions: Boolean = false,
    val awaitingTransferConfirmation: Boolean = false,
    val pendingTransferAmount: Double = 0.0,
    val pendingTransferRecipient: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val transferMoneyUseCase: TransferMoneyUseCase,
    private val requestChequeBookUseCase: RequestChequeBookUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val intentParser: VoiceIntentParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var speakCallback: ((String) -> Unit)? = null

    fun setSpeakCallback(callback: (String) -> Unit) {
        speakCallback = callback
    }

    fun onVoiceResult(text: String) {
        if (text.isBlank()) return

        _uiState.update { it.copy(recognizedText = text, isProcessing = true) }
        addRecentCommand(text)

        val intent = intentParser.parse(text)
        handleIntent(intent)
    }

    private fun handleIntent(intent: VoiceIntent) {
        viewModelScope.launch {
            when (intent) {
                is VoiceIntent.CheckBalance -> handleCheckBalance()
                is VoiceIntent.TransferMoney -> handleTransfer(intent)
                is VoiceIntent.RequestChequeBook -> handleChequeBook()
                is VoiceIntent.RecentTransactions -> handleRecentTransactions()
                is VoiceIntent.Help -> handleHelp()
                is VoiceIntent.ConfirmYes -> handleConfirmYes()
                is VoiceIntent.ConfirmNo -> handleConfirmNo()
                is VoiceIntent.Unknown -> handleUnknown()
            }
        }
    }

    private suspend fun handleCheckBalance() {
        val account = getBalanceUseCase()
        val amountFormatted = formatAmount(account.balance)
        val response = "In your ${account.bankName} savings account ending with ${account.accountLast4}, you have $amountFormatted rupees available."
        respond(response)
    }

    private fun handleTransfer(intent: VoiceIntent.TransferMoney) {
        val amount = intent.amount
        val recipient = intent.recipient

        if (amount == null || recipient == null) {
            respond("Please say the amount and recipient. For example, transfer 500 rupees to Rahul.")
            return
        }

        _uiState.update {
            it.copy(
                awaitingTransferConfirmation = true,
                pendingTransferAmount = amount,
                pendingTransferRecipient = recipient
            )
        }

        val amountFormatted = formatAmount(amount)
        respond("You are transferring $amountFormatted rupees to $recipient. Do you want to continue?")
    }

    private suspend fun handleConfirmYes() {
        val state = _uiState.value
        if (state.awaitingTransferConfirmation) {
            _uiState.update { it.copy(isProcessing = true) }
            val result = transferMoneyUseCase(state.pendingTransferAmount, state.pendingTransferRecipient)
            _uiState.update {
                it.copy(awaitingTransferConfirmation = false)
            }
            if (result.success) {
                respond("Transfer successful. Transaction reference number ${result.txnId}.")
            } else {
                respond("Transfer failed. Please try again.")
            }
        } else {
            respond("There is nothing to confirm right now.")
        }
    }

    private fun handleConfirmNo() {
        if (_uiState.value.awaitingTransferConfirmation) {
            _uiState.update { it.copy(awaitingTransferConfirmation = false) }
            respond("Transfer cancelled.")
        } else {
            respond("Okay. How can I help you?")
        }
    }

    private suspend fun handleChequeBook() {
        val result = requestChequeBookUseCase()
        respond("Your cheque book request has been registered successfully and will be delivered within ${result.estimatedDeliveryDays} working days.")
    }

    private suspend fun handleRecentTransactions() {
        val transactions = getRecentTransactionsUseCase()
        _uiState.update { it.copy(transactions = transactions, showTransactions = true) }

        if (transactions.isNotEmpty()) {
            val latest = transactions.first()
            val amountFormatted = formatAmount(latest.amount)
            respond("Your last transaction was $amountFormatted rupees spent at ${latest.description} ${latest.date}.")
        } else {
            respond("You have no recent transactions.")
        }
    }

    private fun handleHelp() {
        respond("Connecting you to customer support. You can say check balance, transfer money, request cheque book, or show recent transactions.")
    }

    private fun handleUnknown() {
        respond("I did not understand. You can say check balance, transfer money, request cheque book, recent transactions, or help.")
    }

    private fun respond(text: String) {
        _uiState.update { it.copy(responseText = text, isProcessing = false) }
        speakCallback?.invoke(text)
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun dismissTransactions() {
        _uiState.update { it.copy(showTransactions = false) }
    }

    private fun addRecentCommand(command: String) {
        _uiState.update { state ->
            val updated = listOf(command) + state.recentCommands.take(4)
            state.copy(recentCommands = updated)
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            String.format("%,d", amount.toLong())
        } else {
            String.format("%,.2f", amount)
        }
    }
}
