// ─────────────────────────────────────────────────
//  torch.js — draws the darkness + torch light
//             onto the UI overlay canvas
//
//  Tweak TORCH_RADIUS to make the light bigger/smaller.
//  Tweak FLICKER_STRENGTH for more/less flame wobble.
// ─────────────────────────────────────────────────

const Torch = (() => {

  const TORCH_RADIUS    = 145;  // px — default base radius of light cone
  const FLICKER_STRENGTH = 6;   // px — how much the radius varies

  let flickerT = 0;
  let baseRadius = TORCH_RADIUS; // overridden per-level by the game

  // set the base light radius (see Levels.torchRadius)
  function setRadius(r) { baseRadius = r; }

  // ── draw(ctx, charScreenX, charScreenY, mouseScreenX, mouseScreenY)
  //  charScreen* : character is always at screen centre
  //  mouseSX/SY  : where the cursor/torch tip is
  function draw(ctx, cw, ch, mouseSX, mouseSY) {
    flickerT += 0.055;
    const flick =
      Math.sin(flickerT * 2.1) * FLICKER_STRENGTH * 0.7 +
      Math.sin(flickerT * 5.9) * FLICKER_STRENGTH * 0.3;
    const r = baseRadius + flick;

    // character is pinned to screen centre
    const cx = cw / 2;
    const cy = ch / 2;

    ctx.clearRect(0, 0, cw, ch);

    // 1. Flood fill black
    ctx.fillStyle = '#000';
    ctx.fillRect(0, 0, cw, ch);

    // 2. Cut out the torch cone around the character
    const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
    g.addColorStop(0,    'rgba(0,0,0,1)');
    g.addColorStop(0.38, 'rgba(0,0,0,1)');
    g.addColorStop(0.65, 'rgba(0,0,0,0.72)');
    g.addColorStop(0.85, 'rgba(0,0,0,0.3)');
    g.addColorStop(1,    'rgba(0,0,0,0)');
    ctx.globalCompositeOperation = 'destination-out';
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalCompositeOperation = 'source-over';

    // 3. Warm amber tint inside torch
    const wg = ctx.createRadialGradient(cx, cy, 0, cx, cy, r * 0.5);
    wg.addColorStop(0, 'rgba(255,170,50,0.07)');
    wg.addColorStop(1, 'transparent');
    ctx.fillStyle = wg;
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.5, 0, Math.PI * 2);
    ctx.fill();

    // 4. Mouse cursor glow dot
    ctx.beginPath();
    ctx.arc(mouseSX, mouseSY, 5, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255,220,120,0.85)';
    ctx.fill();

    // crosshair ring
    ctx.strokeStyle = 'rgba(255,220,120,0.35)';
    ctx.lineWidth   = 1;
    ctx.beginPath();
    ctx.arc(mouseSX, mouseSY, 14, 0, Math.PI * 2);
    ctx.stroke();
  }

  return { draw, setRadius };
})();
