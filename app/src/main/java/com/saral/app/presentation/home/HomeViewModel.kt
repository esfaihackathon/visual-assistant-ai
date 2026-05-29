package com.saral.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saral.app.domain.models.Transaction
import com.saral.app.domain.models.TransactionType
import com.saral.app.domain.models.VoiceIntent
import com.saral.app.domain.usecases.GetBalanceUseCase
import com.saral.app.domain.usecases.GetRecentTransactionsUseCase
import com.saral.app.domain.usecases.RequestChequeBookUseCase
import com.saral.app.domain.usecases.SearchTransactionsUseCase
import com.saral.app.domain.usecases.TransferMoneyUseCase
import com.saral.app.voice.VoiceIntentParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeNavigationEvent {
    object NavigateToTransfer : HomeNavigationEvent()
}

data class HomeUiState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val responseText: String = "",
    val isProcessing: Boolean = false,
    val recentCommands: List<String> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val showTransactions: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val transferMoneyUseCase: TransferMoneyUseCase,
    private val requestChequeBookUseCase: RequestChequeBookUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val intentParser: VoiceIntentParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<HomeNavigationEvent>()
    val navigationEvent: SharedFlow<HomeNavigationEvent> = _navigationEvent.asSharedFlow()

    private var speakCallback: ((String) -> Unit)? = null

    // True immediately after reading transactions — next voice input is a
    // follow-up choice ("more" or "main menu") rather than a new intent.
    private var awaitingPostTransactionChoice = false

    // True after balance check or cheque book — next input is either "main menu"
    // (dismiss) or a new command (re-parsed normally).
    private var awaitingSimpleFollowUp = false

    fun setSpeakCallback(callback: (String) -> Unit) {
        speakCallback = callback
    }

    fun onVoiceResult(text: String) {
        if (text.isBlank()) return

        _uiState.update { it.copy(recognizedText = text, isProcessing = true) }
        addRecentCommand(text)

        // Post-transaction follow-up takes priority over normal intent parsing
        if (awaitingPostTransactionChoice) {
            viewModelScope.launch { handlePostTransactionChoice(text) }
            return
        }

        // Post-balance / post-cheque-book simple follow-up
        if (awaitingSimpleFollowUp) {
            viewModelScope.launch { handleSimpleFollowUp(text) }
            return
        }

        val intent = intentParser.parse(text)
        handleIntent(intent)
    }

    private fun handleIntent(intent: VoiceIntent) {
        viewModelScope.launch {
            when (intent) {
                is VoiceIntent.CheckBalance -> handleCheckBalance()
                is VoiceIntent.TransferMoney -> _navigationEvent.emit(HomeNavigationEvent.NavigateToTransfer)
                is VoiceIntent.RequestChequeBook -> handleChequeBook()
                is VoiceIntent.RecentTransactions -> handleAskTransactionCount()
                is VoiceIntent.AskTransactionCount -> handleAskTransactionCount()
                is VoiceIntent.TransactionCount -> handleTransactionCount(intent.count)
                is VoiceIntent.QueryTransaction -> handleQueryTransaction(intent.keyword)
                is VoiceIntent.Help -> handleHelp()
                is VoiceIntent.ConfirmYes -> respond("There is nothing to confirm right now. How can I help you?")
                is VoiceIntent.ConfirmNo -> respond("Okay. How can I help you?")
                is VoiceIntent.Unknown -> handleUnknown()
            }
        }
    }

    private suspend fun handleCheckBalance() {
        val account = getBalanceUseCase()
        val amountFormatted = formatAmount(account.balance)
        val response = "In your ${account.bankName} savings account ending with ${account.accountLast4}, you have $amountFormatted rupees available."
        awaitingSimpleFollowUp = true
        respond("$response Say main menu to go home, or tell me what you'd like to do next.")
    }

    private suspend fun handleChequeBook() {
        val result = requestChequeBookUseCase()
        val response = "Your cheque book request has been registered successfully and will be delivered within ${result.estimatedDeliveryDays} working days."
        awaitingSimpleFollowUp = true
        respond("$response Say main menu to go home, or tell me what you'd like to do next.")
    }

    // Handles the single follow-up after balance or cheque book:
    //   "main menu / done / home / back" → dismiss with help menu prompt
    //   Anything else → clear flag and re-parse as a new intent
    private suspend fun handleSimpleFollowUp(text: String) {
        awaitingSimpleFollowUp = false
        val lower = text.lowercase().trim()
        val wantsHome = Regex("main menu|home|done|back|exit|okay|no|nothing|enough|stop").containsMatchIn(lower)
        if (wantsHome) {
            respond("Okay! How else can I help you? You can check balance, transfer money, request a cheque book, or ask for help.")
        } else {
            val intent = intentParser.parse(text)
            handleIntent(intent)
        }
    }

    private fun handleAskTransactionCount() {
        awaitingPostTransactionChoice = false
        awaitingSimpleFollowUp = false
        respond("Do you want to hear the last transaction, last 5 transactions, or last 10 transactions?")
    }

    private suspend fun handleTransactionCount(count: Int) {
        val transactions = getRecentTransactionsUseCase()
        val subset = transactions.take(count)
        _uiState.update { it.copy(transactions = transactions, showTransactions = true) }

        if (subset.isEmpty()) {
            respond("You have no recent transactions.")
            return
        }

        val details = if (count == 1) {
            val t = subset.first()
            val action = if (t.type == TransactionType.CREDIT) "credited" else "debited"
            "Your last transaction: ${formatAmount(t.amount)} rupees $action for ${t.description} on ${t.date}."
        } else {
            val sb = StringBuilder("Here are your last ${subset.size} transactions. ")
            subset.forEachIndexed { index, t ->
                val action = if (t.type == TransactionType.CREDIT) "credited" else "debited"
                sb.append("${index + 1}. ${t.description}, ${formatAmount(t.amount)} rupees $action on ${t.date}. ")
            }
            sb.toString().trim()
        }

        // After reading transactions, ask what the user wants next
        awaitingPostTransactionChoice = true
        respond("$details Would you like to hear more transactions, or say main menu to go back home?")
    }

    private suspend fun handleQueryTransaction(keyword: String) {
        val matches = searchTransactionsUseCase(keyword)

        if (matches.isEmpty()) {
            respond("There are no transactions matching your request for this month.")
            return
        }

        val t = matches.first()
        val amountFormatted = formatAmount(t.amount)
        val details = if (t.type == TransactionType.CREDIT) {
            "Yes, ${t.description} of $amountFormatted rupees was credited on ${t.date}."
        } else {
            "Yes, a payment of $amountFormatted rupees was made on ${t.date} for ${t.description}."
        }

        awaitingPostTransactionChoice = true
        respond("$details Would you like to check another transaction, or say main menu to go back?")
    }

    // Handles the follow-up after reading transactions:
    //   "more / another / yes" → ask how many again
    //   "home / main menu / no / done" → return to normal assistant mode
    private suspend fun handlePostTransactionChoice(text: String) {
        val lower = text.lowercase().trim()
        val wantsMore = Regex("more|another|yes|again|transaction|last|hear|show").containsMatchIn(lower)
        val wantsHome = Regex("main menu|home|done|no|back|enough|stop|okay|that.s all|exit").containsMatchIn(lower)

        when {
            wantsMore -> {
                awaitingPostTransactionChoice = false
                handleAskTransactionCount()
            }
            wantsHome -> {
                awaitingPostTransactionChoice = false
                respond("Okay! How else can I help you? You can check balance, transfer money, request a cheque book, or ask for help.")
            }
            else -> {
                // Re-prompt without resetting the flag
                respond("Say 'more transactions' to hear more, or 'main menu' to go back home.")
            }
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
