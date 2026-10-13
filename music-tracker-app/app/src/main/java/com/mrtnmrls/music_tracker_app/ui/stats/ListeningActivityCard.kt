package com.mrtnmrls.music_tracker_app.ui.stats

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrtnmrls.music_tracker_app.domain.model.HeatMapActivity
import com.mrtnmrls.music_tracker_app.domain.model.SelectedMonth
import com.mrtnmrls.music_tracker_app.ui.theme.MusicTrackerAppTheme
import kotlin.math.ceil

private val CELL_SIZE: Dp = 24.dp
private val CELL_SPACING: Dp = 3.dp

// Index 0 = Monday ... 6 = Sunday, matching the design's row labels.
private val WEEKDAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Collapsible "Listening Activity" card for StatsScreen.
 * `expanded` is purely local UI state: unkeyed to [selectedMonth] on purpose, so it
 * survives month navigation instead of resetting every time the user changes month.
 */
@Composable
fun ListeningActivityCard(
    selectedMonth: SelectedMonth,
    heatMapActivity: HeatMapActivity,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val totalPlays = heatMapActivity.playsPerDay.sum()

    Surface(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Listening Activity",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$totalPlays plays this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Avg. ${"%.1f".format(heatMapActivity.averagePlaysPerDay)} / day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                HeatMapGrid(
                    selectedMonth = selectedMonth,
                    playsPerDay = heatMapActivity.playsPerDay
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (totalPlays > 0) {
                        Text(
                            text = "⚡ Most active: ${formatHour(heatMapActivity.startPeakHour)} - " +
                                    formatHour(heatMapActivity.endPeakHour),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Legend()
                }
            }
        }
    }
}

@Composable
private fun HeatMapGrid(
    selectedMonth: SelectedMonth,
    playsPerDay: List<Int>,
    modifier: Modifier = Modifier
) {
    val daysInMonth = playsPerDay.size
    val firstDayOffset = remember(selectedMonth) { firstWeekdayOffset(selectedMonth) }
    val maxCount = playsPerDay.maxOrNull() ?: 0
    val totalCells = firstDayOffset + daysInMonth
    val weeks = (totalCells + 6) / 7

    Column (
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CELL_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WeekdayLabels()

        Column (verticalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
            for (week in 0 until weeks) {
                Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                    for (weekday in 0..6) {
                        val cellIndex = week * 7 + weekday
                        val day = cellIndex - firstDayOffset + 1
                        if (day in 1..daysInMonth) {
                            HeatMapCell(level = levelFor(playsPerDay[day - 1], maxCount))
                        } else {
                            Spacer(modifier = Modifier.size(CELL_SIZE))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabels(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)
    ) {
        WEEKDAY_LABELS.forEach { label ->
            Box(
                modifier = Modifier.size(CELL_SIZE),
                contentAlignment = Alignment.Center
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatMapCell(level: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(CELL_SIZE)
            .clip(RoundedCornerShape(5.dp))
            .background(levelColor(level))
    )
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (0..4).forEach { level ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(levelColor(level))
                )
            }
        }
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun levelColor(level: Int): Color {
    if (level <= 0) return MaterialTheme.colorScheme.surfaceVariant
    val alphas = listOf(0.3f, 0.5f, 0.75f, 1f)
    return MaterialTheme.colorScheme.primary.copy(
        alpha = alphas[(level - 1).coerceIn(0, alphas.lastIndex)]
    )
}

/** 0 = no plays; 1-4 = intensity scaled proportionally to the month's busiest day. */
private fun levelFor(count: Int, maxCount: Int): Int {
    if (count <= 0 || maxCount <= 0) return 0
    val bucketSize = (maxCount / 4.0).coerceAtLeast(1.0)
    return ceil(count / bucketSize).toInt().coerceIn(1, 4)
}

/** Monday = 0 ... Sunday = 6, for the weekday the 1st of [month] falls on. */
private fun firstWeekdayOffset(month: SelectedMonth): Int {
    val cal = Calendar.getInstance()
    cal.set(month.year, month.month - 1, 1)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 .. SATURDAY=7
    return (dayOfWeek + 5) % 7
}

private fun formatHour(hour: Int): String {
    val normalized = ((hour % 24) + 24) % 24
    val period = if (normalized < 12) "AM" else "PM"
    val displayHour = normalized % 12
    return "${if (displayHour == 0) 12 else displayHour} $period"
}

// ─── Previews ────────────────────────────────────────────────────────────────

private val SAMPLE_PLAYS_PER_DAY = listOf(
    0, 3, 5, 2, 6, 8, 4,
    0, 2, 4, 3, 7, 9, 5,
    1, 4, 6, 14, 7, 3, 2,
    5, 8, 6, 4, 2, 0, 3,
    5, 4
)

@Preview(showBackground = true)
@Composable
private fun ListeningActivityCardCollapsedPreview() {
    MusicTrackerAppTheme {
        Surface {
            ListeningActivityCard(
                selectedMonth = SelectedMonth.current(),
                heatMapActivity = HeatMapActivity(
                    playsPerDay = SAMPLE_PLAYS_PER_DAY,
                    averagePlaysPerDay = 4.8f,
                    startPeakHour = 20,
                    endPeakHour = 22
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListeningActivityCardExpandedPreview() {
    MusicTrackerAppTheme {
        Surface {
            ListeningActivityCard(
                selectedMonth = SelectedMonth.current(),
                heatMapActivity = HeatMapActivity(
                    playsPerDay = SAMPLE_PLAYS_PER_DAY,
                    averagePlaysPerDay = 4.8f,
                    startPeakHour = 20,
                    endPeakHour = 22
                ),
                initiallyExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ListeningActivityCardEmptyPreview() {
    MusicTrackerAppTheme {
        Surface {
            ListeningActivityCard(
                selectedMonth = SelectedMonth.current(),
                heatMapActivity = HeatMapActivity(
                    playsPerDay = listOf(),
                    averagePlaysPerDay = 0f,
                    startPeakHour = 0,
                    endPeakHour = 0
                ),
                initiallyExpanded = true
            )
        }
    }
}
