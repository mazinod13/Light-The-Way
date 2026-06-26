package lighttheway;

/** Campaign + endless procedural run, with difficulty scaling (ported from js/levels.js). */
public final class Levels {

    public static final int CURATED = Mazes.LIST.length; // hand-built campaign length
    public static final int TOTAL   = 12;                // levels shown in the picker

    /** A concrete, playable level. */
    public static final class Level {
        public final int[][] grid;
        public final int sr, sc, er, ec;
        Level(int[][] grid, int sr, int sc, int er, int ec) {
            this.grid = grid; this.sr = sr; this.sc = sc; this.er = er; this.ec = ec;
        }
    }

    public static double difficultyMult(String difficulty) {
        if ("easy".equals(difficulty)) return 1.30;
        if ("hard".equals(difficulty)) return 0.72;
        return 1.00; // normal
    }

    public static Level get(int i) {
        if (i < CURATED) {
            Mazes.Def d = Mazes.LIST[i];
            return new Level(d.grid, d.start[0], d.start[1], d.exit[0], d.exit[1]);
        }
        int step = i - CURATED;
        int cols = Math.min(9 + step, 21);
        int rows = Math.min(7 + step, 15);
        long seed = 0x9E37L + (long) i * 2654435761L;
        MazeGen.Result g = MazeGen.generate(seed, cols, rows);
        return new Level(g.grid, g.start[0], g.start[1], g.exit[0], g.exit[1]);
    }

    public static double torchRadius(int i, String difficulty) {
        double base = Config.TORCH_RADIUS - i * Config.TORCH_SHRINK_PER_LEVEL;
        double mult = difficultyMult(difficulty);
        return Math.max(Config.TORCH_RADIUS_MIN, Math.round(base * mult));
    }

    public static String name(int i) {
        return i < CURATED ? "Depth " + (i + 1) : "Abyss " + (i - CURATED + 1);
    }

    public static boolean isEndless(int i) { return i >= CURATED; }

    private Levels() {}
}
