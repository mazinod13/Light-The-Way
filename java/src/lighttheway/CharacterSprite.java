package lighttheway;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Sprite-sheet walk animation (ported from js/character.js). */
public final class CharacterSprite {

    private static final int FRAME_SIZE      = 48;
    private static final int FRAME_COUNT      = 6;
    private static final int DISPLAY_SIZE     = 48;
    private static final int TICKS_PER_FRAME  = 5;

    private static Image sheet;
    private static boolean ready = false;

    static {
        try {
            sheet = new Image(CharacterSprite.class.getResourceAsStream("/assets/sprites/llama.png"));
            ready = sheet != null && !sheet.isError();
        } catch (Exception e) {
            ready = false;
        }
    }

    public static void draw(GraphicsContext gc, double x, double y, long walkTick, boolean isMoving) {
        if (!ready) { // fallback so the game still runs without the asset
            gc.setFill(Color.web("#caa15a"));
            gc.fillOval(x - 12, y - 12, 24, 24);
            return;
        }
        int frame = isMoving ? (int) ((walkTick / TICKS_PER_FRAME) % FRAME_COUNT) : 0;
        double sx = frame * FRAME_SIZE;
        double d = DISPLAY_SIZE;

        gc.setImageSmoothing(false);
        gc.drawImage(sheet,
            sx, 0, FRAME_SIZE, FRAME_SIZE,
            Math.round(x - d / 2), Math.round(y - d / 2), d, d);
    }

    private CharacterSprite() {}
}
