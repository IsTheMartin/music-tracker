package com.mrtnmrls.music_tracker_app.ui.onboarding

sealed interface OnboardingIntent {
    object OpenNotificationSettings : OnboardingIntent
    object CheckPermission : OnboardingIntent
}