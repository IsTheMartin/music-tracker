package com.mrtnmrls.music_tracker_app.ui.onboarding

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(app: Application): AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun handleIntent(intent: OnboardingIntent) {
        when(intent) {
            OnboardingIntent.CheckPermission -> checkPermission()
            OnboardingIntent.OpenNotificationSettings -> openSettings()
        }
    }

    private fun checkPermission() {
        val granted = NotificationManagerCompat
            .getEnabledListenerPackages(getApplication())
            .contains(getApplication<Application>().packageName)
        _uiState.update { it.copy(hasPermission = granted) }
    }

    private fun openSettings() {
        getApplication<Application>()
            .startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
    }

}