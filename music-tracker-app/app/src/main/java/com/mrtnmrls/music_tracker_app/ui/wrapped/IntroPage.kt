package com.mrtnmrls.music_tracker_app.ui.wrapped

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtnmrls.music_tracker_app.ui.stats.SelectedMonth
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme

@Composable
fun IntroPage(modifier: Modifier = Modifier, month: SelectedMonth) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Your recap".uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = month.displayName(),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 56.sp,
                lineHeight = 56.sp
            )
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "A month of listening, distilled into five artists and five songs.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))

        SwipeToStart()
    }
}

@Composable
private fun SwipeToStart() {
    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffset"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Swipe to see your top artists".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "→",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 6.dp)
                .graphicsLayer {
                    translationX = offsetX
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IntroPagePreview() {
    MusicTrackerAppTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            IntroPage(
                modifier = Modifier.padding(paddingValues),
                month = SelectedMonth(year = 2026, month = 8)
            )
        }
    }
}