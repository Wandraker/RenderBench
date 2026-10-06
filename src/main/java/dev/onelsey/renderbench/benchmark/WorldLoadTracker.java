package dev.onelsey.renderbench.benchmark;

public final class WorldLoadTracker {
    private static volatile long loadStartNanos;
    private static volatile long loadLevelEndNanos;
    private static volatile long firstWorldFrameNanos;

    private WorldLoadTracker() {
    }

    public static void onLoadLevelStart() {
        loadStartNanos = System.nanoTime();
        loadLevelEndNanos = 0L;
        firstWorldFrameNanos = 0L;
    }

    public static void onLoadLevelEnd() {
        if (loadStartNanos != 0L) {
            loadLevelEndNanos = System.nanoTime();
        }
    }

    public static void onWorldFrame() {
        if (loadStartNanos != 0L && loadLevelEndNanos != 0L && firstWorldFrameNanos == 0L) {
            firstWorldFrameNanos = System.nanoTime();
        }
    }

    public static double loadLevelMillis() {
        if (loadStartNanos == 0L || loadLevelEndNanos == 0L) return -1.0;
        return (loadLevelEndNanos - loadStartNanos) / 1_000_000.0;
    }

    public static double firstWorldFrameMillis() {
        if (loadStartNanos == 0L || firstWorldFrameNanos == 0L) return -1.0;
        return (firstWorldFrameNanos - loadStartNanos) / 1_000_000.0;
    }
}
