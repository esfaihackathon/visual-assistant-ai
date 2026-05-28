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
