package com.mrtnmrls.music_tracker_app.ui.wrapped

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme
import kotlinx.coroutines.delay
import kotlin.collections.forEachIndexed

@Composable
internal fun RankedPage(
    eyebrow: String,
    title: String,
    caption: String,
    rows: List<RankRowData>,
    visible: Boolean
) {

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
        )

        if (rows.isEmpty()) {
            Text("No plays recorded this month.", style = MaterialTheme.typography.bodySmall)
        } else {
            rows.forEachIndexed { index, row ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(450, delayMillis = 60 + index * 90)) + slideInVertically(
                        tween(450, delayMillis = 60 + index * 90)
                    ) { it / 3 }
                ) {
                    RankRow(row, isTop = index == 0)
                }
            }
        }
    }
}

@Composable
private fun RankRow(row: RankRowData, isTop: Boolean) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(key1 = isTop) {
        if (isTop) {
            delay(500)
            scale.animateTo(1.15f, tween(200))
            scale.animateTo(1f, tween(200))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%02d".format(row.rank),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = if (isTop) 30.sp else 26.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(44.dp)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        )
        if (row.artUri != null) {
            AsyncImage(
                model = row.artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = row.primary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            row.secondary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "${row.playCount} plays",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankedPageArtistsPreview() {
    MusicTrackerAppTheme {
        RankedPage(
            eyebrow = "01 — 05",
            title = "Top Artists",
            caption = "Your most-played artists this month",
            rows = listOf(
                RankRowData(rank = 1, primary = "Kali Uchis", playCount = 42),
                RankRowData(rank = 2, primary = "Men I Trust", playCount = 37),
                RankRowData(rank = 3, primary = "Tame Impala", playCount = 29),
                RankRowData(rank = 4, primary = "Rosalía", playCount = 24),
                RankRowData(rank = 5, primary = "Bad Bunny", playCount = 21)
            ),
            visible = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankedPageSongsPreview() {
    MusicTrackerAppTheme {
        RankedPage(
            eyebrow = "01 — 05",
            title = "Top Songs",
            caption = "Your most-played tracks this month",
            rows = listOf(
                RankRowData(rank = 1, primary = "Telepatía", secondary = "Kali Uchis", playCount = 18),
                RankRowData(rank = 2, primary = "Show Me How", secondary = "Men I Trust", playCount = 15),
                RankRowData(rank = 3, primary = "The Less I Know The Better", secondary = "Tame Impala", playCount = 13),
                RankRowData(rank = 4, primary = "Con Altura", secondary = "Rosalía", playCount = 12),
                RankRowData(rank = 5, primary = "Monaco", secondary = "Bad Bunny", playCount = 11)
            ),
            visible = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankedPageEmptyPreview() {
    MusicTrackerAppTheme {
        RankedPage(
            eyebrow = "01 — 05",
            title = "Top Artists",
            caption = "Your most-played artists this month",
            rows = emptyList(),
            visible = true
        )
    }
}