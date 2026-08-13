# CLAUDE.md — YT Music Tracker

## Monorepo layout
```
yt-music-tracker/
├── yt-music-tracker-app/   # Phase 1: Android app (current)
└── (phases 2 & 3 TBD)
```

## Phase 1 — Android app

### What it does
Tracks songs played in YouTube Music using `NotificationListenerService` +
`MediaSessionManager`. Stores play history in a local Room/SQLite database and
shows top-artists / top-songs stats by month.

### Tech stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3, MVI pattern
- **DB**: Room 2.7.1 with KSP, SQLite
- **Async**: Kotlin Coroutines + Flow
- **Image loading**: Coil 3.2.0 (`coil-compose` + `coil-network-okhttp`)
- **Navigation**: Navigation Compose 2.9.0
- **compileSdk / targetSdk**: 37 — required by `core-ktx 1.19.0` and `lifecycle 2.11.0`
- **minSdk**: 24

### gradle.properties quirk
```
android.disallowKotlinSourceSets=false
```
Required for KSP + AGP 9.x compatibility (see AndroidX issue #2729).

---

## Architecture

### Clean architecture layers
```
domain/         Pure Kotlin. No Android deps.
  model/        Play, ArtistStat, SongStat
  repository/   PlayRepository interface

data/           Android/Room implementations.
  local/
    entity/     PlayEntity
    dao/        PlayDao
    db/         AppDatabase
  repository/   PlayRepositoryImpl

service/        MusicTrackerService (NotificationListenerService)
ui/             Compose screens, ViewModels, MVI
  onboarding/
  stats/
  navigation/
  theme/
```

### MVI conventions
- `UiState` sealed class (`Loading`, `Success`)
- `Intent` sealed class for user actions
- `AndroidViewModel` with `StateFlow<UiState>`
- Stateless inner composables, stateful screen composables
- `collectAsStateWithLifecycle()` in screens

---

## MusicTrackerService

### How playback is captured
- `NotificationListenerService` gives access to `MediaSessionManager`
- One `MediaController` is registered per supported app (currently only YT Music)
- `MediaController.Callback` fires `onMetadataChanged` / `onPlaybackStateChanged` / `onSessionDestroyed`

### Skip detection
Position-based sampling was unreliable — YT Music moves `PlaybackState.position`
to near end-of-song right before firing `onMetadataChanged` for the next track.

**Solution**: wall-clock elapsed time minus accumulated pause time.
```
listenedMs = (now - startedAt - totalPausedMs - ongoingPauseMs).coerceAtLeast(0)
skipped = durationMs > 0 && listenedMs < 0.7 * durationMs
```

### Duplicate metadata guard
YT Music fires `onMetadataChanged` multiple times per track.
Guard: if `title + artist` matches the current `ActivePlay`, the event is ignored.

### Album art resolution (`resolveArtUri`)
Priority chain:
1. `METADATA_KEY_ART_URI` (String CDN URL, e.g. `lh3.googleusercontent.com`)
2. `METADATA_KEY_ALBUM_ART` or `METADATA_KEY_ART` bitmap → saved as JPEG to
   `filesDir/art/<hash>.jpg` → stored as `file://` URI
3. Empty string fallback

Coil handles both `https://` and `file://` URIs transparently.

### Supported packages
```kotlin
const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
```
To add Spotify (`com.spotify.music`), the single-controller approach in
`updateMediaController` must be expanded to support multiple concurrent controllers,
each with its own `ActivePlay` and pause tracking state. Note: Spotify uses
`spotify:image:...` URIs which Coil cannot load without a custom fetcher — the
bitmap fallback handles it.

---

## Database

### Room schema (version 2)
Table: `plays`

| Column | Type |
|---|---|
| id | INTEGER PK autoGenerate |
| title | TEXT |
| artist | TEXT |
| album | TEXT |
| artUri | TEXT (default '') |
| durationMs | INTEGER |
| listenedMs | INTEGER |
| startedAt | INTEGER (epoch ms) |
| endedAt | INTEGER (epoch ms) |
| skipped | INTEGER (boolean) |
| sourcePackage | TEXT |

### Migration history
- **1 → 2**: `ALTER TABLE plays ADD COLUMN artUri TEXT NOT NULL DEFAULT ''`

### DAO queries
Both `topArtists` and `topSongs` accept a `:limit` parameter (default 20, set
via `StatsViewModel.DEFAULT_TOP_LIMIT`). This is the hook for a future settings
screen. `topSongs` uses `MAX(artUri)` in the GROUP BY to surface a non-empty
art URI for each song group.

---

## UI / Stats screen

### TopAppBar behavior
`MediumTopAppBar` with `exitUntilCollapsedScrollBehavior`. A `LaunchedEffect`
keyed on `selectedMonth` resets `scrollBehavior.state.heightOffset = 0f` on
every month change so that a month with no items doesn't inherit the collapsed
state from a scrolled month.

### Rank format
Zero-padded two digits: `"%02d".format(rank)` → `01`, `09`, `15`.

---

## Design system

### Fonts (`res/font/`)
- **Barlow** (Regular, Medium, SemiBold, Bold) — title, body, label styles
- **Barlow Condensed** (Regular, Medium, SemiBold, Bold) — display, headline styles

Fonts are bundled as TTF files (not downloadable fonts) to avoid the Google Fonts
certificate file requirement.

### Color scheme
Custom light/dark — dynamic color is disabled.

| Role | Light | Dark |
|---|---|---|
| Background / Surface | `#f2f2f3` | `#1d1f20` |
| Text (onBackground / onSurface) | `#1d1f20` | `#f2f2f3` |
| Accent / Primary (rank, label, icons) | `#416180` | `#94bce3` |
| Divider (outlineVariant) | `rgba(29,31,32,0.16)` | `rgba(242,242,243,0.16)` |

`HorizontalDivider` picks up `outlineVariant` automatically.
Accent elements use `MaterialTheme.colorScheme.primary` (not hardcoded) so they
adapt to dark/light automatically.

---

## Git history (summary)
| Commit | Description |
|---|---|
| `bf5591f` | Initial project scaffold |
| `851c068` | Phase 1 deps + clean architecture data/domain layers |
| `8f148fa` | MusicTrackerService with playback capture and duplicate guard |
| `1d21153` | MVI UI layer + skip detection fix (wall-clock) |
| `d0553bb` | Fix .gitignore: ignore root .idea/ |
| `bf8f60d` | Album art support (Coil, artUri, bitmap fallback, stats thumbnail) |
| `7c2d5ec` | Design system: Barlow fonts, custom color scheme, rank format, top-20 limit, TopAppBar collapse fix |
