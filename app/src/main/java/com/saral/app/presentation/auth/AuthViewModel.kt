package com.saral.app.presentation.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val otpManager = OtpManager()
    private val _authStep = MutableStateFlow<AuthStep>(AuthStep.BiometricPending)
    val authStep: StateFlow<AuthStep> = _authStep.asStateFlow()

    private var speakCallback: ((String, (() -> Unit)?) -> Unit)? = null

    fun setSpeakCallback(callback: (String, (() -> Unit)?) -> Unit) {
        speakCallback = callback
    }

    fun onBiometricSuccess() {
        startOtpFlow()
    }

    private fun startOtpFlow() {
        val otp = otpManager.generate()
        _authStep.value = AuthStep.OtpCalling
        val otpSpoken = otp.toCharArray().joinToString(", ")
        speakCallback?.invoke(
            "This is an automated call from Saral Bank. Your one-time password is $otpSpoken. Please say this OTP to verify.",
            {
                _authStep.value = AuthStep.OtpInput(OtpManager.MAX_ATTEMPTS)
                speakCallback?.invoke("Please say your 4-digit OTP now.", null)
            }
        )
    }

    fun onVoiceInput(spoken: String) {
        when (_authStep.value) {
            is AuthStep.OtpInput -> verifyOtp(spoken)
            is AuthStep.OtpExhausted -> handleRegenerateResponse(spoken)
            else -> Unit
        }
    }

    private fun verifyOtp(spoken: String) {
        when {
            otpManager.verify(spoken) -> {
                speakCallback?.invoke("OTP verified successfully. Welcome user.", {
                    speakCallback?.invoke(
                        "You can say: check balance, transfer money, request cheque book, show recent transactions, or help.",
                        null
                    )
                })
                _authStep.value = AuthStep.Success
            }
            otpManager.isExhausted -> {
                _authStep.value = AuthStep.OtpExhausted
                speakCallback?.invoke(
                    "Incorrect OTP entered 3 times. Say yes to regenerate a new OTP, or no to cancel.",
                    null
                )
            }
            else -> {
                val left = otpManager.attemptsLeft
                speakCallback?.invoke(
                    "Incorrect OTP. You have $left attempt${if (left == 1) "" else "s"} left. Please try again.",
                    {
                        _authStep.value = AuthStep.OtpInput(left)
                        speakCallback?.invoke("Please say your OTP now.", null)
                    }
                )
            }
        }
    }

    private fun handleRegenerateResponse(spoken: String) {
        val lower = spoken.lowercase()
        if (lower.contains("yes") || lower.contains("haan") || lower.contains("ok")) {
            startOtpFlow()
        } else {
            _authStep.value = AuthStep.BiometricPending
            speakCallback?.invoke(
                "Authentication cancelled. Please place your finger on the sensor to try again.",
                null
            )
        }
    }

    fun reset() {
        _authStep.value = AuthStep.BiometricPending
    }
}
