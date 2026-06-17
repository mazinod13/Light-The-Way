// ─────────────────────────────────────────────────
//  maze-gen.js — seeded procedural maze generator
//
//  Recursive-backtracker → a "perfect" maze (exactly
//  one path between any two cells), so every generated
//  level is guaranteed solvable.
//
//    MazeGen.generate(seed, cols, rows)
//      cols/rows = number of rooms (grid is rooms*2+1)
//      → { grid:[[0|1]], start:[r,c], exit:[r,c] }
// ─────────────────────────────────────────────────

const MazeGen = (() => {

  // mulberry32 — tiny deterministic PRNG seeded per level
  function makeRand(seed) {
    let s = seed >>> 0;
    return () => {
      s = (s + 0x6D2B79F5) | 0;
      let t = Math.imul(s ^ (s >>> 15), 1 | s);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function generate(seed, cols, rows) {
    const rand = makeRand(seed);
    const R = rows * 2 + 1, C = cols * 2 + 1;
    const grid = Array.from({ length: R }, () => new Array(C).fill(1));
    const seen = Array.from({ length: rows }, () => new Array(cols).fill(false));

    // iterative DFS (avoids deep recursion on big mazes)
    const stack = [[0, 0]];
    seen[0][0] = true;
    grid[1][1] = 0;

    while (stack.length) {
      const [cx, cy] = stack[stack.length - 1];
      const dirs = [[0, -1], [0, 1], [-1, 0], [1, 0]];
      for (let i = dirs.length - 1; i > 0; i--) {
        const j = Math.floor(rand() * (i + 1));
        [dirs[i], dirs[j]] = [dirs[j], dirs[i]];
      }
      let advanced = false;
      for (const [dx, dy] of dirs) {
        const nx = cx + dx, ny = cy + dy;
        if (nx < 0 || ny < 0 || nx >= cols || ny >= rows || seen[ny][nx]) continue;
        seen[ny][nx] = true;
        grid[ny * 2 + 1][nx * 2 + 1] = 0;          // carve room
        grid[cy * 2 + 1 + dy][cx * 2 + 1 + dx] = 0; // carve wall between
        stack.push([nx, ny]);
        advanced = true;
        break;
      }
      if (!advanced) stack.pop();
    }

    return { grid, start: [1, 1], exit: [R - 2, C - 2] };
  }

  return { generate };
})();
