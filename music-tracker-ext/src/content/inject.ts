// Runs in the page's MAIN world — has reliable access to the real navigator.mediaSession.
// Sends finalized plays to the isolated content script via window.postMessage.

interface ActivePlay {
  title: string;
  artist: string;
  album: string;
  artUri: string;
  durationMs: number;
  startedAt: number;
  totalPausedMs: number;
  pauseStartedAt: number | null;
  lastState: string;
}

const MIN_LISTEN_MS = 15_000;
const SKIP_THRESHOLD = 0.7;

let active: ActivePlay | null = null;

// Patch setPositionState to capture duration directly — no postMessage round-trip needed.
const origSetPositionState = navigator.mediaSession.setPositionState?.bind(navigator.mediaSession);
if (origSetPositionState) {
  navigator.mediaSession.setPositionState = function (state?: MediaPositionState) {
    if (state?.duration && active) {
      const dur = Math.round(state.duration * 1000);
      if (dur > 0) active.durationMs = dur;
    }
    return origSetPositionState(state);
  };
}

function finalizePlay(): void {
  if (!active) return;

  const endedAt = Date.now();
  const ongoingPauseMs = active.pauseStartedAt != null ? endedAt - active.pauseStartedAt : 0;
  const listenedMs = Math.max(
    0,
    endedAt - active.startedAt - active.totalPausedMs - ongoingPauseMs,
  );

  if (listenedMs < MIN_LISTEN_MS) {
    active = null;
    return;
  }

  const skipped = active.durationMs > 0 && listenedMs < SKIP_THRESHOLD * active.durationMs;
  if (skipped) {
    active = null;
    return;
  }

  window.postMessage(
    {
      type: '__YTM_SAVE_PLAY__',
      play: {
        title: active.title,
        artist: active.artist,
        album: active.album,
        artUri: active.artUri,
        durationMs: active.durationMs,
        listenedMs,
        startedAt: active.startedAt,
        endedAt,
        sourcePackage: 'music.youtube.com',
      },
    },
    window.location.origin,
  );

  active = null;
}

function poll(): void {
  console.log('[YTM]', navigator.mediaSession.metadata?.title, navigator.mediaSession.playbackState);
  const meta = navigator.mediaSession.metadata;
  const state = navigator.mediaSession.playbackState;

  if (!meta?.title) {
    if (active) finalizePlay();
    return;
  }

  const title = meta.title;
  const artist = meta.artist ?? '';
  const album = meta.album ?? '';
  const artUri = meta.artwork?.[0]?.src ?? '';
  const isSameSong = active?.title === title && active?.artist === artist;

  if (!isSameSong) {
    if (active) finalizePlay();
    // Start tracking whenever we see new metadata, regardless of reported state.
    // playbackState may lag behind metadata on the first event.
    active = {
      title,
      artist,
      album,
      artUri,
      durationMs: 0,
      startedAt: Date.now(),
      totalPausedMs: 0,
      // If state is already 'paused', start the pause clock immediately.
      pauseStartedAt: state === 'paused' ? Date.now() : null,
      lastState: state || 'none',
    };
    return;
  }

  if (!active) return;
  if (!active.artUri && artUri) active.artUri = artUri;

  if (state === 'paused' && active.lastState !== 'paused') {
    active.pauseStartedAt = Date.now();
    active.lastState = 'paused';
  } else if (state === 'playing' && active.lastState !== 'playing') {
    if (active.pauseStartedAt != null) {
      active.totalPausedMs += Date.now() - active.pauseStartedAt;
      active.pauseStartedAt = null;
    }
    active.lastState = 'playing';
  }
}

window.addEventListener('beforeunload', finalizePlay);
setInterval(poll, 500);
