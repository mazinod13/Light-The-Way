// ─────────────────────────────────────────────────
//  levels.js — the campaign + endless procedural run
//
//  Levels 0..CURATED-1  → hand-built mazes (mazes.js)
//  Levels CURATED..TOTAL → seeded procedural mazes that
//                          grow as you go deeper.
//
//  Difficulty (from settings) and depth both shrink the
//  torch radius, so later levels are genuinely harder.
// ─────────────────────────────────────────────────

const Levels = (() => {

  const CURATED = MAZES.length;   // hand-built campaign length
  const TOTAL   = 12;             // levels shown in the picker

  const DIFFICULTY = {
    easy:   { torch: 1.30, label: 'Easy'   },
    normal: { torch: 1.00, label: 'Normal' },
    hard:   { torch: 0.72, label: 'Hard'   },
  };

  // ── get the maze definition for a level index ──────
  function get(i) {
    if (i < CURATED) {
      const d = MAZES[i];
      return {
        grid:  d.grid,
        start: { r: d.start[0], c: d.start[1] },
        exit:  { r: d.exit[0],  c: d.exit[1] },
      };
    }
    // procedural — grow the maze gently with depth
    const step = i - CURATED;
    const cols = Math.min(9 + step,  21);
    const rows = Math.min(7 + step,  15);
    const g = MazeGen.generate(0x9E37 + i * 2654435761, cols, rows);
    return {
      grid:  g.grid,
      start: { r: g.start[0], c: g.start[1] },
      exit:  { r: g.exit[0],  c: g.exit[1] },
    };
  }

  // ── torch radius for a level (depth + difficulty) ──
  function torchRadius(i, difficulty) {
    const base = CONFIG.TORCH_RADIUS - i * CONFIG.TORCH_SHRINK_PER_LEVEL;
    const mult = (DIFFICULTY[difficulty] || DIFFICULTY.normal).torch;
    return Math.max(CONFIG.TORCH_RADIUS_MIN, Math.round(base * mult));
  }

  const name    = i => (i < CURATED ? `Depth ${i + 1}` : `Abyss ${i - CURATED + 1}`);
  const isEndless = i => i >= CURATED;

  return { TOTAL, CURATED, DIFFICULTY, get, torchRadius, name, isEndless };
})();
