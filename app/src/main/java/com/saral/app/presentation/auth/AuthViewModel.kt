package com.saral.app.presentation.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _authStep = MutableStateFlow<AuthStep>(AuthStep.BiometricPending)
    val authStep: StateFlow<AuthStep> = _authStep.asStateFlow()

    private var speakCallback: ((String) -> Unit)? = null

    fun setSpeakCallback(callback: (String) -> Unit) {
        speakCallback = callback
    }

    fun onBiometricSuccess() {
        _authStep.value = AuthStep.Success
        speakCallback?.invoke("Welcome to Saral. You can say: check balance, transfer money, request cheque book, show recent transactions, or help.")
    }

    fun reset() {
        _authStep.value = AuthStep.BiometricPending
    }
}
