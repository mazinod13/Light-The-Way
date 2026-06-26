package lighttheway;

import java.util.prefs.Preferences;

/** Progress + settings persistence via the Java Preferences API (ported from js/storage.js). */
public final class Save {

    private static final Preferences P = Preferences.userRoot().node("lighttheway/save_v1");

    public static int     unlocked()        { return P.getInt("unlocked", 1); }
    public static boolean isUnlocked(int i)  { return i < unlocked(); }
    public static Integer best(int i)        { int v = P.getInt("best." + i, -1); return v < 0 ? null : v; }
    public static int     stars(int i)       { return P.getInt("stars." + i, 0); }

    public static int totalStars() {
        int t = 0;
        for (int i = 0; i < Levels.TOTAL; i++) t += stars(i);
        return t;
    }

    public static boolean soundOn()   { return P.getBoolean("set.sound", true); }
    public static boolean musicOn()   { return P.getBoolean("set.music", true); }
    public static String  difficulty(){ return P.get("set.difficulty", "normal"); }

    public static void setSound(boolean v)     { P.putBoolean("set.sound", v); }
    public static void setMusic(boolean v)     { P.putBoolean("set.music", v); }
    public static void setDifficulty(String v) { P.put("set.difficulty", v); }

    /** Keep fewest steps + best stars; unlock the next level. */
    public static void recordWin(int i, int steps, int stars) {
        int b = P.getInt("best." + i, -1);
        if (b < 0 || steps < b) P.putInt("best." + i, steps);
        if (stars > P.getInt("stars." + i, 0)) P.putInt("stars." + i, stars);
        if (i + 2 > unlocked()) P.putInt("unlocked", i + 2);
    }

    public static void reset() {
        try { P.clear(); } catch (Exception ignored) {}
    }

    private Save() {}
}
