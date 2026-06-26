package lighttheway;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** BFS shortest path through a maze (ported from js/pathfinder.js). */
public final class Pathfinder {

    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /** Returns the shortest path as a list of {r, c}; empty if none / same cell. */
    public static List<int[]> bfs(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        List<int[]> path = new ArrayList<>();
        if (sr == er && sc == ec) return path;

        boolean[][] visited = new boolean[rows][cols];
        int[][][] prev = new int[rows][cols][];
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sr, sc});
        visited[sr][sc] = true;

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            for (int[] d : DIRS) {
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                if (maze[nr][nc] == 1) continue;
                if (visited[nr][nc]) continue;
                visited[nr][nc] = true;
                prev[nr][nc] = new int[]{r, c};
                if (nr == er && nc == ec) {
                    int[] step = {nr, nc};
                    while (step[0] != sr || step[1] != sc) {
                        path.add(new int[]{step[0], step[1]});
                        step = prev[step[0]][step[1]];
                    }
                    java.util.Collections.reverse(path);
                    return path;
                }
                queue.add(new int[]{nr, nc});
            }
        }
        return path;
    }

    private Pathfinder() {}
}
