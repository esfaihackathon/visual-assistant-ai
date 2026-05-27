package com.saral.app.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.saral.app.presentation.auth.AuthScreen
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
    homeViewModel: HomeViewModel
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
            AuthScreen(
                onAuthenticate = onAuthenticate,
                onSpeak = onSpeak
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
