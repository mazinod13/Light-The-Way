# Light the Way

> Move your mouse — your torch leads the way. The wanderer follows the light through the dark.

A minimalist, atmospheric maze game built with **vanilla JavaScript + HTML5 Canvas** — no frameworks, no build step, no asset pipeline. You hold a flickering torch in a pitch‑black labyrinth; a little wanderer (a llama) follows the light you cast and you must guide it to the glowing exit ◈.

---

## Features

### Atmospheric main menu
- **Starts completely dark** — only a pulsing `PRESS T FOR TORCH` prompt is visible.
- **Idle teaser sweep** — after ~4.5s of no mouse movement, a beam sweeps the title left → right, briefly revealing it, then darkness returns. Repeats while idle.
- **Press `T`** to light your torch: a warm flame follows your cursor and reveals the menu content where it falls. Once lit, the prompt disappears and the menu options (Enter the Dark / Level Select / Settings) appear.

### Gameplay
- **Torch‑lit exploration** — the world is black except for the radius around your torch. The torch flickers like real flame.
- **Follow‑the‑light movement** — point anywhere reachable and the wanderer pathfinds there (BFS shortest path) and walks, animated frame‑by‑frame.
- **Find the exit ◈** to clear the level.

### Levels
- **Curated campaign** — hand‑built mazes first.
- **Endless procedural depths** — after the campaign, a seeded maze generator produces an effectively endless run of mazes that **grow larger with depth**. Every generated maze is a "perfect" maze (exactly one path between any two cells), so it's **always solvable**.
- **12 levels** shown in the picker out of the box (easy to raise — see *Customizing*).

### Progression & scoring
- **Star ratings** per level based on how close to optimal your step count is:
  - ★★★ — within ~115% of the shortest path
  - ★★☆ — within ~160%
  - ★☆☆ — cleared it
- **Best (fewest) steps** saved per level.
- **Unlocks** — clearing a level unlocks the next; locked levels are greyed out.
- **"NEW BEST"** celebration on the win screen when you beat your record.
- Everything **persists** in `localStorage` (survives refresh).

### Menus & flow
- **Level Select** — a grid of cards showing level number, name (`Depth N` / `Abyss N`), and earned stars, plus a running `★ x / max` total.
- **Pause menu** (`Esc`) — Resume / Restart Level / Level Select / Main Menu.
- **Win screen** — stars, steps, best, and Go Deeper / Replay / Level Select.
- **Settings** — toggle Sound effects, toggle Ambient music, pick Difficulty, and Reset all progress.

### Sound — 100% procedural (no audio files)
All audio is synthesised live with the **Web Audio API**:
- footsteps, UI clicks, a win arpeggio, an error/deny buzz
- a slow two‑note **ambient drone** during play
- HUD mute button and independent Sound / Music toggles in Settings
- audio is unlocked on the first click/keypress (browser autoplay‑policy safe)

### Difficulty
Selectable in Settings — it scales the torch radius (smaller torch = harder):

| Difficulty | Torch multiplier |
|-----------|------------------|
| Easy      | 1.30×            |
| Normal    | 1.00×            |
| Hard      | 0.72×            |

The torch **also shrinks with depth** (−10px per level, floored at 90px), so deeper levels are genuinely tougher.

---

## Controls

| Input | Action |
|-------|--------|
| **Mouse move** | Aim your torch / guide the wanderer |
| **`T`** | Toggle the torch on the main menu |
| **`Esc`** | Pause / resume |
| **`R`** | Restart the current level |
| HUD buttons | Pause / Restart / Mute |

---

## Running it

It's a fully static site — no dependencies, no build.

**Easiest:** open `index.html` directly in a modern browser.

**Recommended (avoids any `file://` quirks):** serve the folder locally, e.g.

```bash
# Python 3
python -m http.server 8000

# or Node
npx serve .
```

Then visit `http://localhost:8000`.

> **Browser support:** needs a current Chromium / Firefox / Safari. The main‑menu reveal uses CSS `mask` and `@property`; on a very old browser the menu simply shows fully lit (graceful degradation). Respects `prefers-reduced-motion`.

---

## Project structure

