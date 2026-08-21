package com.mrtnmrls.music_tracker_app.ui.wrapped

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.absoluteValue

@Composable
internal fun WrappedScreen(
    onClose: () -> Unit,
    viewModel: WrappedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            WrappedUiState.Loading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is WrappedUiState.Success -> {
                WrappedContent(state, onClose)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WrappedContent(state: WrappedUiState.Success, onClose: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HorizontalPager(pagerState) { page ->
            val pageOffset =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val depth = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                        scaleX = lerp(0.92f, 1f, depth)
                        scaleY = lerp(0.92f, 1f, depth)
                        alpha = lerp(0.4f, 1f, depth)
                    }
                    .padding(horizontal = 16.dp, vertical = 56.dp)
            ) {
                when (page) {
                    0 -> IntroPage(modifier = Modifier, month = state.month)
                    1 -> RankedPage(
                        eyebrow = "01 - 05",
                        title = "Top Artists",
                        caption = "Your most-played artist this month",
                        rows = state.topArtists.mapIndexed { index, stat ->
                            RankRowData(
                                rank = index + 1,
                                primary = stat.artist,
                                playCount = stat.playCount
                            )
                        },
                        visible = pagerState.settledPage == page
                    )

                    2 -> RankedPage(
                        eyebrow = "01 - 05",
                        title = "Top Songs",
                        caption = "Your most-played tracks this month",
                        rows = state.topSongs.mapIndexed { index, stat ->
                            RankRowData(
                                rank = index + 1,
                                primary = stat.title,
                                secondary = stat.artist,
                                artUri = stat.artUri.takeIf { it.isNotEmpty() },
                                playCount = stat.playCount
                            )
                        },
                        visible = pagerState.settledPage == page
                    )

                    else -> OutroPage(
                        totalPlays = state.totalPlays
                    )
                }
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(vertical = 32.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        PagerDots(
            pageCount = 4,
            currentPage = pagerState.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun PagerDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            val width by animateDpAsState(
                targetValue = if (active) 20.dp else 6.dp,
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant)
            ) { }
        }
    }
}