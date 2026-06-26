package lighttheway;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Procedural sound — no audio files (ported from js/audio.js).
 * One-shots are synthesised into a PCM buffer and played; the ambient drone
 * is streamed on a background thread. All wrapped in try/catch so a missing
 * audio device never crashes the game.
 */
public final class Sound {

    private static final float SR = 44100f;
    private static final AudioFormat FMT = new AudioFormat(SR, 16, 1, true, false);

    private static volatile boolean soundOn = true;
    private static volatile boolean musicOn = true;
    private static volatile boolean droneOn = false;

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx"); t.setDaemon(true); return t;
    });

    public static void init(boolean sound, boolean music) { soundOn = sound; musicOn = music; }
    public static void unlock() { /* javax.sound needs no user-gesture unlock */ }
    public static void setSound(boolean v) { soundOn = v; }
    public static void setMusic(boolean v) { musicOn = v; if (!v) stopMusic(); }
    public static boolean isMusicPlaying() { return droneOn; }

    // ── one-shots ─────────────────────────────────────
    public static void step() {
        float[] b = buf(0.07);
        tone(b, 80 + Math.random() * 26, 0, 0.07, 0.05, "triangle");
        play(b);
    }
    public static void click() {
        float[] b = buf(0.11);
        tone(b, 540, 0,    0.05, 0.10, "square");
        tone(b, 820, 0.01, 0.04, 0.05, "square");
        play(b);
    }
    public static void deny() {
        float[] b = buf(0.22);
        tone(b, 180, 0,    0.16, 0.10, "saw");
        tone(b, 120, 0.02, 0.18, 0.08, "saw");
        play(b);
    }
    public static void win() {
        float[] b = buf(0.9);
        double[] notes = {523.25, 659.25, 783.99, 1046.5};
        for (int i = 0; i < notes.length; i++) tone(b, notes[i], i * 0.11, 0.32, 0.16, "sine");
        play(b);
    }

    // ── ambient drone ─────────────────────────────────
    public static void startMusic() {
        if (!musicOn || droneOn) return;
        droneOn = true;
        Thread t = new Thread(Sound::droneLoop, "drone");
        t.setDaemon(true);
        t.start();
    }
    public static void stopMusic() { droneOn = false; }

    private static void droneLoop() {
        try {
            SourceDataLine line = AudioSystem.getSourceDataLine(FMT);
            line.open(FMT, 8192);
            line.start();
            int block = 1024;
            byte[] bytes = new byte[block * 2];
            double ph1 = 0, ph2 = 0, lfo = 0, gain = 0;
            while (droneOn) {
                for (int i = 0; i < block; i++) {
                    gain += (0.05 - gain) * 0.00003;          // slow swell to target
                    double mod = Math.sin(lfo) * 5;           // ±5 Hz drift on the 5th
                    double s = (Math.sin(ph1) + Math.sin(ph2)) * 0.5 * gain;
                    short v = (short) (Math.max(-1, Math.min(1, s)) * 32767);
                    bytes[i * 2] = (byte) (v & 0xff);
                    bytes[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
                    ph1 += 2 * Math.PI * 55.0 / SR;
                    ph2 += 2 * Math.PI * (82.41 + mod) / SR;
                    lfo += 2 * Math.PI * 0.07 / SR;
                }
                line.write(bytes, 0, bytes.length);
            }
            line.stop();
            line.close();
        } catch (Exception ignored) {}
    }

    // ── synthesis helpers ─────────────────────────────
    private static float[] buf(double seconds) { return new float[(int) (seconds * SR)]; }

    private static void tone(float[] b, double freq, double start, double dur, double amp, String type) {
        int s0 = (int) (start * SR);
        int n = (int) (dur * SR);
        double atk = 0.008;
        double k = Math.log(10000) / Math.max(0.001, dur - atk); // exp decay to ~0.0001
        for (int i = 0; i < n; i++) {
            int idx = s0 + i;
            if (idx < 0 || idx >= b.length) continue;
            double t = i / SR;
            double env = t < atk ? t / atk : Math.exp(-(t - atk) * k);
            double frac = (freq * t) % 1.0;
            double w;
            switch (type) {
                case "square":   w = Math.sin(2 * Math.PI * freq * t) >= 0 ? 1 : -1; break;
                case "saw":      w = 2 * frac - 1; break;
                case "triangle": w = 2 * Math.abs(2 * frac - 1) - 1; break;
                default:         w = Math.sin(2 * Math.PI * freq * t);
            }
            b[idx] += (float) (w * amp * env);
        }
    }

    private static void play(float[] b) {
        if (!soundOn) return;
        final byte[] pcm = toPCM(b);
        EXEC.submit(() -> {
            try {
                SourceDataLine line = AudioSystem.getSourceDataLine(FMT);
                line.open(FMT);
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
                line.stop();
                line.close();
            } catch (Exception ignored) {}
        });
    }

    private static byte[] toPCM(float[] b) {
        byte[] out = new byte[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            float v = Math.max(-1f, Math.min(1f, b[i]));
            short s = (short) (v * 32767);
            out[i * 2] = (byte) (s & 0xff);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    private Sound() {}
}
