package com.mrtnmrls.music_tracker_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrtnmrls.music_tracker_app.ui.onboarding.OnboardingScreen
import com.mrtnmrls.music_tracker_app.ui.stats.StatsScreen
import com.mrtnmrls.music_tracker_app.ui.wrapped.WrappedScreen

private const val ONBOARDING_ROUTE = "onboarding"
private const val STATS_ROUTE = "stats"
private const val WRAPPED_ROUTE = "wrapped"

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
            StatsScreen(
                onOpenWrapped = {
                    navController.navigate(WRAPPED_ROUTE)
                }
            )
        }

        composable(WRAPPED_ROUTE) {
            WrappedScreen(
                onClose = {
                    navController.popBackStack()
                }
            )
        }
    }
}

fun startDestination(hasPermission: Boolean) =
    if (hasPermission) STATS_ROUTE else ONBOARDING_ROUTE