package com.saral.app.presentation.auth

class OtpManager {
    companion object {
        const val MAX_ATTEMPTS = 3
    }

    private var currentOtp: String = ""
    private var attempts = 0

    fun generate(): String {
        currentOtp = String.format("%04d", (0..9999).random())
        attempts = 0
        return currentOtp
    }

    fun verify(input: String): Boolean {
        if (attempts >= MAX_ATTEMPTS) return false
        attempts++
        return normalizeInput(input) == currentOtp
    }

    private fun normalizeInput(input: String): String =
        input.lowercase()
            .replace("zero", "0").replace("one", "1").replace("two", "2")
            .replace("three", "3").replace("four", "4").replace("five", "5")
            .replace("six", "6").replace("seven", "7").replace("eight", "8")
            .replace("nine", "9")
            .filter { it.isDigit() }

    val attemptsLeft: Int get() = MAX_ATTEMPTS - attempts
    val isExhausted: Boolean get() = attempts >= MAX_ATTEMPTS
}
