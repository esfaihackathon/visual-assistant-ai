package com.saral.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.saral.app.accessibility.HapticManager
import com.saral.app.navigation.SaralNavGraph
import java.util.Locale
import com.saral.app.presentation.auth.AuthViewModel
import com.saral.app.presentation.home.HomeViewModel
import com.saral.app.presentation.transfer.TransferViewModel
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.SaralTheme
import com.saral.app.voice.SpeechRecognizerManager
import com.saral.app.voice.TextToSpeechManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executor
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var ttsManager: TextToSpeechManager
    @Inject lateinit var speechManager: SpeechRecognizerManager
    @Inject lateinit var hapticManager: HapticManager

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val transferViewModel: TransferViewModel by viewModels()
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private var navController: androidx.navigation.NavHostController? = null
    private var pendingBiometricSuccessAction: (() -> Unit)? = null

    // ── Session timeout (Feature 2) ──────────────────────────────────────────
    private companion object {
        const val SESSION_IDLE_MS    = 3 * 60 * 1000L   // 3 min before warning
        const val SESSION_WARNING_MS = 30 * 1000L        // 30 s from warning to expiry
    }
    private var sessionJob: Job? = null

    private val ttsReadyState = mutableStateOf(false)
    private val availableVoicesState = mutableStateOf<List<String>>(emptyList())
    private val selectedVoiceNameState = mutableStateOf<String?>(null)
    private val selectedLanguageState = mutableStateOf("English")
    private val speechRateState = mutableStateOf(0.88f)
    private val hapticEnabledState = mutableStateOf(true)
    private val fontScaleState = mutableFloatStateOf(1.0f)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            ttsManager.speak("Microphone permission is required for voice commands.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executor = ContextCompat.getMainExecutor(this)
        setupBiometric()
        initializeVoice()
        requestMicPermission()

        authViewModel.setSpeakCallback { text ->
            ttsManager.speak(text)
        }
        // Start session timer once the user reaches home (auth success fires this callback)
        homeViewModel.setSpeakCallback { text ->
            ttsManager.speak(text)
        }

        transferViewModel.setSpeakCallback { text ->
            ttsManager.speak(text)
        }

        setContent {
            val fontScale by fontScaleState
            val systemDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(systemDensity.density, fontScale)
            ) {
                SaralTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color    = NavyDark
                    ) {
                        val navCtrl = rememberNavController()
                        navController = navCtrl

                        val isListening     by speechManager.isListening.collectAsState()
                        val availableVoices by availableVoicesState
                        val selectedVoiceName by selectedVoiceNameState
                        val selectedLanguage  by selectedLanguageState
                        val speechRate        by speechRateState
                        val hapticEnabled     by hapticEnabledState

                        SaralNavGraph(
                            navController   = navCtrl,
                            onSpeak         = { text -> ttsManager.speak(text) },
                            onAuthenticate  = { showBiometricPrompt { authViewModel.onBiometricSuccess(); resetSessionTimeout() } },
                            onMicClick      = { onMicButtonClick() },
                            onTextCommand   = { command -> resetSessionTimeout(); homeViewModel.onVoiceResult(command) },
                            onQuickCommand  = { command -> resetSessionTimeout(); homeViewModel.onVoiceResult(command) },
                            availableVoices = availableVoices,
                            selectedVoice   = selectedVoiceName,
                            selectedLanguage = selectedLanguage,
                            speechRate      = speechRate,
                            hapticEnabled   = hapticEnabled,
                            fontScale       = fontScale,
                            onLanguageSelected = { language ->
                                selectedLanguageState.value = language
                                val locale = if (language == "Hindi") Locale("hi", "IN") else Locale("en", "IN")
                                ttsManager.setLanguage(locale)
                                speechManager.setLanguage(locale)
                                selectedVoiceNameState.value = ttsManager.getSelectedVoiceName()
                            },
                            onSpeechRateChanged = { rate ->
                                speechRateState.value = rate
                                ttsManager.setSpeechRate(rate)
                            },
                            onVoiceSelected = { voiceName ->
                                selectedVoiceNameState.value = voiceName
                                ttsManager.setVoice(voiceName)
                            },
                            onHapticToggled = { enabled ->
                                hapticEnabledState.value = enabled
                            },
                            onFontScaleChanged = { scale ->
                                fontScaleState.floatValue = scale
                            },
                            onTransferMicClick = { onTransferMicButtonClick() },
                            onRequestTransferBiometric = { showBiometricPrompt { transferViewModel.onBiometricSuccess() } },
                            isListening    = isListening,
                            homeViewModel  = homeViewModel,
                            authViewModel  = authViewModel,
                            transferViewModel = transferViewModel
                        )
                    }
                }
            }
        }
    }

    private fun initializeVoice() {
        ttsManager.initialize {
            ttsReadyState.value = true
            availableVoicesState.value = ttsManager.getAvailableVoiceNames()
            selectedVoiceNameState.value = ttsManager.getSelectedVoiceName()
        }
        speechManager.initialize()
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupBiometric() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    hapticManager.vibrateSuccess()
                    pendingBiometricSuccessAction?.invoke()
                    pendingBiometricSuccessAction = null
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    hapticManager.vibrateError()
                    ttsManager.speak("Authentication failed. Please try again.")
                    transferViewModel.onBiometricFailed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        hapticManager.vibrateSuccess()
                        pendingBiometricSuccessAction?.invoke()
                        pendingBiometricSuccessAction = null
                    }
                }
            }
        )
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        pendingBiometricSuccessAction = onSuccess
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Saral Authentication")
                .setSubtitle("Authenticate to access your banking")
                .setNegativeButtonText("Cancel")
                .build()
            biometricPrompt.authenticate(promptInfo)
        } else {
            hapticManager.vibrateSuccess()
            onSuccess()
            pendingBiometricSuccessAction = null
        }
    }

    // Reset inactivity timer on every user interaction
    fun resetSessionTimeout() {
        sessionJob?.cancel()
        sessionJob = lifecycleScope.launch {
            delay(SESSION_IDLE_MS)
            ttsManager.speak(
                "Your session will expire in 30 seconds due to inactivity. " +
                "Tap or speak to continue."
            )
            delay(SESSION_WARNING_MS)
            authViewModel.reset()
            navController?.navigate(com.saral.app.navigation.Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
            ttsManager.speak("Session expired. Please authenticate again.")
        }
    }

    private fun onMicButtonClick() {
        resetSessionTimeout()
        hapticManager.vibrateShort()
        if (speechManager.isListening.value) {
            speechManager.stopListening()
            homeViewModel.setListening(false)
        } else {
            ttsManager.stop()
            homeViewModel.setListening(true)
            speechManager.startListening { result ->
                homeViewModel.setListening(false)
                homeViewModel.onVoiceResult(result)
            }
        }
    }

    private fun onTransferMicButtonClick() {
        resetSessionTimeout()
        hapticManager.vibrateShort()
        if (speechManager.isListening.value) {
            speechManager.stopListening()
        } else {
            ttsManager.stop()
            speechManager.startListening { result ->
                transferViewModel.onVoiceInput(result)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        speechManager.destroy()
    }
}
