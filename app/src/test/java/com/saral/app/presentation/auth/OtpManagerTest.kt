package com.saral.app.presentation.auth

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OtpManagerTest {

    private lateinit var manager: OtpManager

    @Before
    fun setup() {
        manager = OtpManager()
    }

    // ──────────────────────────────────────────
    // generate()
    // ──────────────────────────────────────────

    @Test
    fun generate_returnsExactlyFourDigits() {
        val otp = manager.generate()
        assertEquals(4, otp.length)
    }

    @Test
    fun generate_isNumericOnly() {
        val otp = manager.generate()
        assertTrue("OTP must contain only digits", otp.all { it.isDigit() })
    }

    @Test
    fun generate_hasLeadingZeroWhenNeeded() {
        // Run many times to statistically guarantee we see values < 1000
        val otps = (1..200).map { OtpManager().generate() }
        assertTrue("Format must be 4 digits including leading zeros",
            otps.all { it.length == 4 })
    }

    @Test
    fun generate_resetsAttemptsLeftToMax() {
        manager.generate()
        assertEquals(OtpManager.MAX_ATTEMPTS, manager.attemptsLeft)
    }

    @Test
    fun generate_resetsIsExhausted() {
        manager.generate()
        repeat(OtpManager.MAX_ATTEMPTS) { manager.verify("0000") }
        assertTrue(manager.isExhausted)

        manager.generate()
        assertFalse(manager.isExhausted)
    }

    @Test
    fun generate_afterExhaustion_attemptsLeftIsMax() {
        manager.generate()
        repeat(OtpManager.MAX_ATTEMPTS) { manager.verify("0000") }

        manager.generate()
        assertEquals(OtpManager.MAX_ATTEMPTS, manager.attemptsLeft)
    }

    @Test
    fun generate_producesVariety() {
        // Statistical check — 20 independent generators should not all produce the same OTP
        val values = (1..20).map { OtpManager().generate() }.toSet()
        assertTrue("Expected varied OTPs across 20 runs", values.size > 1)
    }

    // ──────────────────────────────────────────
    // verify() — correctness
    // ──────────────────────────────────────────

    @Test
    fun verify_exactMatchReturnsTrue() {
        val otp = manager.generate()
        assertTrue(manager.verify(otp))
    }

    @Test
    fun verify_wrongOtpReturnsFalse() {
        val otp = manager.generate()
        val wrong = if (otp == "1234") "5678" else "1234"
        assertFalse(manager.verify(wrong))
    }

    @Test
    fun verify_partialInput_returnsFalse() {
        manager.generate()
        // 3-digit input can never match a 4-digit OTP
        assertFalse(manager.verify("123"))
    }

    @Test
    fun verify_extraDigits_returnsFalse() {
        manager.generate()
        // 5-digit input can never match a 4-digit OTP
        assertFalse(manager.verify("12345"))
    }

    @Test
    fun verify_emptyInput_returnsFalse() {
        manager.generate()
        assertFalse(manager.verify(""))
    }

    // ──────────────────────────────────────────
    // verify() — normalisation
    // ──────────────────────────────────────────

    @Test
    fun verify_wordedDigits_matchOtp() {
        val otp = manager.generate()
        val wordForm = otp.map { c -> digitToWord(c) }.joinToString(" ")
        assertTrue("Word-form '$wordForm' should match OTP '$otp'", manager.verify(wordForm))
    }

    @Test
    fun verify_upperCaseWordedDigits_matchOtp() {
        val otp = manager.generate()
        val wordForm = otp.map { c -> digitToWord(c).uppercase() }.joinToString(" ")
        assertTrue(manager.verify(wordForm))
    }

    @Test
    fun verify_digitsWithSpaces_matchOtp() {
        val otp = manager.generate()
        val spaced = otp.toList().joinToString(" ")
        assertTrue(manager.verify(spaced))
    }

    @Test
    fun verify_digitsWithCommas_matchOtp() {
        val otp = manager.generate()
        val csv = otp.toList().joinToString(", ")
        assertTrue(manager.verify(csv))
    }

    @Test
    fun verify_mixedWordAndDigit_matchOtp() {
        // e.g. OTP "1two34" - won't typically happen from STT, but normalisation should handle it
        val otp = manager.generate()
        val mixed = otp[0].toString() + digitToWord(otp[1]) + otp[2] + otp[3]
        assertTrue(manager.verify(mixed))
    }

    // ──────────────────────────────────────────
    // verify() — attempt tracking
    // ──────────────────────────────────────────

    @Test
    fun attemptsLeft_startsAtMax() {
        manager.generate()
        assertEquals(OtpManager.MAX_ATTEMPTS, manager.attemptsLeft)
    }

    @Test
    fun attemptsLeft_decreasesWithEachWrongAttempt() {
        manager.generate()
        manager.verify("0000")
        assertEquals(2, manager.attemptsLeft)
        manager.verify("0000")
        assertEquals(1, manager.attemptsLeft)
    }

    @Test
    fun attemptsLeft_decreasesOnCorrectAttemptToo() {
        val otp = manager.generate()
        manager.verify(otp)
        assertEquals(OtpManager.MAX_ATTEMPTS - 1, manager.attemptsLeft)
    }

    @Test
    fun isExhausted_falseBeforeMaxAttempts() {
        manager.generate()
        assertFalse(manager.isExhausted)
        manager.verify("0000")
        assertFalse(manager.isExhausted)
        manager.verify("0000")
        assertFalse(manager.isExhausted)
    }

    @Test
    fun isExhausted_trueAfterMaxAttempts() {
        manager.generate()
        repeat(OtpManager.MAX_ATTEMPTS) { manager.verify("0000") }
        assertTrue(manager.isExhausted)
    }

    @Test
    fun verify_whenExhausted_returnsFalseEvenForCorrectOtp() {
        val otp = manager.generate()
        repeat(OtpManager.MAX_ATTEMPTS) { manager.verify("0000") }
        // Even the correct OTP is rejected after exhaustion
        assertFalse(manager.verify(otp))
    }

    @Test
    fun verify_whenExhausted_doesNotDecrementAttemptsLeftFurther() {
        manager.generate()
        repeat(OtpManager.MAX_ATTEMPTS) { manager.verify("0000") }
        assertEquals(0, manager.attemptsLeft)

        manager.verify("0000") // extra call after exhaustion
        assertEquals(0, manager.attemptsLeft)
    }

    // ──────────────────────────────────────────
    // Constants
    // ──────────────────────────────────────────

    @Test
    fun maxAttempts_isThree() {
        assertEquals(3, OtpManager.MAX_ATTEMPTS)
    }

    // ──────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────

    private fun digitToWord(c: Char) = mapOf(
        '0' to "zero", '1' to "one", '2' to "two", '3' to "three", '4' to "four",
        '5' to "five", '6' to "six", '7' to "seven", '8' to "eight", '9' to "nine"
    ).getValue(c)
}
