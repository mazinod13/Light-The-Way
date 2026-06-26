package lighttheway;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Darkness overlay + flickering torch light (ported from js/torch.js).
 * JavaFX can't do canvas "destination-out", so instead of cutting a hole we
 * paint a radial gradient that is transparent at the centre and opaque black
 * at the edge — same visual result, no compositing tricks.
 */
public final class Torch {

    private static final double FLICKER_STRENGTH = Config.FLICKER_AMOUNT;
    private static double flickerT = 0;
    private static double baseRadius = Config.TORCH_RADIUS;

    public static void setRadius(double r) { baseRadius = r; }

    public static void draw(GraphicsContext gc, double cw, double ch, double mouseX, double mouseY) {
        flickerT += 0.055;
        double flick = Math.sin(flickerT * 2.1) * FLICKER_STRENGTH * 0.7
                     + Math.sin(flickerT * 5.9) * FLICKER_STRENGTH * 0.3;
        double r = baseRadius + flick;

        double cx = cw / 2, cy = ch / 2; // character pinned to screen centre

        gc.clearRect(0, 0, cw, ch);

        // 1. darkness: transparent at the torch centre → opaque black past the radius
        RadialGradient dark = new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
            new Stop(0.00, Color.rgb(0, 0, 0, 0)),
            new Stop(0.38, Color.rgb(0, 0, 0, 0)),
            new Stop(0.65, Color.rgb(0, 0, 0, 0.72)),
            new Stop(0.85, Color.rgb(0, 0, 0, 0.95)),
            new Stop(1.00, Color.rgb(0, 0, 0, 1)));
        gc.setFill(dark);
        gc.fillRect(0, 0, cw, ch);

        // 2. warm amber tint inside the torch
        RadialGradient warm = new RadialGradient(0, 0, cx, cy, r * 0.5, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(255, 170, 50, 0.07)),
            new Stop(1.0, Color.rgb(255, 170, 50, 0)));
        gc.setFill(warm);
        gc.fillRect(0, 0, cw, ch);

        // 3. mouse cursor glow dot + crosshair ring
        gc.setFill(Color.rgb(255, 220, 120, 0.85));
        gc.fillOval(mouseX - 5, mouseY - 5, 10, 10);
        gc.setStroke(Color.rgb(255, 220, 120, 0.35));
        gc.setLineWidth(1);
        gc.strokeOval(mouseX - 14, mouseY - 14, 28, 28);
    }

    private Torch() {}
}
