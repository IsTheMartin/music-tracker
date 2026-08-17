import { useState, useEffect } from 'react';
import type { ArtistStat, SongStat, StatsResult } from '../shared/types';

type Tab = 'artists' | 'songs';

function currentMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function monthLabel(month: string): string {
  const [y, m] = month.split('-');
  return new Date(Number(y), Number(m) - 1).toLocaleString('default', {
    month: 'long',
    year: 'numeric',
  });
}

function prevMonth(month: string): string {
  const [y, m] = month.split('-').map(Number);
  return m === 1
    ? `${y - 1}-12`
    : `${y}-${String(m - 1).padStart(2, '0')}`;
}

function nextMonth(month: string): string {
  const [y, m] = month.split('-').map(Number);
  return m === 12
    ? `${y + 1}-01`
    : `${y}-${String(m + 1).padStart(2, '0')}`;
}

function Thumb({ src }: { src: string }) {
  return src ? (
    <img className="thumb" src={src} alt="" />
  ) : (
    <div className="thumb thumb--placeholder" />
  );
}

export default function App() {
  const [month, setMonth] = useState(currentMonth);
  const [tab, setTab] = useState<Tab>('artists');
  const [artists, setArtists] = useState<ArtistStat[]>([]);
  const [songs, setSongs] = useState<SongStat[]>([]);
  const [totalPlays, setTotalPlays] = useState(0);
  const [loading, setLoading] = useState(true);

  const isCurrentMonth = month === currentMonth();

  useEffect(() => {
    setLoading(true);
    chrome.runtime.sendMessage({ type: 'GET_STATS', month }, (result: StatsResult | undefined) => {
      if (result) {
        setArtists(result.artists);
        setSongs(result.songs);
        setTotalPlays(result.totalPlays);
      }
      setLoading(false);
    });
  }, [month]);

  const items = tab === 'artists' ? artists : songs;
  const empty = !loading && items.length === 0;

  return (
    <div className="app">
      <header className="header">
        <div className="header-label">YT Music Tracker</div>
        <div className="month-nav">
          <button className="nav-btn" onClick={() => setMonth(prevMonth)}>‹</button>
          <span className="month-label">{monthLabel(month)}</span>
          <button
            className="nav-btn"
            onClick={() => setMonth(nextMonth)}
            disabled={isCurrentMonth}
          >›</button>
        </div>
      </header>

      <div className="tab-bar">
        <button
          className={`tab${tab === 'artists' ? ' tab--active' : ''}`}
          onClick={() => setTab('artists')}
        >Artists</button>
        <button
          className={`tab${tab === 'songs' ? ' tab--active' : ''}`}
          onClick={() => setTab('songs')}
        >Songs</button>
      </div>

      <div className="list">
        {loading && <div className="empty">Loading…</div>}

        {empty && (
          <div className="empty">
            {isCurrentMonth ? 'No plays yet — open YouTube Music to start tracking' : 'No plays this month'}
          </div>
        )}

        {!loading && tab === 'artists' && artists.map((item, i) => (
          <div key={item.artist} className="row">
            <span className="rank">{String(i + 1).padStart(2, '0')}</span>
            <Thumb src={item.artUri} />
            <div className="item-info">
              <span className="item-title">{item.artist}</span>
            </div>
            <span className="count">{item.playCount}</span>
          </div>
        ))}

        {!loading && tab === 'songs' && songs.map((item, i) => (
          <div key={`${item.title}\0${item.artist}`} className="row">
            <span className="rank">{String(i + 1).padStart(2, '0')}</span>
            <Thumb src={item.artUri} />
            <div className="item-info">
              <span className="item-title">{item.title}</span>
              <span className="item-sub">{item.artist}</span>
            </div>
            <span className="count">{item.playCount}</span>
          </div>
        ))}
      </div>

      {!loading && totalPlays > 0 && (
        <div className="footer">
          {totalPlays} play{totalPlays !== 1 ? 's' : ''} this month
        </div>
      )}
    </div>
  );
}
