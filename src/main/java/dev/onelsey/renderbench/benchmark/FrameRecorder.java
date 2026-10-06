package dev.onelsey.renderbench.benchmark;

public final class FrameRecorder {
    private static final FrameSeries OUTBOUND = new FrameSeries();
    private static final FrameSeries RETURN = new FrameSeries();

    private static BenchmarkPhase lastPhase = BenchmarkPhase.IDLE;
    private static long previousFrameStart;

    private FrameRecorder() {
    }

    public static synchronized void reset() {
        OUTBOUND.clear();
        RETURN.clear();
        lastPhase = BenchmarkPhase.IDLE;
        previousFrameStart = 0L;
    }

    public static synchronized void onFrameStart(long nowNanos) {
        BenchmarkPhase phase = BenchmarkController.phase();
        if (phase != lastPhase) {
            lastPhase = phase;
            previousFrameStart = nowNanos;
            return;
        }

        if (phase != BenchmarkPhase.OUTBOUND && phase != BenchmarkPhase.RETURN) {
            previousFrameStart = nowNanos;
            return;
        }

        if (previousFrameStart != 0L) {
            long delta = nowNanos - previousFrameStart;
            if (phase == BenchmarkPhase.OUTBOUND) {
                OUTBOUND.add(delta);
            } else {
                RETURN.add(delta);
            }
        }
        previousFrameStart = nowNanos;
    }

    public static synchronized long[] outboundCopy() {
        return OUTBOUND.copy();
    }

    public static synchronized long[] returnCopy() {
        return RETURN.copy();
    }
}
