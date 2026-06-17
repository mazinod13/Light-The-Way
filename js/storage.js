// ─────────────────────────────────────────────────
//  storage.js — progress + settings persistence
//
//  Everything the player earns or configures lives in
//  one localStorage blob. Safe to call anywhere; if
//  storage is unavailable it silently keeps state in
//  memory for the session.
// ─────────────────────────────────────────────────

const Save = (() => {

  const KEY = 'ltw.save.v1';

  const defaults = () => ({
    best:     {},   // { levelIndex: fewestSteps }
    stars:    {},   // { levelIndex: bestStarCount 0-3 }
    unlocked: 1,    // number of unlocked levels (index < unlocked = playable)
    settings: { sound: true, music: true, difficulty: 'normal' },
  });

  let data = read();

  function read() {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return defaults();
      const p = JSON.parse(raw);
      const d = defaults();
      return {
        best:     p.best     || d.best,
        stars:    p.stars    || d.stars,
        unlocked: p.unlocked || d.unlocked,
        settings: Object.assign(d.settings, p.settings || {}),
      };
    } catch { return defaults(); }
  }

  function persist() {
    try { localStorage.setItem(KEY, JSON.stringify(data)); } catch { /* ignore */ }
  }

  return {
    isUnlocked: i => i < data.unlocked,
    get unlocked() { return data.unlocked; },
    best:  i => (i in data.best  ? data.best[i]  : null),
    stars: i => (i in data.stars ? data.stars[i] : 0),
    totalStars: () => Object.values(data.stars).reduce((a, b) => a + b, 0),

    settings:   () => data.settings,
    setSetting(k, v) { data.settings[k] = v; persist(); },

    // record a clear: keeps the best (fewest) steps + best stars, unlocks next
    recordWin(i, steps, stars) {
      if (data.best[i] == null || steps < data.best[i]) data.best[i] = steps;
      if (stars > (data.stars[i] || 0)) data.stars[i] = stars;
      if (i + 2 > data.unlocked) data.unlocked = i + 2;
      persist();
    },

    reset() { data = defaults(); persist(); },
  };
})();
