package com.saral.app.presentation.transfer

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.BankAccount
import com.saral.app.domain.models.Beneficiary
import com.saral.app.domain.models.ChequeBookRequest
import com.saral.app.domain.models.Transaction
import com.saral.app.domain.models.TransferResult
import com.saral.app.domain.usecases.GetBalanceUseCase
import com.saral.app.domain.usecases.GetBeneficiariesUseCase
import com.saral.app.domain.usecases.TransferMoneyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

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
    // Helpers
    // ──────────────────────────────────────────

    private class FakeBankingRepository(var balance: Double = 25000.0) : BankingRepository {
        override suspend fun getBalance() = BankAccount("SBI", "7845", "Savings", balance)
        override suspend fun transferMoney(amount: Double, recipientName: String): TransferResult {
            balance -= amount
            return TransferResult(success = true, txnId = "TXN999", amount = amount, recipientName = recipientName)
        }
        override suspend fun requestChequeBook() = ChequeBookRequest("REQUESTED", 5)
        override suspend fun getRecentTransactions(): List<Transaction> = emptyList()
        override suspend fun searchTransactions(keyword: String): List<Transaction> = emptyList()
        override suspend fun getBeneficiaries() = listOf(
            Beneficiary("BEN001", "Rahul Sharma", "1234", "SBI", "SBIN0001234"),
            Beneficiary("BEN002", "Priya Gupta", "5678", "HDFC Bank", "HDFC0005678"),
            Beneficiary("BEN003", "Amit Kumar", "9012", "ICICI Bank", "ICIC0009012")
        )
    }

    private class FailingTransferRepository : BankingRepository {
        override suspend fun getBalance() = BankAccount("SBI", "7845", "Savings", 1000.0)
        override suspend fun transferMoney(amount: Double, recipientName: String) =
            TransferResult(success = false, txnId = "", amount = amount, recipientName = recipientName)
        override suspend fun requestChequeBook() = ChequeBookRequest("REQUESTED", 5)
        override suspend fun getRecentTransactions(): List<Transaction> = emptyList()
        override suspend fun searchTransactions(keyword: String): List<Transaction> = emptyList()
        override suspend fun getBeneficiaries() = listOf(
            Beneficiary("BEN001", "Rahul Sharma", "1234", "SBI", "SBIN0001234")
        )
    }

    private fun createVm(
        repo: BankingRepository = FakeBankingRepository()
    ): Pair<TransferViewModel, MutableList<String>> {
        val spoken = mutableListOf<String>()
        val vm = TransferViewModel(
            getBeneficiariesUseCase = GetBeneficiariesUseCase(repo),
            transferMoneyUseCase = TransferMoneyUseCase(repo),
            getBalanceUseCase = GetBalanceUseCase(repo)
        )
        vm.setSpeakCallback { text -> spoken.add(text) }
        return vm to spoken
    }

    // ──────────────────────────────────────────
    // Initial state
    // ──────────────────────────────────────────

    @Test
    fun initialStep_isSelectingBeneficiary() = runTest {
        val (vm, _) = createVm()
        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
    }

    @Test
    fun initialAssistantMessage_isEmpty() = runTest {
        val (vm, _) = createVm()
        assertEquals("", vm.assistantMessage.value)
    }

    @Test
    fun initialRecognizedText_isEmpty() = runTest {
        val (vm, _) = createVm()
        assertEquals("", vm.recognizedText.value)
    }

    @Test
    fun beneficiaries_loadedAfterInit() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        assertEquals(3, vm.beneficiaries.value.size)
    }

    @Test
    fun beneficiaries_containsExpectedNames() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        val names = vm.beneficiaries.value.map { it.name }
        assertTrue(names.contains("Rahul Sharma"))
        assertTrue(names.contains("Priya Gupta"))
        assertTrue(names.contains("Amit Kumar"))
    }

    // ──────────────────────────────────────────
    // onScreenLoad
    // ──────────────────────────────────────────

    @Test
    fun onScreenLoad_speaksIntro() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onScreenLoad()

        assertTrue(spoken.isNotEmpty())
        val msg = spoken.first()
        assertTrue(msg.contains("beneficiar", ignoreCase = true))
    }

    @Test
    fun onScreenLoad_listsAllBeneficiaries() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onScreenLoad()

        val msg = spoken.first()
        assertTrue(msg.contains("Rahul Sharma"))
        assertTrue(msg.contains("Priya Gupta"))
        assertTrue(msg.contains("Amit Kumar"))
    }

    @Test
    fun onScreenLoad_calledTwice_speaksOnlyOnce() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onScreenLoad()
        vm.onScreenLoad()

        assertEquals(1, spoken.size)
    }

    @Test
    fun onScreenLoad_beforeBeneficiariesLoad_doesNotSpeak() = runTest {
        val (vm, spoken) = createVm()
        vm.onScreenLoad()
        assertTrue(spoken.isEmpty())
    }

    // ──────────────────────────────────────────
    // Beneficiary selection
    // ──────────────────────────────────────────

    @Test
    fun selectBeneficiary_byFullName_transitionsToEnterAmount() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Rahul Sharma")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.EnterAmount)
    }

    @Test
    fun selectBeneficiary_byFirstName_matchesCorrectly() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Priya")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.EnterAmount
        assertEquals("Priya Gupta", step.beneficiary.name)
    }

    @Test
    fun selectBeneficiary_byLastName_matchesCorrectly() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Kumar")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.EnterAmount
        assertEquals("Amit Kumar", step.beneficiary.name)
    }

    @Test
    fun selectBeneficiary_inPhrase_matches() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Transfer to Rahul")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.EnterAmount)
    }

    @Test
    fun selectBeneficiary_speaksSelectedDetails() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("Rahul Sharma") })
        assertTrue(spoken.any { it.contains("SBI") })
        assertTrue(spoken.any { it.contains("1234") })
    }

    @Test
    fun selectBeneficiary_notFound_remainsInSelectingState() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Zubair Khan Mirza")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
    }

    @Test
    fun selectBeneficiary_notFound_speaksError() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("Zubair Khan Mirza")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("could not find", ignoreCase = true) })
    }

    @Test
    fun selectBeneficiary_updatesRecognizedText() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        assertEquals("Rahul", vm.recognizedText.value)
    }

    // ──────────────────────────────────────────
    // Amount input
    // ──────────────────────────────────────────

    @Test
    fun enterAmount_integer_parsedCorrectly() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        vm.onVoiceInput("500 rupees")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.ConfirmTransfer
        assertEquals(500.0, step.amount, 0.001)
    }

    @Test
    fun enterAmount_withCommas_parsedCorrectly() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        vm.onVoiceInput("1,250 rupees")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.ConfirmTransfer
        assertEquals(1250.0, step.amount, 0.001)
    }

    @Test
    fun enterAmount_decimalAmount_parsedCorrectly() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        vm.onVoiceInput("250.50")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.ConfirmTransfer
        assertEquals(250.50, step.amount, 0.001)
    }

    @Test
    fun enterAmount_noNumber_remainsInEnterAmountState() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        vm.onVoiceInput("some rupees please")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.EnterAmount)
    }

    @Test
    fun enterAmount_noNumber_speaksError() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("rupees please")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("could not understand", ignoreCase = true) })
    }

    @Test
    fun enterAmount_speaksConfirmationWithDetails() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("300 rupees")
        advanceUntilIdle()

        val msg = spoken.joinToString(" ")
        assertTrue(msg.contains("300"))
        assertTrue(msg.contains("Rahul Sharma"))
        assertTrue(msg.contains("yes") || msg.contains("confirm"))
    }

    @Test
    fun enterAmount_preservesBeneficiary() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Priya")
        advanceUntilIdle()

        vm.onVoiceInput("750 rupees")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.ConfirmTransfer
        assertEquals("Priya Gupta", step.beneficiary.name)
    }

    // ──────────────────────────────────────────
    // Confirmation — yes → AwaitingBiometric
    // ──────────────────────────────────────────

    @Test
    fun confirmYes_movesToAwaitingBiometric() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("yes")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
    }

    @Test
    fun confirmYes_speaksAuthPrompt() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("yes")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("fingerprint", ignoreCase = true) || it.contains("authenticate", ignoreCase = true) })
    }

    @Test
    fun confirmYes_awaitingBiometric_preservesBeneficiaryAndAmount() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Priya")
        advanceUntilIdle()
        vm.onVoiceInput("2000")
        advanceUntilIdle()

        vm.onVoiceInput("yes")
        advanceUntilIdle()

        val step = vm.step.value as TransferStep.AwaitingBiometric
        assertEquals("Priya Gupta", step.beneficiary.name)
        assertEquals(2000.0, step.amount, 0.001)
    }

    @Test
    fun confirmYes_haan_movesToAwaitingBiometric() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("haan")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
    }

    @Test
    fun confirmOk_movesToAwaitingBiometric() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("ok")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
    }

    // ──────────────────────────────────────────
    // AwaitingBiometric — voice is ignored
    // ──────────────────────────────────────────

    @Test
    fun awaitingBiometric_voiceInputIsIgnored() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
        spoken.clear()

        vm.onVoiceInput("yes confirm go ahead")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
        assertTrue(spoken.isEmpty())
    }

    // ──────────────────────────────────────────
    // Biometric success → transfer executes
    // ──────────────────────────────────────────

    @Test
    fun onBiometricSuccess_completesTransfer() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
        spoken.clear()

        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Complete)
        assertTrue(spoken.any { it.contains("successful", ignoreCase = true) })
    }

    @Test
    fun onBiometricSuccess_speaksRemainingBalance() = runTest {
        val repo = FakeBankingRepository(balance = 25000.0)
        val (vm, spoken) = createVm(repo)
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500 rupees")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        spoken.clear()

        vm.onBiometricSuccess()
        advanceUntilIdle()

        val msg = spoken.joinToString(" ")
        assertTrue(msg.contains("24,500") || msg.contains("24500"))
    }

    @Test
    fun onBiometricSuccess_completeStateHasCorrectDetails() = runTest {
        val repo = FakeBankingRepository(balance = 10000.0)
        val (vm, _) = createVm(repo)
        advanceUntilIdle()
        vm.onVoiceInput("Priya")
        advanceUntilIdle()
        vm.onVoiceInput("2000")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()

        vm.onBiometricSuccess()
        advanceUntilIdle()

        val complete = vm.step.value as TransferStep.Complete
        assertEquals("Priya Gupta", complete.beneficiary.name)
        assertEquals(2000.0, complete.amount, 0.001)
        assertEquals(8000.0, complete.remainingBalance, 0.001)
        assertTrue(complete.txnId.isNotEmpty())
    }

    @Test
    fun onBiometricSuccess_whenNotAwaitingBiometric_doesNothing() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        spoken.clear()

        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
        assertTrue(spoken.isEmpty())
    }

    // ──────────────────────────────────────────
    // Biometric failed
    // ──────────────────────────────────────────

    @Test
    fun onBiometricFailed_remainsInAwaitingBiometric() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()

        vm.onBiometricFailed()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)
    }

    @Test
    fun onBiometricFailed_speaksError() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        spoken.clear()

        vm.onBiometricFailed()

        assertTrue(spoken.any { it.contains("failed", ignoreCase = true) || it.contains("try again", ignoreCase = true) })
    }

    @Test
    fun onBiometricFailed_whenNotAwaitingBiometric_doesNothing() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        spoken.clear()

        vm.onBiometricFailed()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
        assertTrue(spoken.isEmpty())
    }

    // ──────────────────────────────────────────
    // Confirmation — no
    // ──────────────────────────────────────────

    @Test
    fun confirmNo_returnsToSelectingBeneficiary() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("no")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
    }

    @Test
    fun confirmNo_nahi_alsoWorks() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("nahi")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
    }

    @Test
    fun confirmNo_cancel_alsoWorks() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("cancel")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
    }

    @Test
    fun confirmNo_speaksCancelledMessage() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("no")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("cancel", ignoreCase = true) })
    }

    // ──────────────────────────────────────────
    // Confirmation — ambiguous
    // ──────────────────────────────────────────

    @Test
    fun confirmAmbiguous_remainsInConfirmState() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()

        vm.onVoiceInput("maybe later I think")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.ConfirmTransfer)
    }

    @Test
    fun confirmAmbiguous_speaksRetryPrompt() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("blah blah")
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("yes") || it.contains("no") })
    }

    // ──────────────────────────────────────────
    // Failed transfer (via biometric success)
    // ──────────────────────────────────────────

    @Test
    fun transferFailed_stateIsSetToFailed() = runTest {
        val (vm, _) = createVm(FailingTransferRepository())
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()

        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Failed)
    }

    @Test
    fun transferFailed_speaksFailureMessage() = runTest {
        val (vm, spoken) = createVm(FailingTransferRepository())
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        spoken.clear()

        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(spoken.any { it.contains("failed", ignoreCase = true) })
    }

    // ──────────────────────────────────────────
    // Terminal states ignore voice
    // ──────────────────────────────────────────

    @Test
    fun completeState_voiceInputIsIgnored() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Complete)
        spoken.clear()

        vm.onVoiceInput("check balance")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Complete)
        assertTrue(spoken.isEmpty())
    }

    @Test
    fun failedState_voiceInputIsIgnored() = runTest {
        val (vm, spoken) = createVm(FailingTransferRepository())
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Failed)
        spoken.clear()

        vm.onVoiceInput("retry")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Failed)
        assertTrue(spoken.isEmpty())
    }

    // ──────────────────────────────────────────
    // reset()
    // ──────────────────────────────────────────

    @Test
    fun reset_returnsToSelectingBeneficiary() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        vm.reset()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
    }

    @Test
    fun reset_clearsRecognizedText() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onVoiceInput("Rahul")
        advanceUntilIdle()

        vm.reset()

        assertEquals("", vm.recognizedText.value)
    }

    @Test
    fun reset_clearsAssistantMessage() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()
        vm.onScreenLoad()
        assertTrue(vm.assistantMessage.value.isNotEmpty())

        vm.reset()

        assertEquals("", vm.assistantMessage.value)
    }

    @Test
    fun reset_allowsOnScreenLoadAgain() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        vm.onScreenLoad()
        assertEquals(1, spoken.size)

        vm.reset()

        vm.onScreenLoad()
        assertEquals(2, spoken.size)
    }

    // ──────────────────────────────────────────
    // Blank input
    // ──────────────────────────────────────────

    @Test
    fun blankInput_isIgnored() = runTest {
        val (vm, spoken) = createVm()
        advanceUntilIdle()
        spoken.clear()

        vm.onVoiceInput("   ")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)
        assertTrue(spoken.isEmpty())
    }

    // ──────────────────────────────────────────
    // Full end-to-end flow
    // ──────────────────────────────────────────

    @Test
    fun fullFlow_selectAmountConfirmBiometricComplete() = runTest {
        val repo = FakeBankingRepository(balance = 20000.0)
        val (vm, spoken) = createVm(repo)
        advanceUntilIdle()

        // Intro
        vm.onScreenLoad()
        assertTrue(spoken.any { it.contains("Priya Gupta") })

        // Step 1: select beneficiary
        vm.onVoiceInput("Priya")
        advanceUntilIdle()
        assertTrue(vm.step.value is TransferStep.EnterAmount)

        // Step 2: enter amount
        vm.onVoiceInput("750 rupees")
        advanceUntilIdle()
        assertTrue(vm.step.value is TransferStep.ConfirmTransfer)

        // Step 3: confirm → awaiting biometric
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)

        // Step 4: biometric success → complete
        vm.onBiometricSuccess()
        advanceUntilIdle()
        assertTrue(vm.step.value is TransferStep.Complete)

        val complete = vm.step.value as TransferStep.Complete
        assertEquals("Priya Gupta", complete.beneficiary.name)
        assertEquals(750.0, complete.amount, 0.001)
        assertEquals(19250.0, complete.remainingBalance, 0.001)
        assertTrue(spoken.any { it.contains("successful", ignoreCase = true) })
        assertTrue(spoken.any { it.contains("19,250") || it.contains("19250") })
    }

    @Test
    fun fullFlow_cancelMidway_restartSucceeds() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("no")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.SelectingBeneficiary)

        vm.onVoiceInput("Amit")
        advanceUntilIdle()
        vm.onVoiceInput("1000")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()
        vm.onBiometricSuccess()
        advanceUntilIdle()

        val complete = vm.step.value as TransferStep.Complete
        assertEquals("Amit Kumar", complete.beneficiary.name)
        assertEquals(1000.0, complete.amount, 0.001)
    }

    @Test
    fun fullFlow_hindi_confirmationWorks() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("haan")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)

        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Complete)
    }

    @Test
    fun fullFlow_biometricFailed_thenSucceeds() = runTest {
        val (vm, _) = createVm()
        advanceUntilIdle()

        vm.onVoiceInput("Rahul")
        advanceUntilIdle()
        vm.onVoiceInput("500")
        advanceUntilIdle()
        vm.onVoiceInput("yes")
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)

        // First attempt fails
        vm.onBiometricFailed()
        assertTrue(vm.step.value is TransferStep.AwaitingBiometric)

        // Second attempt succeeds
        vm.onBiometricSuccess()
        advanceUntilIdle()

        assertTrue(vm.step.value is TransferStep.Complete)
    }
}
