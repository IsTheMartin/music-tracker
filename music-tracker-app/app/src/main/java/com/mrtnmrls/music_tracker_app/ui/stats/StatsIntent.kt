package com.mrtnmrls.music_tracker_app.ui.stats

import android.net.Uri

sealed interface StatsIntent {
    data object PreviousMonth : StatsIntent
    data object NextMonth : StatsIntent
}
