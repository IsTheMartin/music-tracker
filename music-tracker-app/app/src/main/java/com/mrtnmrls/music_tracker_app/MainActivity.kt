package com.mrtnmrls.music_tracker_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import com.mrtnmrls.music_tracker_app.ui.navigation.AppNavigation
import com.mrtnmrls.music_tracker_app.ui.navigation.startDestination
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasPermission = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)

        setContent {
            MusicTrackerAppTheme {
                AppNavigation(startDestination = startDestination(hasPermission))
            }
        }
    }
}
