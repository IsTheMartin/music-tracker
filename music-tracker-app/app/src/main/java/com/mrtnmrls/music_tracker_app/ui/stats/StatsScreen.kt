package com.mrtnmrls.music_tracker_app.ui.stats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.SongStat
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val selectedMonth = (uiState as? StatsUiState.Success)?.selectedMonth
    LaunchedEffect(selectedMonth) {
        scrollBehavior.state.heightOffset = 0f
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Listening recap".uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState is StatsUiState.Success) {
                            MonthSelector(
                                displayName = (uiState as StatsUiState.Success).selectedMonth.displayName(),
                                onPrevious = { viewModel.handleIntent(StatsIntent.PreviousMonth) },
                                onNext = { viewModel.handleIntent(StatsIntent.NextMonth) }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        StatsContent(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun StatsContent(
    uiState: StatsUiState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is StatsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is StatsUiState.Success -> {
            SuccessContent(state = uiState, modifier = modifier)
        }
    }
}

@Composable
private fun SuccessContent(
    state: StatsUiState.Success,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item { SectionHeader("Top Artists") }
        if (state.topArtists.isEmpty()) {
            item { EmptyHint("No plays recorded this month.") }
        } else {
            itemsIndexed(state.topArtists) { index, stat ->
                StatRow(rank = index + 1, primary = stat.artist, playCount = stat.playCount)
                HorizontalDivider()
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item { SectionHeader("Top Songs") }
        if (state.topSongs.isEmpty()) {
            item { EmptyHint("No plays recorded this month.") }
        } else {
            itemsIndexed(state.topSongs) { index, stat ->
                StatRow(
                    rank = index + 1,
                    primary = stat.title,
                    secondary = stat.artist,
                    artUri = stat.artUri.takeIf { it.isNotEmpty() },
                    playCount = stat.playCount
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MonthSelector(displayName: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) { Text("‹") }
        Text(text = displayName, style = MaterialTheme.typography.headlineLarge)
        IconButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun StatRow(
    rank: Int,
    primary: String,
    secondary: String? = null,
    artUri: String? = null,
    playCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "%02d".format(rank),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.primary
            )
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Column {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (secondary != null) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = "$playCount plays",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StatsLoadingPreview() {
    MusicTrackerAppTheme {
        StatsContent(uiState = StatsUiState.Loading)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StatsEmptyPreview() {
    MusicTrackerAppTheme {
        StatsContent(
            uiState = StatsUiState.Success(
                selectedMonth = SelectedMonth.current(),
                topArtists = emptyList(),
                topSongs = emptyList()
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StatsWithDataPreview() {
    MusicTrackerAppTheme {
        StatsContent(
            uiState = StatsUiState.Success(
                selectedMonth = SelectedMonth.current(),
                topArtists = listOf(
                    ArtistStat("The Weeknd", 14),
                    ArtistStat("Kendrick Lamar", 9),
                    ArtistStat("Tyler, the Creator", 6)
                ),
                topSongs = listOf(
                    SongStat("Blinding Lights", "The Weeknd", 5),
                    SongStat("HUMBLE.", "Kendrick Lamar", 4),
                    SongStat("See You Again", "Tyler, the Creator", 3)
                )
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun MonthSelectorPreview() {
    MusicTrackerAppTheme {
        MonthSelector(displayName = "August 2026", onPrevious = {}, onNext = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StatRowArtistPreview() {
    MusicTrackerAppTheme {
        Surface {
            StatRow(rank = 1, primary = "The Weeknd", playCount = 14)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatRowSongPreview() {
    MusicTrackerAppTheme {
        Surface {
            StatRow(rank = 1, primary = "Blinding Lights", secondary = "The Weeknd", artUri = null, playCount = 5)
        }
    }
}