```
light-the-way/
├── index.html              # markup: canvases, HUD, all screens, script order
├── style.css               # all styling + the main-menu torch/mask effects
├── README.md
├── assets/
│   └── sprites/
│       ├── llama.png        # 6-frame horizontal walk sheet (48×48 each)
│       └── llama-*.piskel   # editable Piskel source
└── js/
    ├── config.js            # CONFIG — all tunable constants
    ├── mazes.js             # MAZES — hand-built campaign levels
    ├── maze-gen.js          # MazeGen — seeded procedural maze generator
    ├── levels.js            # Levels — campaign + endless + difficulty scaling
    ├── pathfinder.js        # Pathfinder.bfs — shortest path through a maze
    ├── character.js         # Character — sprite-sheet walk animation
    ├── torch.js             # Torch — darkness overlay + flickering light
    ├── renderer.js          # Renderer — draws maze tiles, exit, coord helpers
    ├── storage.js           # Save — localStorage progress + settings
    ├── audio.js             # Sound — procedural Web Audio SFX + ambient drone
    ├── game.js              # main loop, state machine, screens, input, scoring
    └── menu.js              # main-menu torch reveal + idle sweep
```

### Module responsibilities

| Module | Exposes | Role |
|--------|---------|------|
| `config.js` | `CONFIG` | Tile size, character speed, torch radius/shrink/flicker |
| `mazes.js` | `MAZES` | Array of `{ grid, start, exit }` hand-built levels |
| `maze-gen.js` | `MazeGen.generate(seed, cols, rows)` | Deterministic perfect-maze generator (recursive backtracker, iterative) |
| `levels.js` | `Levels` | `get(i)`, `torchRadius(i, diff)`, `name(i)`, `isEndless(i)`, `TOTAL`, `CURATED` |
| `pathfinder.js` | `Pathfinder.bfs(...)` | BFS shortest path → `[{r,c}, …]` |
| `character.js` | `Character.draw(...)` | Pixel-perfect sprite-sheet walk cycle |
| `torch.js` | `Torch.draw(...)`, `Torch.setRadius(r)` | Black overlay with a radial light hole + flame flicker |
| `renderer.js` | `Renderer` | Maze/exit drawing + world↔screen↔cell conversions |
| `storage.js` | `Save` | Best steps, stars, unlock count, settings (key `ltw.save.v1`) |
| `audio.js` | `Sound` | `unlock/init/step/click/win/deny/startMusic/stopMusic`, sound & music toggles |
| `game.js` | game loop & wiring | `menu → levels → settings → play → pause → win` state machine |
| `menu.js` | (self-contained) | Main-menu darkness, mouse-torch reveal, idle left→right sweep |

---

## Customizing

All quick knobs live in **`js/config.js`**:

```js
const CONFIG = {
  TILE: 32,                  // px per maze cell
  CHAR_SPEED: 1.8,           // wanderer speed (px/frame)
  TORCH_RADIUS: 145,         // base light radius
  TORCH_RADIUS_MIN: 90,      // never shrinks below this
  TORCH_SHRINK_PER_LEVEL: 10,// torch shrink per depth
  FLICKER_AMOUNT: 6,         // flame wobble (0 = none)
};
```

**Add a handcrafted level** — append to `MAZES` in `js/mazes.js`. New entries become the *start* of the campaign automatically (everything after the curated set is procedural):

```js
{
  grid: [ /* 2-D array, 0 = floor, 1 = wall */ ],
  start: [row, col],   // a floor cell
  exit:  [row, col],   // a floor cell
}
```

**Show more levels** — bump `TOTAL` in `js/levels.js` (procedural mazes fill any index beyond the curated ones).

**Tune star thresholds** — `starsFor()` in `js/game.js`.

**Swap the character sprite** — replace `assets/sprites/llama.png` (a horizontal sheet) and update `FRAME_SIZE` / `FRAME_COUNT` in `js/character.js`. The `.piskel` source opens at [piskelapp.com](https://www.piskelapp.com/).

**Change difficulty curves** — `DIFFICULTY` map and `torchRadius()` in `js/levels.js`.

---

## How it works (in brief)

1. **Two stacked canvases.** `#game-canvas` draws the maze, exit, and character in world space (camera keeps the wanderer centered). `#ui-canvas` paints a full‑screen black overlay and "cuts" a soft radial hole at the cursor — that hole *is* the torchlight.
2. **Pointing = pathfinding.** On `mousemove`, the screen point is converted to a maze cell; BFS finds the shortest path from the wanderer to it; the character walks the queued cells.
3. **Win = scoring.** Reaching the exit computes stars vs. the BFS‑optimal step count, records best/stars, unlocks the next level, and shows the win screen.
4. **Procedural depth.** Beyond the curated mazes, `MazeGen` builds a seeded perfect maze sized to the depth, guaranteeing a solvable, ever‑larger labyrinth.

---

## Tech stack

- Vanilla **JavaScript** (no frameworks, no bundler)
- **HTML5 Canvas 2D**
- **Web Audio API** (all sound synthesised at runtime)
- **CSS** masks / custom `@property` animations for the menu lighting
- **localStorage** for persistence

---

*Built as part of a portfolio. Guide the wanderer. Mind the dark.*
