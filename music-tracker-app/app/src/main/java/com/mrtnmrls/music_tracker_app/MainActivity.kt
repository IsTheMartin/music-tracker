package com.mrtnmrls.music_tracker_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import com.mrtnmrls.music_tracker_app.ui.navigation.AppNavigation
import com.mrtnmrls.music_tracker_app.ui.navigation.startDestination
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasPermission = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)

        setContent {
            MusicTrackerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavigation(startDestination = startDestination(hasPermission))
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicTrackerAppTheme {
        Greeting("Android")
    }
}