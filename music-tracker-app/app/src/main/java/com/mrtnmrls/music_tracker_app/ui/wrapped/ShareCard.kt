package com.mrtnmrls.music_tracker_app.ui.wrapped

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.SongStat
import com.mrtnmrls.music_tracker_app.ui.stats.SelectedMonth
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme

@Composable
internal fun ShareCard(
    month: SelectedMonth,
    topArtists: List<ArtistStat>,
    topSongs: List<SongStat>,
    totalPlays: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        Text(
            text = "Your recap".uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = month.displayName(),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 38.sp),
            modifier = Modifier.padding(top = 4.dp)
        )

        Column(modifier = Modifier.padding(top = 24.dp)) {
            ShareSectionLabel("Top Artists")
            topArtists.forEachIndexed { index, stat ->
                ShareRow(rank = index + 1, primary = stat.artist)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Column {
            ShareSectionLabel("Top Songs")
            topSongs.forEachIndexed { index, stat ->
                ShareRow(rank = index + 1, primary = stat.title, secondary = stat.artist)
            }
        }

        HorizontalDivider(
            modifier = Modifier.width(40.dp).padding(top = 20.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {

            Text(
                text = "$totalPlays",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 60.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "songs played this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShareSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun ShareRow(rank: Int, primary: String, secondary: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%02d".format(rank),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = if (secondary != null) "$primary — $secondary" else primary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(widthDp = 360, heightDp = 680, showBackground = true)
@Composable
private fun ShareCardPreview() {
    MusicTrackerAppTheme {
        ShareCard(
            month = SelectedMonth(year = 2026, month = 8),
            topArtists = listOf(
                ArtistStat("Kali Uchis", 42),
                ArtistStat("Men I Trust", 37),
                ArtistStat("Tame Impala", 29),
                ArtistStat("Rosalía", 24),
                ArtistStat("Bad Bunny", 21)
            ),
            topSongs = listOf(
                SongStat("Telepatía", "Kali Uchis", 18),
                SongStat("Show Me How", "Men I Trust", 15),
                SongStat("The Less I Know The Better", "Tame Impala", 13),
                SongStat("Con Altura", "Rosalía", 12),
                SongStat("Monaco", "Bad Bunny", 11)
            ),
            totalPlays = 164
        )
    }
}