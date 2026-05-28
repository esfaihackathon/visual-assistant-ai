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
import com.saral.app.presentation.home.HomeScreen
import com.saral.app.presentation.home.HomeViewModel
import com.saral.app.presentation.settings.SettingsScreen
import com.saral.app.presentation.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun SaralNavGraph(
    navController: NavHostController,
    onSpeak: (String) -> Unit,
    onAuthenticate: () -> Unit,
    onMicClick: () -> Unit,
    onOtpMicClick: () -> Unit,
    isListening: Boolean,
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel
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
                    else -> Unit
                }
            }

            AuthScreen(
                authStep = authStep,
                isListening = isListening,
                onAuthenticate = onAuthenticate,
                onOtpMicClick = onOtpMicClick,
                onRegenerate = { authViewModel.onVoiceInput("yes") },
                onCancel = { authViewModel.onVoiceInput("no") }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onMicClick = onMicClick,
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
