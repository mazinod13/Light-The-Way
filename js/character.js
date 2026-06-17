// ─────────────────────────────────────────────────────────────────
//  character.js — draws the player character onto the game canvas
//
//  ╔══════════════════════════════════════════════════════════════╗
//  ║              CHARACTER SPRITE — LLAMA WALK CYCLE             ║
//  ╠══════════════════════════════════════════════════════════════╣
//  ║                                                              ║
//  ║  The character is a PNG sprite sheet exported from Piskel.   ║
//  ║                                                              ║
//  ║  Source : assets/sprites/llama-20260616-155433.piskel       ║
//  ║  Sheet  : assets/sprites/llama.png  (generated from it)     ║
//  ║                                                              ║
//  ║  The sheet is HORIZONTAL — 6 frames laid left-to-right,     ║
//  ║  each frame 48×48 px → total image is 288×48 px.            ║
//  ║                                                              ║
//  ║      ┌────┬────┬────┬────┬────┬────┐                        ║
//  ║      │ 0  │ 1  │ 2  │ 3  │ 4  │ 5  │   frame i @ x = i·48   ║
//  ║      └────┴────┴────┴────┴────┴────┘                        ║
//  ║                                                              ║
//  ║  TO SWAP IN A DIFFERENT SPRITE                              ║
//  ║  ─────────────────────────────                              ║
//  ║  1. Draw / edit in Piskel (piskelapp.com).                  ║
//  ║  2. Export → PNG → "single PNG" (sprite sheet) into         ║
//  ║     assets/sprites/ and point `sheet.src` at it.            ║
//  ║  3. Update FRAME_SIZE / FRAME_COUNT to match your sheet.    ║
//  ║                                                              ║
//  ║  (Re-exporting a .piskel? Regenerate the PNG with:          ║
//  ║   read the file's JSON → layers[0].chunks[0].base64PNG →    ║
//  ║   strip the data: prefix → base64-decode → write .png)      ║
//  ╚══════════════════════════════════════════════════════════════╝
// ─────────────────────────────────────────────────────────────────

const Character = (() => {

  // ── SPRITE SHEET LAYOUT ───────────────────────────────────────
  const FRAME_SIZE      = 48;  // each frame is 48×48 px in the sheet
  const FRAME_COUNT     = 6;   // number of walk frames on the sheet
  const DISPLAY_SIZE    = 48;  // px drawn on screen (1× keeps it crisp)
  const TICKS_PER_FRAME = 5;   // walkTicks before advancing one frame
                               // (~12 fps at 60fps loop ≈ the .piskel's 13 fps)

  // ── LOAD THE SHEET ────────────────────────────────────────────
  // Loaded async; draw() simply skips until it's ready.
  const sheet = new Image();
  let sheetReady = false;
  sheet.onload  = () => { sheetReady = true; };
  sheet.src = 'assets/sprites/llama.png';

  // ── PUBLIC draw() ─────────────────────────────────────────────
  //  ctx      : game canvas 2d context
  //  x, y     : screen-space centre position
  //  walkTick : ever-incrementing integer, controls frame cycling
  //  isMoving : bool — freeze on frame 0 when standing still
  function draw(ctx, x, y, walkTick, isMoving) {
    if (!sheetReady) return; // sheet still decoding — nothing to draw yet

    const frameIndex = isMoving
      ? Math.floor(walkTick / TICKS_PER_FRAME) % FRAME_COUNT
      : 0;

    const sx = frameIndex * FRAME_SIZE;     // source-x of this frame
    const d  = DISPLAY_SIZE;

    // draw pixel-perfect (disable smoothing so pixels stay crisp)
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(
      sheet,
      sx, 0, FRAME_SIZE, FRAME_SIZE,             // source frame rect
      Math.round(x - d / 2), Math.round(y - d / 2), // dest centred on (x,y)
      d, d
    );
  }

  return { draw };
})();
