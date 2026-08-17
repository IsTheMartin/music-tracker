// Isolated world — can use chrome.* APIs but cannot access the page's mediaSession.
// Acts as a relay: receives finalized play data from inject.ts (MAIN world) and
// forwards it to the background service worker.

import type { Play } from '../shared/types';

window.addEventListener('message', (event) => {
  if (event.source !== window) return;
  if (event.data?.type !== '__YTM_SAVE_PLAY__') return;
  chrome.runtime.sendMessage({ type: 'SAVE_PLAY', play: event.data.play as Omit<Play, 'id'> });
});
