package lighttheway;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/** Draws maze tiles + the exit marker, and world/screen/cell conversions (ported from js/renderer.js). */
public final class Renderer {

    public static final int TILE = Config.TILE;

    private static final Color WALL  = Color.web("#17110a");
    private static final Color FLOOR = Color.web("#0c0c0c");
    private static final Color BEVEL = Color.rgb(255, 255, 255, 0.025);

    public static void drawMaze(GraphicsContext gc, int[][] maze, int rows, int cols,
                                double ox, double oy, int exitR, int exitC, double timeMs) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = ox + c * TILE;
                double y = oy + r * TILE;
                if (maze[r][c] == 1) {
                    gc.setFill(WALL);
                    gc.fillRect(x, y, TILE, TILE);
                    gc.setFill(BEVEL);
                    gc.fillRect(x, y, TILE, 1);
                    gc.fillRect(x, y, 1, TILE);
                } else {
                    gc.setFill(FLOOR);
                    gc.fillRect(x, y, TILE, TILE);
                }
            }
        }

        // exit tile pulse + glyph
        double ex = ox + exitC * TILE;
        double ey = oy + exitR * TILE;
        double pulse = 0.5 + 0.5 * Math.sin(timeMs / 380.0);

        gc.setFill(Color.rgb(0, 255, 190, 0.07 + 0.07 * pulse));
        gc.fillRect(ex, ey, TILE, TILE);

        gc.setFont(Font.font("monospace", TILE - 8));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(Color.rgb(0, 255, 190, 0.5 + 0.4 * pulse));
        gc.fillText("◈", ex + TILE / 2.0, ey + TILE / 2.0);
    }

    public static double[] cellToWorld(int r, int c) {
        return new double[]{c * TILE + TILE / 2.0, r * TILE + TILE / 2.0};
    }

    public static int[] worldToCell(double wx, double wy) {
        return new int[]{(int) Math.round((wy - TILE / 2.0) / TILE),
                         (int) Math.round((wx - TILE / 2.0) / TILE)};
    }

    public static int[] screenToCell(double sx, double sy, double charX, double charY, double cw, double ch) {
        double ox = cw / 2 - charX;
        double oy = ch / 2 - charY;
        return new int[]{(int) Math.floor((sy - oy) / TILE),
                         (int) Math.floor((sx - ox) / TILE)};
    }

    public static double[] getOffset(double charX, double charY, double cw, double ch) {
        return new double[]{cw / 2 - charX, ch / 2 - charY};
    }

    private Renderer() {}
}
