export interface Play {
  id: number;
  title: string;
  artist: string;
  album: string;
  artUri: string;
  durationMs: number;
  listenedMs: number;
  startedAt: number;
  endedAt: number;
  sourcePackage: string;
}

export interface ArtistStat {
  artist: string;
  playCount: number;
  artUri: string;
}

export interface SongStat {
  title: string;
  artist: string;
  playCount: number;
  artUri: string;
}

export interface StatsResult {
  artists: ArtistStat[];
  songs: SongStat[];
  totalPlays: number;
}