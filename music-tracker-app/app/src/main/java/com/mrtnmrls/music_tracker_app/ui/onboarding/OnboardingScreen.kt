package com.mrtnmrls.music_tracker_app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme

@Composable
internal fun OnboardingScreen(
    onPermissionGranted: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(key1 = lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
            viewModel.handleIntent(OnboardingIntent.CheckPermission)
        }
    }

    LaunchedEffect(key1 = uiState.hasPermission) {
        if (uiState.hasPermission) onPermissionGranted()
    }

    OnboardingContent(
        onGrantClicked = { viewModel.handleIntent(OnboardingIntent.OpenNotificationSettings) }
    )
}

@Composable
private fun OnboardingContent(
    onGrantClicked: () -> Unit
) {
    Column(
        modifier= Modifier.fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Music Tracker", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Grant notification access so the app can track what you play in your music apps.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onGrantClicked) {
            Text(text = "Grant access")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    MusicTrackerAppTheme {
        OnboardingContent {  }
    }
}