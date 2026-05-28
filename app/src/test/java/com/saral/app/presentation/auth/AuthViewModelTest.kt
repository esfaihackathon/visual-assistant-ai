package com.saral.app.presentation.auth

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for AuthViewModel state machine and spoken output.
 *
 * Two harness modes:
 *  - createVm()         — TTS callbacks fire immediately (simulates instant speech completion).
 *                         Most tests use this; final state is deterministic after each call.
 *  - createVmDeferred() — callbacks are queued but NOT fired automatically.
 *                         Used when a test needs to inspect an intermediate state (e.g. OtpCalling).
 */
class AuthViewModelTest {

    // ──────────────────────────────────────────
    // Harness
    // ──────────────────────────────────────────

    /** Auto-fires every TTS completion callback immediately after it is registered. */
    private fun createVm(): VmHarness {
        val vm = AuthViewModel()
        val spoken = mutableListOf<String>()
        vm.setSpeakCallback { text, onComplete ->
            spoken.add(text)
            onComplete?.invoke()
        }
        return VmHarness(vm, spoken)
    }

    /** Queues TTS completion callbacks without firing them. */
    private fun createVmDeferred(): DeferredVmHarness {
        val vm = AuthViewModel()
        val spoken = mutableListOf<String>()
        val pending = mutableListOf<() -> Unit>()
        vm.setSpeakCallback { text, onComplete ->
            spoken.add(text)
            if (onComplete != null) pending.add(onComplete)
        }
        return DeferredVmHarness(vm, spoken, pending)
    }

    data class VmHarness(val vm: AuthViewModel, val spoken: MutableList<String>)
    data class DeferredVmHarness(
        val vm: AuthViewModel,
        val spoken: MutableList<String>,
        val pending: MutableList<() -> Unit>
    ) {
        fun fireNext() { pending.removeFirstOrNull()?.invoke() }
        fun fireAll()  { pending.toList().also { pending.clear() }.forEach { it() } }
    }

    /**
     * The call TTS message is: "...one-time password is X, X, X, X. Please..."
     * Extract the 4-digit OTP so tests can submit the correct value.
     */
    private fun extractOtp(spoken: List<String>): String {
        for (text in spoken) {
            val match = Regex("is (\\d(?:, \\d){3})\\.").find(text)
            if (match != null) return match.groupValues[1].replace(", ", "")
        }
        error("No OTP found in spoken texts: $spoken")
    }

    /** Advance a fresh VM through biometric + OTP call; returns it in OtpInput(3) state. */
    private fun vmAtOtpInput(): VmHarness {
        val h = createVm()
        h.vm.onBiometricSuccess()
        assertEquals(AuthStep.OtpInput(3), h.vm.authStep.value)
        return h
    }

    /** Exhaust all 3 attempts with wrong OTPs. Returns VM in OtpExhausted state. */
    private fun vmExhausted(): VmHarness {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        val wrong = if (otp == "1234") "5678" else "1234"
        repeat(OtpManager.MAX_ATTEMPTS) { h.vm.onVoiceInput(wrong) }
        assertEquals(AuthStep.OtpExhausted, h.vm.authStep.value)
        return h
    }

    // ──────────────────────────────────────────
    // Initial state
    // ──────────────────────────────────────────

    @Test
    fun initialState_isBiometricPending() {
        val vm = AuthViewModel()
        assertTrue(vm.authStep.value is AuthStep.BiometricPending)
    }

    @Test
    fun noSpeakCallback_doesNotCrash_onBiometricSuccess() {
        val vm = AuthViewModel()
        vm.onBiometricSuccess() // no callback set — should not throw
        assertTrue(vm.authStep.value is AuthStep.OtpCalling)
    }

    // ──────────────────────────────────────────
    // Biometric → OTP calling
    // ──────────────────────────────────────────

    @Test
    fun onBiometricSuccess_immediatelySetsOtpCalling_beforeTtsDone() {
        val h = createVmDeferred()
        h.vm.onBiometricSuccess()
        // TTS hasn't finished yet — state must be OtpCalling at this point
        assertTrue(h.vm.authStep.value is AuthStep.OtpCalling)
    }

    @Test
    fun onBiometricSuccess_speaksAutomatedCallMessage() {
        val h = createVmDeferred()
        h.vm.onBiometricSuccess()
        assertTrue(
            "Expected automated-call message",
            h.spoken.any { it.contains("one-time password", ignoreCase = true) }
        )
    }

    @Test
    fun onBiometricSuccess_spokenMessageContainsFourDigitOtp() {
        val h = createVmDeferred()
        h.vm.onBiometricSuccess()
        val otp = extractOtp(h.spoken) // throws if not found
        assertEquals(4, otp.length)
        assertTrue(otp.all { it.isDigit() })
    }

    // ──────────────────────────────────────────
    // OTP calling → OTP input
    // ──────────────────────────────────────────

