import type { Play, ArtistStat, SongStat, StatsResult } from '../shared/types';

async function getPlays(): Promise<Play[]> {
  const data = (await chrome.storage.local.get('plays')) as { plays?: Play[] };
  return data.plays ?? [];
}

async function handleSavePlay(play: Omit<Play, 'id'>): Promise<void> {
  const plays = await getPlays();
  plays.push({ ...play, id: Date.now() });
  await chrome.storage.local.set({ plays });
}

function toMonthStr(ts: number): string {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

async function handleGetStats(month: string): Promise<StatsResult> {
  const plays = await getPlays();
  const filtered = plays.filter((p) => toMonthStr(p.startedAt) === month && !p.skipped);

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