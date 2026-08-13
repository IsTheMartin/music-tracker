package com.mrtnmrls.music_tracker_app.ui.stats

import android.net.Uri

sealed interface StatsIntent {
    object PreviousMonth : StatsIntent
    object NextMonth : StatsIntent
    data class ExportToUri(val uri: Uri) : StatsIntent
    data class ImportFromUri(val uri: Uri) : StatsIntent
}
