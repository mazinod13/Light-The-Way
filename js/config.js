// ─────────────────────────────────────────────
//  config.js  —  tweak all game constants here
// ─────────────────────────────────────────────

const CONFIG = {
  TILE: 32,           // px size of one maze cell

  // Character movement speed in px per frame
  // Lower = slower wanderer, higher = snappier
  CHAR_SPEED: 1.8,

  // Torch light radius in px
  // Decrease per level to make it harder
  TORCH_RADIUS: 145,
  TORCH_RADIUS_MIN: 90,   // floor (never goes below this)
  TORCH_SHRINK_PER_LEVEL: 10,

  // Torch flicker intensity (0 = no flicker)
  FLICKER_AMOUNT: 6,
};
