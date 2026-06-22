// ─────────────────────────────────────────────────
//  game.js — main loop, screens, input, state machine
//
//  States: menu · levels · settings · play · pause · win
//
//  Wires together:
//    Save (storage) · Sound (audio) · Levels (+ MazeGen)
//    Pathfinder · Character · Torch · Renderer
// ─────────────────────────────────────────────────

const $ = id => document.getElementById(id);

// ── CANVAS SETUP ─────────────────────────────────
const gc = $('game-canvas');
const uc = $('ui-canvas');
const gx = gc.getContext('2d');
const ux = uc.getContext('2d');

function resize() {
  gc.width = uc.width = window.innerWidth;
  gc.height = uc.height = window.innerHeight;
}
window.addEventListener('resize', resize);
resize();

// ── STATE ─────────────────────────────────────────
const CHAR_SPEED = CONFIG.CHAR_SPEED;

let state = 'menu';
let lvlIdx = 0;
let maze, rows, cols, exitCell;
let steps = 0, optimalSteps = 1;

let charX = 0, charY = 0;
let path = [], pathTarget = null;

let mouseSX = window.innerWidth / 2;
let mouseSY = window.innerHeight / 2;

let walkTick = 0, isMoving = false, raf = null;

// ── SCREEN / STATE MANAGEMENT ─────────────────────
const SCREENS = {
  menu:     'start-screen',
  levels:   'levels-screen',
  settings: 'settings-screen',
  win:      'win-screen',
  pause:    'pause-screen',
};

function setState(next) {
  state = next;

  // show the matching overlay (play has none)
  Object.entries(SCREENS).forEach(([k, id]) => {
    const el = $(id);
    if (el) el.style.display = (k === next) ? 'flex' : 'none';
  });

  // HUD only while in a level
  $('hud').style.display = (next === 'play' || next === 'pause') ? 'flex' : 'none';
  $('hint').style.display = (next === 'play') ? 'block' : 'none';

  if (next === 'play') {
    Sound.startMusic();
  } else {
    Sound.stopMusic();
  }

  // clear the world when we leave the level entirely
  if (next === 'menu' || next === 'levels' || next === 'settings') {
    cancelAnimationFrame(raf); raf = null;
    gx.clearRect(0, 0, gc.width, gc.height);
    ux.clearRect(0, 0, uc.width, uc.height);
  }
}

// ── LEVEL LOAD ────────────────────────────────────
function loadLevel(idx) {
  lvlIdx = idx;
  const def = Levels.get(idx);
  maze = def.grid;
  rows = maze.length;
  cols = maze[0].length;
  exitCell = { r: def.exit.r, c: def.exit.c };

  const sw = Renderer.cellToWorld(def.start.r, def.start.c);
  charX = sw.x; charY = sw.y;
  path = []; pathTarget = null;
  steps = 0;

  const op = Pathfinder.bfs(maze, rows, cols, def.start.r, def.start.c, exitCell.r, exitCell.c);
  optimalSteps = Math.max(1, op.length);

  Torch.setRadius(Levels.torchRadius(idx, Save.settings().difficulty));
  updateHUD();
}

function playLevel(idx) {
  loadLevel(idx);
  setState('play');
  if (!raf) loop();
}

// ── STAR RATING ───────────────────────────────────
function starsFor(stepCount) {
  if (stepCount <= Math.ceil(optimalSteps * 1.15)) return 3;
  if (stepCount <= Math.ceil(optimalSteps * 1.6))  return 2;
  return 1;
}

// ── PATHFINDING ───────────────────────────────────
function repath(tr, tc) {
  if (tc < 0 || tr < 0 || tr >= rows || tc >= cols) return;
  if (maze[tr][tc] === 1) return;
  if (pathTarget && pathTarget.r === tr && pathTarget.c === tc) return;
  pathTarget = { r: tr, c: tc };
  const cc = Renderer.worldToCell(charX, charY);
  path = Pathfinder.bfs(maze, rows, cols, cc.r, cc.c, tr, tc);
}

