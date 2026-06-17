// ─────────────────────────────────────────────────
//  audio.js — procedural sound, no asset files
//
//  All sounds are synthesised with the Web Audio API,
//  so there are zero .mp3/.wav dependencies.
//    Sound.unlock()  → call on first user gesture
//    Sound.click / step / win / deny  → one-shots
//    Sound.startMusic / stopMusic     → ambient drone
// ─────────────────────────────────────────────────

const Sound = (() => {

  let ctx = null, master = null, drone = null;
  let soundOn = true, musicOn = true;

  function ensure() {
    if (ctx) return;
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    ctx = new AC();
    master = ctx.createGain();
    master.gain.value = 0.5;
    master.connect(ctx.destination);
  }

  // one-shot tone with a quick attack + exponential tail
  function tone(freq, dur, type = 'sine', vol = 0.2, when = 0) {
    if (!soundOn || !ctx) return;
    const t = ctx.currentTime + when;
    const o = ctx.createOscillator();
    const g = ctx.createGain();
    o.type = type;
    o.frequency.setValueAtTime(freq, t);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.linearRampToValueAtTime(vol, t + 0.008);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(master);
    o.start(t); o.stop(t + dur + 0.02);
  }

  // ── one-shots ─────────────────────────────────────
  const step  = () => tone(80 + Math.random() * 26, 0.07, 'triangle', 0.05);
  const click = () => { tone(540, 0.05, 'square', 0.10); tone(820, 0.04, 'square', 0.05, 0.01); };
  const deny  = () => { tone(180, 0.16, 'sawtooth', 0.10); tone(120, 0.18, 'sawtooth', 0.08, 0.02); };
  const win   = () => [523.25, 659.25, 783.99, 1046.5]
                        .forEach((f, i) => tone(f, 0.32, 'sine', 0.16, i * 0.11));

  // ── ambient drone ─────────────────────────────────
  function startMusic() {
    if (!musicOn || !ctx || drone) return;
    const g = ctx.createGain();
    g.gain.setValueAtTime(0.0001, ctx.currentTime);
    g.gain.linearRampToValueAtTime(0.05, ctx.currentTime + 3);
    g.connect(master);

    const o1 = ctx.createOscillator(); o1.type = 'sine'; o1.frequency.value = 55;    // A1
    const o2 = ctx.createOscillator(); o2.type = 'sine'; o2.frequency.value = 82.41;  // E2
    const lfo = ctx.createOscillator(); lfo.frequency.value = 0.07;                   // slow swell
    const lfoGain = ctx.createGain(); lfoGain.gain.value = 5;
    lfo.connect(lfoGain); lfoGain.connect(o2.frequency);

    o1.connect(g); o2.connect(g);
    o1.start(); o2.start(); lfo.start();
    drone = { g, nodes: [o1, o2, lfo] };
  }

  function stopMusic() {
    if (!drone || !ctx) return;
    const d = drone; drone = null;
    d.g.gain.cancelScheduledValues(ctx.currentTime);
    d.g.gain.linearRampToValueAtTime(0.0001, ctx.currentTime + 0.8);
    const nodes = d.nodes;
    setTimeout(() => { try { nodes.forEach(n => n.stop()); } catch { /* already stopped */ } }, 1000);
  }

  return {
    init(s) { soundOn = !!s.sound; musicOn = !!s.music; },
    unlock() { ensure(); if (ctx && ctx.state === 'suspended') ctx.resume(); },
    setSound(v) { soundOn = !!v; },
    setMusic(v) { musicOn = !!v; if (!v) stopMusic(); },
    isMusicPlaying: () => !!drone,
    step, click, deny, win, startMusic, stopMusic,
  };
})();
