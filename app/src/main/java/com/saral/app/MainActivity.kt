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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.saral.app.accessibility.HapticManager
import com.saral.app.navigation.Routes
import com.saral.app.navigation.SaralNavGraph
import com.saral.app.presentation.home.HomeViewModel
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
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private var navController: androidx.navigation.NavHostController? = null

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

        homeViewModel.setSpeakCallback { text ->
            ttsManager.speak(text)
        }

        setContent {
            SaralTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyDark
                ) {
                    val navCtrl = rememberNavController()
                    navController = navCtrl

                    SaralNavGraph(
                        navController = navCtrl,
                        onSpeak = { text -> ttsManager.speak(text) },
                        onAuthenticate = { showBiometricPrompt() },
                        onMicClick = { onMicButtonClick() },
                        homeViewModel = homeViewModel
                    )
                }
            }
        }
    }

    private fun initializeVoice() {
        ttsManager.initialize()
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
                    ttsManager.speak("Authentication successful. Welcome Jayesh.") {
                        ttsManager.speak("You can now say commands like check balance, transfer money, or request cheque book.")
                    }
                    navController?.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    hapticManager.vibrateError()
                    ttsManager.speak("Authentication failed. Please try again.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        // For demo: bypass auth and go to home
                        hapticManager.vibrateSuccess()
                        ttsManager.speak("Demo mode. Welcome Jayesh.") {
                            ttsManager.speak("You can now say commands like check balance, transfer money, or request cheque book.")
                        }
                        navController?.navigate(Routes.HOME) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    }
                }
            }
        )
    }

    private fun showBiometricPrompt() {
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
            // No biometric hardware — proceed in demo mode
            hapticManager.vibrateSuccess()
            ttsManager.speak("Authentication successful. Welcome Jayesh.") {
                ttsManager.speak("You can now say commands like check balance, transfer money, or request cheque book.")
            }
            navController?.navigate(Routes.HOME) {
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        }
    }

    private fun onMicButtonClick() {
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

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        speechManager.destroy()
    }
}
