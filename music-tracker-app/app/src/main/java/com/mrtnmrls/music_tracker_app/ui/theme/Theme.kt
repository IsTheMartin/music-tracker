package com.mrtnmrls.music_tracker_app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary          = LightAccent,
    onPrimary        = LightBackground,
    background       = LightBackground,
    onBackground     = LightText,
    surface          = LightBackground,
    onSurface        = LightText,
    surfaceVariant   = LightSurfaceVar,
    onSurfaceVariant = LightOnSurfaceVar,
    outlineVariant   = LightDivider,
)

private val DarkColorScheme = darkColorScheme(
    primary          = DarkAccent,
    onPrimary        = DarkBackground,
    background       = DarkBackground,
    onBackground     = DarkText,
    surface          = DarkBackground,
    onSurface        = DarkText,
    surfaceVariant   = DarkSurfaceVar,
    onSurfaceVariant = DarkOnSurfaceVar,
    outlineVariant   = DarkDivider,
)

@Composable
fun MusicTrackerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}