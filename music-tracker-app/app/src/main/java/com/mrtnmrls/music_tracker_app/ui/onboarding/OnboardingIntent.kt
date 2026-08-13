package com.mrtnmrls.music_tracker_app.ui.onboarding

sealed interface OnboardingIntent {
    data object OpenNotificationSettings : OnboardingIntent
    data object CheckPermission : OnboardingIntent
}