// ── CHARACTER MOVEMENT ────────────────────────────
function stepCharacter() {
  if (!path.length) { isMoving = false; return; }
  isMoving = true;
  const next = path[0];
  const target = Renderer.cellToWorld(next.r, next.c);
  const dx = target.x - charX;
  const dy = target.y - charY;
  const dist = Math.hypot(dx, dy);

  if (dist < CHAR_SPEED + 0.5) {
    charX = target.x; charY = target.y;
    path.shift();
    steps++;
    Sound.step();
    updateHUD();
    const cc = Renderer.worldToCell(charX, charY);
    if (cc.r === exitCell.r && cc.c === exitCell.c) winLevel();
  } else {
    charX += (dx / dist) * CHAR_SPEED;
    charY += (dy / dist) * CHAR_SPEED;
    walkTick++;
  }
}

// ── MAIN LOOP ─────────────────────────────────────
function loop() {
  if (state !== 'play') { raf = null; return; }

  stepCharacter();

  const { ox, oy } = Renderer.getOffset(charX, charY, gc.width, gc.height);
  Renderer.drawMaze(gx, maze, rows, cols, ox, oy, exitCell);
  Character.draw(gx, charX + ox, charY + oy, walkTick, isMoving);
  Torch.draw(ux, uc.width, uc.height, mouseSX, mouseSY);

  raf = requestAnimationFrame(loop);
}

// ── HUD ───────────────────────────────────────────
function updateHUD() {
  $('s-lvl').textContent   = lvlIdx + 1;
  $('s-steps').textContent = steps;
  const b = Save.best(lvlIdx);
  $('s-best').textContent  = b == null ? '—' : b;
}

function starMarkup(n) {
  let s = '';
  for (let i = 0; i < 3; i++) s += `<span class="${i < n ? 'on' : ''}">★</span>`;
  return s;
}

// ── FLOW ──────────────────────────────────────────
function winLevel() {
  const stars = starsFor(steps);
  const prevBest = Save.best(lvlIdx);
  Save.recordWin(lvlIdx, steps, stars);
  cancelAnimationFrame(raf); raf = null;
  Sound.win();

  $('w-steps').textContent = steps;
  $('w-best').textContent  = Save.best(lvlIdx);
  $('w-stars').innerHTML   = starMarkup(stars);
  $('w-record').style.display =
    (prevBest == null || steps < prevBest) ? 'block' : 'none';
  $('btn-next').style.display =
    (lvlIdx + 1 < Levels.TOTAL) ? 'inline-block' : 'none';
  setState('win');
}

function pause()  { if (state === 'play')  { cancelAnimationFrame(raf); raf = null; setState('pause'); } }
function resume() { if (state === 'pause') { setState('play'); if (!raf) loop(); } }
function restart() { Sound.click(); playLevel(lvlIdx); }

// ── LEVEL SELECT (built once, refreshed on open) ──
function buildLevelGrid() {
  const grid = $('level-grid');
  grid.innerHTML = '';
  for (let i = 0; i < Levels.TOTAL; i++) {
    const unlocked = Save.isUnlocked(i);
    const card = document.createElement('button');
    card.className = 'level-card' + (unlocked ? '' : ' locked') +
                     (Levels.isEndless(i) ? ' endless' : '');
    card.disabled = !unlocked;
    if (unlocked) {
      card.innerHTML =
        `<span class="lc-num">${i + 1}</span>` +
        `<span class="lc-stars">${starMarkup(Save.stars(i))}</span>` +
        `<span class="lc-name">${Levels.name(i)}</span>`;
      card.addEventListener('click', () => { Sound.click(); playLevel(i); });
    } else {
      card.innerHTML = `<span class="lc-num">${i + 1}</span><span class="lc-name">Locked</span>`;
    }
    grid.appendChild(card);
  }
  $('levels-total').textContent = Save.totalStars();
  $('levels-max').textContent   = Levels.TOTAL * 3;
}

function openLevels() { Sound.click(); buildLevelGrid(); setState('levels'); }

