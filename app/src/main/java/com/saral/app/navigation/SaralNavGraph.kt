package com.saral.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.saral.app.presentation.auth.AuthScreen
import com.saral.app.presentation.auth.AuthStep
import com.saral.app.presentation.auth.AuthViewModel
import com.saral.app.presentation.home.HomeNavigationEvent
import com.saral.app.presentation.home.HomeScreen
import com.saral.app.presentation.home.HomeViewModel
import com.saral.app.presentation.settings.SettingsScreen
import com.saral.app.presentation.splash.SplashScreen
import com.saral.app.presentation.transfer.TransferScreen
import com.saral.app.presentation.transfer.TransferViewModel

object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val TRANSFER = "transfer"
}

@Composable
fun SaralNavGraph(
    navController: NavHostController,
    onSpeak: (String) -> Unit,
    onAuthenticate: () -> Unit,
    onMicClick: () -> Unit,
    onTextCommand: (String) -> Unit,
    onQuickCommand: (String) -> Unit,
    availableVoices: List<String>,
    selectedVoice: String?,
    selectedLanguage: String,
    speechRate: Float,
    hapticEnabled: Boolean,
    onLanguageSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onHapticToggled: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChanged: (Float) -> Unit,
    onTransferMicClick: () -> Unit,
    onRequestTransferBiometric: () -> Unit,
    isListening: Boolean,
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    transferViewModel: TransferViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onSpeak = onSpeak
            )
        }

        composable(Routes.AUTH) {
            val authStep by authViewModel.authStep.collectAsState()

            LaunchedEffect(authStep) {
                when (authStep) {
                    is AuthStep.BiometricPending ->
                        onSpeak("Please press your finger for authentication.")
                    is AuthStep.Success ->
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                }
            }

            AuthScreen(
                authStep = authStep,
                onAuthenticate = onAuthenticate
            )
        }

        composable(Routes.HOME) {
            // Observe navigation events from HomeViewModel
            LaunchedEffect(homeViewModel) {
                homeViewModel.navigationEvent.collect { event ->
                    when (event) {
                        is HomeNavigationEvent.NavigateToTransfer ->
                            navController.navigate(Routes.TRANSFER)
                    }
                }
            }

            HomeScreen(
                viewModel = homeViewModel,
                onMicClick = onMicClick,
                onTextCommand = onTextCommand,
                onQuickCommand = onQuickCommand,
                isListening = isListening,
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack            = { navController.popBackStack() },
                availableVoices   = availableVoices,
                selectedVoice     = selectedVoice,
                selectedLanguage  = selectedLanguage,
                speechRate        = speechRate,
                hapticEnabled     = hapticEnabled,
                fontScale         = fontScale,
                onLanguageSelected  = onLanguageSelected,
                onSpeechRateChanged = onSpeechRateChanged,
                onVoiceSelected     = onVoiceSelected,
                onHapticToggled     = onHapticToggled,
                onFontScaleChanged  = onFontScaleChanged
            )
        }

        composable(Routes.TRANSFER) {
            TransferScreen(
                viewModel = transferViewModel,
                isListening = isListening,
                onMicClick = onTransferMicClick,
                onRequestBiometric = onRequestTransferBiometric,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
