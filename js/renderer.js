// ─────────────────────────────────────────────────
//  renderer.js — draws the maze tiles + exit marker
//
//  Change TILE to resize every cell in the maze.
//  Change wall/floor colours here.
// ─────────────────────────────────────────────────

const Renderer = (() => {

  const TILE = 32; // px per maze cell

  // ── drawMaze ─────────────────────────────────
  function drawMaze(ctx, maze, rows, cols, ox, oy, exitCell) {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const x = ox + c * TILE;
        const y = oy + r * TILE;
        if (maze[r][c] === 1) {
          // wall
          ctx.fillStyle = '#17110a';
          ctx.fillRect(x, y, TILE, TILE);
          // subtle top/left bevel
          ctx.fillStyle = 'rgba(255,255,255,0.025)';
          ctx.fillRect(x, y, TILE, 1);
          ctx.fillRect(x, y, 1, TILE);
        } else {
          // floor
          ctx.fillStyle = '#0c0c0c';
          ctx.fillRect(x, y, TILE, TILE);
        }
      }
    }

    // exit tile pulse
    const ex = ox + exitCell.c * TILE;
    const ey = oy + exitCell.r * TILE;
    const pulse = 0.5 + 0.5 * Math.sin(Date.now() / 380);

    ctx.fillStyle = `rgba(0,255,190,${0.07 + 0.07 * pulse})`;
    ctx.fillRect(ex, ey, TILE, TILE);

    ctx.font          = `${TILE - 8}px monospace`;
    ctx.textAlign     = 'center';
    ctx.textBaseline  = 'middle';
    ctx.fillStyle     = `rgba(0,255,190,${0.5 + 0.4 * pulse})`;
    ctx.fillText('◈', ex + TILE / 2, ey + TILE / 2);
  }

  // ── helpers ──────────────────────────────────
  function cellToWorld(r, c) {
    return { x: c * TILE + TILE / 2, y: r * TILE + TILE / 2 };
  }

  function worldToCell(wx, wy) {
    return { r: Math.round((wy - TILE/2) / TILE), c: Math.round((wx - TILE/2) / TILE) };
  }

  function screenToCell(sx, sy, charX, charY, cw, ch) {
    const ox = cw/2 - charX;
    const oy = ch/2 - charY;
    return {
      r: Math.floor((sy - oy) / TILE),
      c: Math.floor((sx - ox) / TILE),
    };
  }

  function getOffset(charX, charY, cw, ch) {
    return { ox: cw/2 - charX, oy: ch/2 - charY };
  }

  return { TILE, drawMaze, cellToWorld, worldToCell, screenToCell, getOffset };
})();
