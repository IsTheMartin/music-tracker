import type { Play, ArtistStat, SongStat, StatsResult } from '../shared/types';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL as string;
const SUPABASE_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY as string;

type SupabasePlay = {
  title: string;
  artist: string;
  album: string;
  art_uri: string;
  duration_ms: number;
  listened_ms: number;
  started_at: number;
  ended_at: number;
  source_package: string;
};

async function getDeviceId(): Promise<string> {
  const data = (await chrome.storage.local.get('device_id')) as { device_id?: string };
  if (data.device_id) return data.device_id;
  const id = crypto.randomUUID();
  await chrome.storage.local.set({ device_id: id });
  return id;
}

async function getPlays(): Promise<Play[]> {
  const data = (await chrome.storage.local.get('plays')) as { plays?: Play[] };
  return data.plays ?? [];
}

async function syncFromSupabase(): Promise<void> {
  const cols = 'title,artist,album,art_uri,duration_ms,listened_ms,started_at,ended_at,source_package';
  const res = await fetch(`${SUPABASE_URL}/rest/v1/plays?select=${cols}`, {
    headers: {
      'apikey': SUPABASE_KEY,
      'Authorization': `Bearer ${SUPABASE_KEY}`,
    },
  });
  if (!res.ok) return;

  const remotePlays = (await res.json()) as SupabasePlay[];
  const localPlays = await getPlays();
  const localStartedAts = new Set(localPlays.map((p) => p.startedAt));

  const newPlays: Play[] = remotePlays
    .filter((r) => !localStartedAts.has(r.started_at))
    .map((r) => ({
      id: r.started_at,
      title: r.title,
      artist: r.artist,
      album: r.album,
      artUri: r.art_uri,
      durationMs: r.duration_ms,
      listenedMs: r.listened_ms,
      startedAt: r.started_at,
      endedAt: r.ended_at,
      sourcePackage: r.source_package,
    }));

  if (newPlays.length > 0) {
    const merged = [...localPlays, ...newPlays].sort((a, b) => a.startedAt - b.startedAt);
    await chrome.storage.local.set({ plays: merged });
  }
}

async function handleSavePlay(play: Omit<Play, 'id'>): Promise<void> {
  const plays = await getPlays();
  const newPlay: Play = { ...play, id: Date.now() };
  plays.push(newPlay);
  await chrome.storage.local.set({ plays });

  const deviceId = await getDeviceId();
  fetch(`${SUPABASE_URL}/rest/v1/plays`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': SUPABASE_KEY,
      'Authorization': `Bearer ${SUPABASE_KEY}`,
      'Prefer': 'return=minimal',
    },
    body: JSON.stringify({
      device_id: deviceId,
      title: newPlay.title,
      artist: newPlay.artist,
      album: newPlay.album,
      art_uri: newPlay.artUri,
      duration_ms: newPlay.durationMs,
      listened_ms: newPlay.listenedMs,
      started_at: newPlay.startedAt,
      ended_at: newPlay.endedAt,
      source_package: newPlay.sourcePackage,
    }),
  }).then((res) => {
    if (!res.ok) res.text().then((t) => console.warn('[YTM] Supabase sync failed:', t));
  }).catch((err) => console.warn('[YTM] Supabase sync error:', err));
}

function toMonthStr(ts: number): string {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

async function handleGetStats(month: string): Promise<StatsResult> {
  await syncFromSupabase().catch(() => {});

  const plays = await getPlays();
  const filtered = plays.filter((p) => toMonthStr(p.startedAt) === month);

  const artistMap = new Map<string, { count: number; artUri: string }>();
  const songMap = new Map<string, { count: number; artUri: string; artist: string }>();

  for (const p of filtered) {
    const a = artistMap.get(p.artist);
    if (a) {
      a.count++;
      if (!a.artUri && p.artUri) a.artUri = p.artUri;
    } else {
      artistMap.set(p.artist, { count: 1, artUri: p.artUri });
    }

    const key = `${p.title}\0${p.artist}`;
    const s = songMap.get(key);
    if (s) {
      s.count++;
      if (!s.artUri && p.artUri) s.artUri = p.artUri;
    } else {
      songMap.set(key, { count: 1, artUri: p.artUri, artist: p.artist });
    }
  }

  const artists: ArtistStat[] = [...artistMap.entries()]
    .map(([artist, { count, artUri }]) => ({ artist, playCount: count, artUri }))
    .sort((a, b) => b.playCount - a.playCount)
    .slice(0, 20);

  const songs: SongStat[] = [...songMap.entries()]
    .map(([key, { count, artUri, artist }]) => ({
      title: key.split('\0')[0],
      artist,
      playCount: count,
      artUri,
    }))
    .sort((a, b) => b.playCount - a.playCount)
    .slice(0, 20);

  return { artists, songs, totalPlays: filtered.length };
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg.type === 'SAVE_PLAY') {
    handleSavePlay(msg.play as Omit<Play, 'id'>).then(() => sendResponse({ ok: true }));
    return true;
  }
  if (msg.type === 'GET_STATS') {
    handleGetStats(msg.month as string).then((result) => sendResponse(result));
    return true;
  }
});
