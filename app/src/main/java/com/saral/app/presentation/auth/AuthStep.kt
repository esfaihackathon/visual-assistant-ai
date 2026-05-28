package com.saral.app.presentation.auth

sealed class AuthStep {
    object BiometricPending : AuthStep()
    object OtpCalling : AuthStep()
    data class OtpInput(val attemptsLeft: Int) : AuthStep()
    object OtpExhausted : AuthStep()
    object Success : AuthStep()
}
