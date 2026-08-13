package com.mrtnmrls.music_tracker_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrtnmrls.music_tracker_app.ui.onboarding.OnboardingScreen
import com.mrtnmrls.music_tracker_app.ui.stats.StatsScreen

private const val ONBOARDING_ROUTE = "onboarding"
private const val STATS_ROUTE = "stats"

@Composable
internal fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ONBOARDING_ROUTE) {
            OnboardingScreen(
                onPermissionGranted = {
                    navController.navigate(STATS_ROUTE) {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(STATS_ROUTE) {
            StatsScreen()
        }
    }
}

fun startDestination(hasPermission: Boolean) =
    if (hasPermission) STATS_ROUTE else ONBOARDING_ROUTE