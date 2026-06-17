// ─────────────────────────────────────────────────
//  menu.js — interactive torch for the main menu
//
//  Three effects on the start screen:
//    • the mouse acts as a torch, revealing content
//    • press T (or the on-screen button) to toggle it
//    • after a few idle seconds, a beam sweeps L→R
//
//  Tweak IDLE_MS for the idle delay, SWEEP_GAP for
//  how often the sweep repeats while idle.
// ─────────────────────────────────────────────────

(() => {
  const screen  = document.getElementById('start-screen');
  const content = document.getElementById('menu-content');
  const flame   = document.getElementById('menu-torch');
  const toggle  = document.getElementById('menu-torch-hint');
  if (!screen || !content || !flame) return;

  const IDLE_MS   = 4500;  // idle time before the first sweep
  const SWEEP_GAP = 2200;  // extra wait between repeated sweeps

  let torchOn    = false;  // menu starts completely dark
  let idleTimer  = null;
  let sweepTimer = null;

  const menuVisible = () => screen.style.display !== 'none';

  // ── torch follows the mouse (coords relative to the content box) ──
  function moveTorch(x, y) {
    const r = content.getBoundingClientRect();
    if (r.width && r.height) {
      content.style.setProperty('--mx', ((x - r.left) / r.width  * 100) + '%');
      content.style.setProperty('--my', ((y - r.top)  / r.height * 100) + '%');
    }
    flame.style.transform = `translate(${x}px, ${y}px) translate(-50%, -50%)`;
  }

  // ── idle → sweep a beam across the content (torch OFF only) ────────
  function runSweep() {
    if (torchOn || !menuVisible()) return;   // teaser sweep lives in the dark
    content.classList.remove('sweeping');
    void content.offsetWidth;          // restart the animation
    content.classList.add('sweeping');
  }
  content.addEventListener('animationend', () => content.classList.remove('sweeping'));

  function stopIdle() {
    clearTimeout(idleTimer);
    clearInterval(sweepTimer);
  }

  function armIdle() {
    stopIdle();
    if (torchOn) return;   // no auto sweep while the torch is lit
    idleTimer = setTimeout(() => {
      runSweep();
      sweepTimer = setInterval(runSweep, IDLE_MS + SWEEP_GAP);
    }, IDLE_MS);
  }

  // ── toggle the torch ──────────────────────────────
  function setTorch(on) {
    torchOn = on;
    screen.classList.toggle('torch-on', on);
    toggle.innerHTML = on
      ? 'TORCH ON · PRESS <kbd>T</kbd>'
      : 'PRESS <kbd>T</kbd> FOR TORCH';
    if (on) stopIdle(); else armIdle();
  }

  // ── input ─────────────────────────────────────────
  document.addEventListener('mousemove', e => {
    if (!menuVisible()) return;
    moveTorch(e.clientX, e.clientY);
    if (!torchOn) armIdle();   // movement re-arms the idle sweep in the dark
  });

  document.addEventListener('keydown', e => {
    if (!menuVisible()) return;
    if (e.key === 't' || e.key === 'T') setTorch(!torchOn);
  });

  toggle.addEventListener('click', () => setTorch(!torchOn));

  // entering the game stops the menu's idle loop
  const startBtn = screen.querySelector('.btn:not(.ghost)');
  if (startBtn) startBtn.addEventListener('click', stopIdle);

  // ── start ─────────────────────────────────────────
  moveTorch(window.innerWidth / 2, window.innerHeight * 0.44);
  setTorch(false);  // begin in darkness — only "PRESS T" shows
})();
