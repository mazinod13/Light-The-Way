package lighttheway;

/**
 * Seeded procedural maze generator (ported from js/maze-gen.js).
 * Recursive-backtracker (iterative) → a "perfect" maze, always solvable.
 */
public final class MazeGen {

    /** Generated maze + its start/exit cells. */
    public static final class Result {
        public final int[][] grid;
        public final int[] start;
        public final int[] exit;
        Result(int[][] grid, int[] start, int[] exit) {
            this.grid = grid; this.start = start; this.exit = exit;
        }
    }

    // mulberry32 — tiny deterministic PRNG (32-bit int math matches JS)
    private static final class Rand {
        private int s;
        Rand(long seed) { s = (int) seed; }
        double next() {
            s = s + 0x6D2B79F5;
            int t = (s ^ (s >>> 15)) * (1 | s);
            t = ((t + ((t ^ (t >>> 7)) * (61 | t))) ^ t);
            return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
        }
    }

    public static Result generate(long seed, int cols, int rows) {
        Rand rand = new Rand(seed);
        int R = rows * 2 + 1, C = cols * 2 + 1;
        int[][] grid = new int[R][C];
        for (int[] row : grid) java.util.Arrays.fill(row, 1);

        boolean[][] seen = new boolean[rows][cols];
        java.util.Deque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[]{0, 0});
        seen[0][0] = true;
        grid[1][1] = 0;

        int[][] base = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            int cx = cell[0], cy = cell[1];

            // shuffle a copy of the directions
            int[][] dirs = {base[0].clone(), base[1].clone(), base[2].clone(), base[3].clone()};
            for (int i = dirs.length - 1; i > 0; i--) {
                int j = (int) Math.floor(rand.next() * (i + 1));
                int[] tmp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = tmp;
            }

            boolean advanced = false;
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows || seen[ny][nx]) continue;
                seen[ny][nx] = true;
                grid[ny * 2 + 1][nx * 2 + 1] = 0;             // carve room
                grid[cy * 2 + 1 + d[1]][cx * 2 + 1 + d[0]] = 0; // carve wall between
                stack.push(new int[]{nx, ny});
                advanced = true;
                break;
            }
            if (!advanced) stack.pop();
        }

        return new Result(grid, new int[]{1, 1}, new int[]{R - 2, C - 2});
    }

    private MazeGen() {}
}
