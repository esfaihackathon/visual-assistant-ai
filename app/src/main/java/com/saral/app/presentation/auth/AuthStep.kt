package com.saral.app.presentation.auth

sealed class AuthStep {
    object BiometricPending : AuthStep()
    object Success : AuthStep()
}
