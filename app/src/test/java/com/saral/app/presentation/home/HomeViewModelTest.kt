package com.saral.app.presentation.home

import com.saral.app.data.mock.MockBankingRepository
import com.saral.app.domain.usecases.GetBalanceUseCase
import com.saral.app.domain.usecases.GetRecentTransactionsUseCase
import com.saral.app.domain.usecases.RequestChequeBookUseCase
import com.saral.app.domain.usecases.SearchTransactionsUseCase
import com.saral.app.domain.usecases.TransferMoneyUseCase
import com.saral.app.voice.VoiceIntentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ──────────────────────────────────────────
    // Balance
    // ──────────────────────────────────────────

    @Test
    fun checkBalance_speaksBalance() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("rupees") })
        assertFalse(vm.uiState.value.isProcessing)
    }

    @Test
    fun checkBalance_includesAccountLastFour() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("What is my account balance")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("7845") })
    }

    // ──────────────────────────────────────────
    // Transfer — emits navigation event
    // ──────────────────────────────────────────

    @Test
    fun transferIntent_emitsNavigateToTransfer() = runTest {
        val vm = createViewModel()
        val events = mutableListOf<HomeNavigationEvent>()

        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.onVoiceResult("Transfer money")
        advanceUntilIdle()

        assertTrue(events.any { it is HomeNavigationEvent.NavigateToTransfer })
        job.cancel()
    }

    @Test
    fun transferIntent_withAmountAndRecipient_emitsNavigateToTransfer() = runTest {
        val vm = createViewModel()
        val events = mutableListOf<HomeNavigationEvent>()

        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.onVoiceResult("Transfer 500 rupees to Rahul")
        advanceUntilIdle()

        assertTrue(events.any { it is HomeNavigationEvent.NavigateToTransfer })
        job.cancel()
    }

    @Test
    fun transferIntent_sendMoney_emitsNavigateToTransfer() = runTest {
        val vm = createViewModel()
        val events = mutableListOf<HomeNavigationEvent>()

        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.onVoiceResult("Send money to Priya")
        advanceUntilIdle()

        assertTrue(events.any { it is HomeNavigationEvent.NavigateToTransfer })
        job.cancel()
    }

    @Test
    fun confirmYes_whenNothingPending_speaksInfo() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Yes")
        advanceUntilIdle()

        assertTrue(spoken.any {
            it.contains("nothing to confirm", ignoreCase = true) ||
            it.contains("how can I help", ignoreCase = true)
        })
    }

    @Test
    fun confirmNo_speaksHelp() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("No")
        advanceUntilIdle()

        assertTrue(spoken.any {
            it.contains("how can I help", ignoreCase = true) ||
            it.contains("okay", ignoreCase = true)
        })
    }

    // ──────────────────────────────────────────
    // Transaction — ask count (Step 1)
    // ──────────────────────────────────────────

    @Test
    fun transactions_genericTrigger_promptsForCount() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Show recent transactions")
        advanceUntilIdle()

        assertTrue(
            "Expected prompt asking how many transactions to hear",
            spoken.any {
                it.contains("last transaction") &&
                it.contains("last 5") &&
                it.contains("last 10")
            }
        )
    }

    @Test
    fun transactions_bareWord_promptsForCount() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("transaction")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("last 5") || it.contains("last 10") })
    }

    // ──────────────────────────────────────────
    // Transaction — count (Step 2)
    // ──────────────────────────────────────────

    @Test
    fun transactionCount_one_speaksLastTransaction() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("last 1 transaction")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("last transaction") || it.contains("Your last transaction") })
        assertTrue(vm.uiState.value.showTransactions)
        assertTrue(vm.uiState.value.transactions.isNotEmpty())
    }

    @Test
    fun transactionCount_five_speaksFiveTransactions() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("last 5 transactions")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("5") || it.contains("five") || it.contains("transactions") })
        assertTrue(vm.uiState.value.showTransactions)
        assertEquals(10, vm.uiState.value.transactions.size)
    }

    @Test
    fun transactionCount_ten_speaksTenTransactions() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("last 10 transactions")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("10") || it.contains("ten") || it.contains("transactions") })
        assertTrue(vm.uiState.value.showTransactions)
    }

    @Test
    fun transactionCount_five_wordForm_works() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("last five transactions")
        advanceUntilIdle()

        assertTrue(spoken.isNotEmpty())
        assertTrue(vm.uiState.value.showTransactions)
    }

    @Test
    fun transactionCount_ten_wordForm_works() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("last ten transactions")
        advanceUntilIdle()

        assertTrue(spoken.isNotEmpty())
        assertTrue(vm.uiState.value.showTransactions)
    }

    @Test
    fun transactionCount_showsTransactionListInState() = runTest {
        val vm = createViewModel()

        vm.onVoiceResult("last 5 transactions")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.showTransactions)
        assertTrue(vm.uiState.value.transactions.isNotEmpty())
    }

    // ──────────────────────────────────────────
    // Transaction — query (keyword search)
    // ──────────────────────────────────────────

    @Test
    fun queryTransaction_salaryFound_speaksCredit() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Please check if the salary is credited this month")
        advanceUntilIdle()

        assertTrue(
            "Expected salary credited response",
            spoken.any { it.lowercase().contains("salary") && it.lowercase().contains("credited") }
        )
    }

    @Test
    fun queryTransaction_electricityFound_speaksDebit() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Is there any transaction for the electricity bill this month")
        advanceUntilIdle()

        assertTrue(
            "Expected electricity bill response",
            spoken.any { it.lowercase().contains("electricity") || it.lowercase().contains("payment") }
        )
    }

    @Test
    fun queryTransaction_notFound_speaksNoMatch() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Is there any transaction for rent this month")
        advanceUntilIdle()

        assertTrue(
            "Expected no-match response",
            spoken.any { it.lowercase().contains("no transaction") || it.contains("no transactions") }
        )
    }

    @Test
    fun queryTransaction_amazon_foundInMockData() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Any transaction for Amazon this month")
        advanceUntilIdle()

        assertTrue(spoken.any { it.lowercase().contains("amazon") || it.lowercase().contains("payment") })
    }

    @Test
    fun queryTransaction_salaryCredit_speaksAmount() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check salary credit")
        advanceUntilIdle()

        // Mock data has Salary Credit = ₹45,000
        assertTrue(spoken.any { it.contains("45") || it.contains("45,000") || it.contains("45000") })
    }

    // ──────────────────────────────────────────
    // Full two-step transaction flow
    // ──────────────────────────────────────────

    @Test
    fun fullTransactionFlow_askThenCount() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Show recent transactions")
        advanceUntilIdle()
        assertTrue(spoken.any { it.contains("last 5") || it.contains("last 10") })

        spoken.clear()
        vm.onVoiceResult("last 5 transactions")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showTransactions)
        assertTrue(spoken.isNotEmpty())
    }

    // ──────────────────────────────────────────
    // Balance follow-up (Feature v7.0)
    // ──────────────────────────────────────────

    @Test
    fun checkBalance_appendsFollowUpPrompt() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        assertTrue(
            "Expected follow-up prompt after balance",
            spoken.any { it.contains("main menu", ignoreCase = true) || it.contains("what you'd like", ignoreCase = true) }
        )
    }

    @Test
    fun checkBalance_followUp_mainMenu_dismisses() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("main menu")
        advanceUntilIdle()

        assertTrue(
            "Expected dismissal response after main menu",
            spoken.any { it.contains("How else can I help", ignoreCase = true) }
        )
    }

    @Test
    fun checkBalance_followUp_done_dismisses() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("done")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("How else can I help", ignoreCase = true) })
    }

    @Test
    fun checkBalance_followUp_back_dismisses() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("go back")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("How else can I help", ignoreCase = true) })
    }

    @Test
    fun checkBalance_followUp_newCommand_parsedNormally() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()
        spoken.clear()

        // Say another valid command — cheque book should work fine
        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()

        assertTrue(
            "Expected cheque book response after balance follow-up",
            spoken.any { it.contains("cheque book", ignoreCase = true) || it.contains("working days", ignoreCase = true) }
        )
    }

    @Test
    fun checkBalance_followUp_onlyOneInterceptedInput() = runTest {
        // After follow-up is consumed, subsequent inputs go through normal intent parsing
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        vm.onVoiceResult("main menu")   // consumes the follow-up flag
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("Check my balance")  // should parse normally again
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("rupees", ignoreCase = true) })
    }

    @Test
    fun checkBalance_followUp_transferCommand_navigates() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()
        val events = mutableListOf<HomeNavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        vm.onVoiceResult("Transfer money")
        advanceUntilIdle()

        assertTrue(events.any { it is HomeNavigationEvent.NavigateToTransfer })
        job.cancel()
    }

    // ──────────────────────────────────────────
    // Cheque book
    // ──────────────────────────────────────────

    @Test
    fun chequeBook_speaksConfirmation() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("cheque book") || it.contains("working days") })
    }

    // ──────────────────────────────────────────
    // Cheque book follow-up (Feature v7.0)
    // ──────────────────────────────────────────

    @Test
    fun chequeBook_appendsFollowUpPrompt() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()

        assertTrue(
            "Expected follow-up prompt after cheque book request",
            spoken.any { it.contains("main menu", ignoreCase = true) || it.contains("what you'd like", ignoreCase = true) }
        )
    }

    @Test
    fun chequeBook_followUp_mainMenu_dismisses() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("main menu")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("How else can I help", ignoreCase = true) })
    }

    @Test
    fun chequeBook_followUp_home_dismisses() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("take me home")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("How else can I help", ignoreCase = true) })
    }

    @Test
    fun chequeBook_followUp_newCommand_parsedNormally() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        assertTrue(
            "Expected balance response after cheque book follow-up",
            spoken.any { it.contains("rupees", ignoreCase = true) }
        )
    }

    @Test
    fun chequeBook_followUp_onlyOneInterceptedInput() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()

        vm.onVoiceResult("done")  // consumes follow-up
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("Request cheque book")  // should parse normally
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("working days", ignoreCase = true) })
    }

    @Test
    fun chequeBook_followUp_transferCommand_navigates() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()
        val events = mutableListOf<HomeNavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.onVoiceResult("Request cheque book")
        advanceUntilIdle()

        vm.onVoiceResult("Transfer money")
        advanceUntilIdle()

        assertTrue(events.any { it is HomeNavigationEvent.NavigateToTransfer })
        job.cancel()
    }

    // ──────────────────────────────────────────
    // Flag isolation (v7.0)
    // ──────────────────────────────────────────

    @Test
    fun simpleFollowUp_doesNotInterfereWithTransactionFollowUp() = runTest {
        // Check balance (sets awaitingSimpleFollowUp), then immediately ask
        // for transactions — the transaction count handler resets simpleFollowUp
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()
        spoken.clear()

        // Asking for transactions clears simpleFollowUp and sets transactionFlow
        vm.onVoiceResult("last 5 transactions")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.showTransactions)
        assertTrue(spoken.any { it.contains("transactions", ignoreCase = true) })
    }

    @Test
    fun transactionFollowUp_doesNotInterfereWithSimpleFollowUp() = runTest {
        // After transaction follow-up is consumed, balance follow-up should work
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("last 5 transactions")
        advanceUntilIdle()

        vm.onVoiceResult("main menu")  // clears transactionFollowUp
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceResult("Check my balance")  // should trigger simpleFollowUp
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("main menu", ignoreCase = true) || it.contains("what you'd like", ignoreCase = true) })
    }

    // ──────────────────────────────────────────
    // Help & Unknown
    // ──────────────────────────────────────────

    @Test
    fun unknownIntent_speaksDidNotUnderstand() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("asldkfjasldkfj")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("did not understand") || it.contains("You can say") })
    }

    @Test
    fun helpIntent_speaksOptions() = runTest {
        val vm = createViewModel()
        val spoken = vm.captureSpoken()

        vm.onVoiceResult("Help")
        advanceUntilIdle()

        assertTrue(spoken.isNotEmpty())
    }

    @Test
    fun blankInput_doesNotCrash() = runTest {
        val vm = createViewModel()
        vm.onVoiceResult("   ")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isProcessing)
    }

    // ──────────────────────────────────────────
    // Recent commands tracking
    // ──────────────────────────────────────────

    @Test
    fun recentCommands_trackedAfterInput() = runTest {
        val vm = createViewModel()

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.recentCommands.contains("Check my balance"))
    }

    // ──────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────

    private fun createViewModel(repo: MockBankingRepository = MockBankingRepository()): HomeViewModel {
        return HomeViewModel(
            getBalanceUseCase = GetBalanceUseCase(repo),
            transferMoneyUseCase = TransferMoneyUseCase(repo),
            requestChequeBookUseCase = RequestChequeBookUseCase(repo),
            getRecentTransactionsUseCase = GetRecentTransactionsUseCase(repo),
            searchTransactionsUseCase = SearchTransactionsUseCase(repo),
            intentParser = VoiceIntentParser()
        )
    }

    private fun HomeViewModel.captureSpoken(): MutableList<String> {
        val spoken = mutableListOf<String>()
        setSpeakCallback { spoken.add(it) }
        return spoken
    }
}
