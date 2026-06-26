package lighttheway;

/** All tunable game constants (ported from js/config.js). */
public final class Config {
    public static final int    TILE                   = 32;    // px size of one maze cell
    public static final double CHAR_SPEED             = 1.8;   // px per frame
    public static final double TORCH_RADIUS           = 145;   // base light radius
    public static final double TORCH_RADIUS_MIN       = 90;    // never shrinks below this
    public static final double TORCH_SHRINK_PER_LEVEL = 10;    // torch shrink per depth
    public static final double FLICKER_AMOUNT         = 6;     // flame wobble (0 = none)

    private Config() {}
}