    @Test
    fun afterCallTtsDone_setsOtpInput() {
        val h = createVmDeferred()
        h.vm.onBiometricSuccess()
        h.fireNext() // simulate TTS of the call message finishing
        assertTrue(h.vm.authStep.value is AuthStep.OtpInput)
    }

    @Test
    fun afterCallTtsDone_attemptsLeftIs3() {
        val h = createVmDeferred()
        h.vm.onBiometricSuccess()
        h.fireNext()
        assertEquals(3, (h.vm.authStep.value as AuthStep.OtpInput).attemptsLeft)
    }

    @Test
    fun afterCallTtsDone_speaksOtpPrompt() {
        val h = createVmDeferred()
        h.vm.onBiometricSuccess()
        h.fireNext()
        assertTrue(h.spoken.any { it.contains("OTP", ignoreCase = true) })
    }

    // ──────────────────────────────────────────
    // Correct OTP
    // ──────────────────────────────────────────

    @Test
    fun correctOtp_setsSuccess() {
        val h = vmAtOtpInput()
        h.vm.onVoiceInput(extractOtp(h.spoken))
        assertEquals(AuthStep.Success, h.vm.authStep.value)
    }

    @Test
    fun correctOtp_speaksWelcomeUser() {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        h.spoken.clear()
        h.vm.onVoiceInput(otp)
        assertTrue(h.spoken.any { it.contains("Welcome user", ignoreCase = true) })
    }

    @Test
    fun correctOtp_speaksAllOptions_includingTransactions() {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        h.spoken.clear()
        h.vm.onVoiceInput(otp)
        val allSpoken = h.spoken.joinToString(" ").lowercase()
        assertTrue("Expected 'transactions' in options", allSpoken.contains("transaction"))
        assertTrue("Expected 'balance' in options",      allSpoken.contains("balance"))
        assertTrue("Expected 'transfer' in options",     allSpoken.contains("transfer"))
        assertTrue("Expected 'cheque' in options",       allSpoken.contains("cheque"))
        assertTrue("Expected 'help' in options",         allSpoken.contains("help"))
    }

    @Test
    fun correctOtp_withWordForm_setsSuccess() {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        val wordForm = otp.map { c ->
            mapOf('0' to "zero", '1' to "one", '2' to "two", '3' to "three", '4' to "four",
                  '5' to "five", '6' to "six", '7' to "seven", '8' to "eight", '9' to "nine"
            ).getValue(c)
        }.joinToString(" ")
        h.vm.onVoiceInput(wordForm)
        assertEquals(AuthStep.Success, h.vm.authStep.value)
    }

    @Test
    fun correctOtp_withSpacedDigits_setsSuccess() {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        h.vm.onVoiceInput(otp.toList().joinToString(" "))
        assertEquals(AuthStep.Success, h.vm.authStep.value)
    }

    // ──────────────────────────────────────────
    // Incorrect OTP — attempt tracking
    // ──────────────────────────────────────────

