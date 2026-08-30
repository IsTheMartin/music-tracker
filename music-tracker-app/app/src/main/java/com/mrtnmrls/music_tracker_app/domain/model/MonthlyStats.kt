package com.mrtnmrls.music_tracker_app.domain.model

data class MonthlyStats(
    val topArtists: List<ArtistStat>,
    val topSongs: List<SongStat>,
    val totalPlays: Int
)
