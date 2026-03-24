package com.carinfo.ar.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carinfo.ar.data.SupportedCountry
import com.carinfo.ar.data.UserPreferences
import com.carinfo.ar.ui.screens.HistoryScreen
import kotlinx.coroutines.runBlocking
import com.carinfo.ar.ui.screens.OnboardingScreen
import com.carinfo.ar.ui.screens.SettingsScreen
import com.carinfo.ar.ui.screens.SplashScreen
import com.carinfo.ar.ui.CameraScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val onboardingComplete by UserPreferences.isOnboardingComplete(context).collectAsState(initial = null)

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
        composable(
            Routes.SPLASH,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(500)) }
        ) {
            SplashScreen(
                onFinished = {
                    // Auto-detect country only on first launch (SIM/network → locale fallback)
                    val autoCountry = SupportedCountry.detect(context)
                    val dest = if (onboardingComplete == true) {
                        Routes.CAMERA
                    } else if (autoCountry != null) {
                        runBlocking {
                            UserPreferences.setSelectedCountry(context, autoCountry.code)
                            UserPreferences.setOnboardingComplete(context, true)
                        }
                        Routes.CAMERA
                    } else {
                        Routes.ONBOARDING
                    }
                    navController.navigate(dest) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.CAMERA) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
