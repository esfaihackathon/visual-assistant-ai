package com.saral.app.presentation.home

import com.saral.app.data.mock.MockBankingRepository
import com.saral.app.domain.usecases.GetBalanceUseCase
import com.saral.app.domain.usecases.GetRecentTransactionsUseCase
import com.saral.app.domain.usecases.RequestChequeBookUseCase
import com.saral.app.domain.usecases.TransferMoneyUseCase
import com.saral.app.voice.VoiceIntentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test
    fun checkBalance_speaksBalance() = runTest {
        val repo = MockBankingRepository()
        val vm = createViewModel(repo)

        val spoken = mutableListOf<String>()
        vm.setSpeakCallback { spoken.add(it) }

        vm.onVoiceResult("Check my balance")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("you have") || it.contains("rupees") })
        assertFalse(vm.uiState.value.isProcessing)
    }

    @Test
    fun transferFlow_confirmationAndExecution() = runTest {
        val repo = MockBankingRepository()
        val vm = createViewModel(repo)

        val spoken = mutableListOf<String>()
        vm.setSpeakCallback { spoken.add(it) }

        vm.onVoiceResult("Transfer 500 rupees to Rahul")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.awaitingTransferConfirmation)
        assertTrue(spoken.any { it.contains("Are you transferring") || it.contains("Do you want to continue") || it.contains("transferring") })

        vm.onVoiceResult("Yes")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.awaitingTransferConfirmation)
        assertTrue(spoken.any { it.contains("Transfer successful") })
    }

    @Test
    fun transferFlow_cancellation() = runTest {
        val repo = MockBankingRepository()
        val vm = createViewModel(repo)

        val spoken = mutableListOf<String>()
        vm.setSpeakCallback { spoken.add(it) }

        vm.onVoiceResult("Transfer 200 rupees to Priya")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.awaitingTransferConfirmation)

        vm.onVoiceResult("No")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.awaitingTransferConfirmation)
        assertTrue(spoken.any { it.contains("Transfer cancelled") })
    }

    @Test
    fun unknownIntent_speaksHelp() = runTest {
        val repo = MockBankingRepository()
        val vm = createViewModel(repo)

        val spoken = mutableListOf<String>()
        vm.setSpeakCallback { spoken.add(it) }

        vm.onVoiceResult("asldkfjasldkfj")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("did not understand") || it.contains("You can say") })
    }

    private fun createViewModel(repo: MockBankingRepository): HomeViewModel {
        val getBalance = GetBalanceUseCase(repo)
        val transfer = TransferMoneyUseCase(repo)
        val cheque = RequestChequeBookUseCase(repo)
        val txns = GetRecentTransactionsUseCase(repo)
        val parser = VoiceIntentParser()

        return HomeViewModel(getBalance, transfer, cheque, txns, parser)
    }
}