// ── SETTINGS ──────────────────────────────────────
function refreshSettings() {
  const s = Save.settings();
  $('set-sound').textContent = s.sound ? 'ON' : 'OFF';
  $('set-sound').classList.toggle('on', s.sound);
  $('set-music').textContent = s.music ? 'ON' : 'OFF';
  $('set-music').classList.toggle('on', s.music);
  document.querySelectorAll('#set-diff .seg').forEach(b =>
    b.classList.toggle('active', b.dataset.diff === s.difficulty));
}

function openSettings() { Sound.click(); refreshSettings(); setState('settings'); }

// ── INPUT ─────────────────────────────────────────
document.addEventListener('mousemove', e => {
  mouseSX = e.clientX;
  mouseSY = e.clientY;
  if (state !== 'play') return;
  const cell = Renderer.screenToCell(e.clientX, e.clientY, charX, charY, gc.width, gc.height);
  repath(cell.r, cell.c);
});

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    if (state === 'play') pause();
    else if (state === 'pause') resume();
  } else if (e.key === 'r' || e.key === 'R') {
    if (state === 'play' || state === 'pause') restart();
  }
});

// unlock audio on the first gesture (browser autoplay policy)
function primeAudio() {
  Sound.unlock();
  window.removeEventListener('pointerdown', primeAudio);
  window.removeEventListener('keydown', primeAudio);
}
window.addEventListener('pointerdown', primeAudio);
window.addEventListener('keydown', primeAudio);

// ── WIRING ────────────────────────────────────────
function bind(id, fn) { const el = $(id); if (el) el.addEventListener('click', fn); }

function init() {
  Sound.init(Save.settings());

  // main menu
  bind('btn-play',     () => { Sound.click(); playLevel(Math.min(Save.unlocked - 1, Levels.TOTAL - 1)); });
  bind('btn-levels',   openLevels);
  bind('btn-settings', openSettings);

  // HUD
  bind('btn-pause',   () => { Sound.click(); pause(); });
  bind('btn-restart', restart);
  bind('btn-mute',    toggleMute);

  // pause menu
  bind('btn-resume',     () => { Sound.click(); resume(); });
  bind('btn-restart-2',  restart);
  bind('btn-pause-levels', openLevels);
  bind('btn-pause-menu',   () => { Sound.click(); setState('menu'); });

  // win screen
  bind('btn-next',       () => { Sound.click(); playLevel(Math.min(lvlIdx + 1, Levels.TOTAL - 1)); });
  bind('btn-replay',     () => { Sound.click(); playLevel(lvlIdx); });
  bind('btn-win-levels', openLevels);

  // level select
  bind('btn-levels-back', () => { Sound.click(); setState('menu'); });

  // settings
  bind('btn-settings-back', () => { Sound.click(); setState('menu'); });
  bind('set-sound', () => {
    const v = !Save.settings().sound; Save.setSetting('sound', v); Sound.setSound(v);
    refreshSettings(); updateMute(); Sound.click();
  });
  bind('set-music', () => {
    const v = !Save.settings().music; Save.setSetting('music', v); Sound.setMusic(v);
    if (v && state === 'play') Sound.startMusic();
    refreshSettings(); Sound.click();
  });
  document.querySelectorAll('#set-diff .seg').forEach(b =>
    b.addEventListener('click', () => {
      Save.setSetting('difficulty', b.dataset.diff);
      refreshSettings(); Sound.click();
    }));
  bind('btn-reset', () => {
    if (confirm('Reset all progress, stars and unlocks?')) {
      Save.reset(); Sound.init(Save.settings()); refreshSettings(); updateMute();
    }
  });

  updateMute();
  setState('menu');
}

function toggleMute() {
  const on = !Save.settings().sound;     // master audio on/off
  Save.setSetting('sound', on);
  Sound.setSound(on);
  if (!on) {
    Sound.setMusic(false);               // silence the drone too
  } else {
    Sound.setMusic(Save.settings().music);  // honour the music preference
    if (Save.settings().music && state === 'play') Sound.startMusic();
  }
  updateMute();
}

function updateMute() {
  const on = Save.settings().sound;
  const btn = $('btn-mute');
  if (btn) {
    btn.textContent = on ? 'SOUND' : 'MUTED';
    btn.classList.toggle('off', !on);
    btn.title = on ? 'Mute' : 'Unmute';
  }
}

init();