    @Test
    fun firstWrongOtp_setsOtpInput2() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        h.vm.onVoiceInput(wrong)
        assertEquals(AuthStep.OtpInput(2), h.vm.authStep.value)
    }

    @Test
    fun firstWrongOtp_speaks2AttemptsLeft() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        h.spoken.clear()
        h.vm.onVoiceInput(wrong)
        assertTrue(h.spoken.any { it.contains("2") && it.contains("attempt") })
    }

    @Test
    fun secondWrongOtp_setsOtpInput1() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        h.vm.onVoiceInput(wrong)
        h.vm.onVoiceInput(wrong)
        assertEquals(AuthStep.OtpInput(1), h.vm.authStep.value)
    }

    @Test
    fun secondWrongOtp_speaks1AttemptLeft() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        h.vm.onVoiceInput(wrong)
        h.spoken.clear()
        h.vm.onVoiceInput(wrong)
        assertTrue(h.spoken.any { it.contains("1 attempt") })
    }

    @Test
    fun threeWrongOtps_setsOtpExhausted() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        repeat(3) { h.vm.onVoiceInput(wrong) }
        assertEquals(AuthStep.OtpExhausted, h.vm.authStep.value)
    }

    @Test
    fun threeWrongOtps_speaksExhaustionMessage() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        repeat(3) { h.vm.onVoiceInput(wrong) }
        assertTrue(h.spoken.any { it.contains("3 times", ignoreCase = true) })
    }

    @Test
    fun threeWrongOtps_exhaustionMessageMentionsRegenerate() {
        val h = vmAtOtpInput()
        val wrong = wrongOtp(extractOtp(h.spoken))
        repeat(3) { h.vm.onVoiceInput(wrong) }
        val lastSpoken = h.spoken.last().lowercase()
        assertTrue(lastSpoken.contains("regenerate") || lastSpoken.contains("yes"))
    }

    // ──────────────────────────────────────────
    // OTP exhausted — regenerate / cancel
    // ──────────────────────────────────────────

    @Test
    fun regenerate_yes_restartsOtpFlow_intoOtpInput() {
        val h = vmExhausted()
        h.vm.onVoiceInput("yes")
        assertEquals(AuthStep.OtpInput(3), h.vm.authStep.value)
    }

    @Test
    fun regenerate_haan_restartsOtpFlow() {
        val h = vmExhausted()
        h.vm.onVoiceInput("Haan")
        assertEquals(AuthStep.OtpInput(3), h.vm.authStep.value)
    }

    @Test
    fun regenerate_ok_restartsOtpFlow() {
        val h = vmExhausted()
        h.vm.onVoiceInput("ok proceed")
        assertEquals(AuthStep.OtpInput(3), h.vm.authStep.value)
    }

    @Test
    fun regenerate_yes_speaksNewCallMessage() {
        val h = vmExhausted()
        h.spoken.clear()
        h.vm.onVoiceInput("yes")
        assertTrue(h.spoken.any { it.contains("one-time password", ignoreCase = true) })
    }

    @Test
    fun regenerate_yes_newOtpIsAlsoFourDigits() {
        val h = vmExhausted()
        h.spoken.clear()
        h.vm.onVoiceInput("yes")
        val newOtp = extractOtp(h.spoken)
        assertEquals(4, newOtp.length)
        assertTrue(newOtp.all { it.isDigit() })
    }

    @Test
    fun regenerate_no_returnsToBiometricPending() {
        val h = vmExhausted()
        h.vm.onVoiceInput("no")
        assertEquals(AuthStep.BiometricPending, h.vm.authStep.value)
    }

    @Test
    fun regenerate_no_speaksCancelMessage() {
        val h = vmExhausted()
        h.spoken.clear()
        h.vm.onVoiceInput("no")
        assertTrue(h.spoken.any { it.contains("cancel", ignoreCase = true) })
    }

    @Test
    fun regenerate_unrecognisedInput_returnsToBiometricPending() {
        val h = vmExhausted()
        h.vm.onVoiceInput("asldkfjasldkfj")
        assertEquals(AuthStep.BiometricPending, h.vm.authStep.value)
    }

    @Test
    fun afterRegenerate_canSucceedWithNewOtp() {
        val h = vmExhausted()
        h.vm.onVoiceInput("yes")         // restart OTP flow
        val newOtp = extractOtp(h.spoken)
        h.vm.onVoiceInput(newOtp)
        assertEquals(AuthStep.Success, h.vm.authStep.value)
    }

    // ──────────────────────────────────────────
    // Voice input routing — guard clauses
    // ──────────────────────────────────────────

    @Test
    fun onVoiceInput_whenBiometricPending_isIgnored() {
        val h = createVm()
        // State is BiometricPending; voice input must not change it
        h.vm.onVoiceInput("1234")
        assertEquals(AuthStep.BiometricPending, h.vm.authStep.value)
    }

    @Test
    fun onVoiceInput_whenOtpCalling_isIgnored() {
        val h = createVmDeferred() // deferred so we stay in OtpCalling
        h.vm.onBiometricSuccess()
        assertTrue(h.vm.authStep.value is AuthStep.OtpCalling)

        h.vm.onVoiceInput("1234") // must be ignored
        assertTrue(h.vm.authStep.value is AuthStep.OtpCalling)
    }

    @Test
    fun onVoiceInput_whenSuccess_isIgnored() {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        h.vm.onVoiceInput(otp) // go to Success
        assertEquals(AuthStep.Success, h.vm.authStep.value)

        h.vm.onVoiceInput("anything")
        assertEquals(AuthStep.Success, h.vm.authStep.value)
    }

    // ──────────────────────────────────────────
    // reset()
    // ──────────────────────────────────────────

    @Test
    fun reset_fromOtpInput_returnsToBiometricPending() {
        val h = vmAtOtpInput()
        h.vm.reset()
        assertEquals(AuthStep.BiometricPending, h.vm.authStep.value)
    }

    @Test
    fun reset_fromOtpExhausted_returnsToBiometricPending() {
        val h = vmExhausted()
        h.vm.reset()
        assertEquals(AuthStep.BiometricPending, h.vm.authStep.value)
    }

    @Test
    fun reset_fromSuccess_returnsToBiometricPending() {
        val h = vmAtOtpInput()
        val otp = extractOtp(h.spoken)
        h.vm.onVoiceInput(otp)
        h.vm.reset()
        assertEquals(AuthStep.BiometricPending, h.vm.authStep.value)
    }

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    /** Returns a 4-digit string that is different from the given OTP. */
    private fun wrongOtp(otp: String) = if (otp == "1234") "5678" else "1234"
}
