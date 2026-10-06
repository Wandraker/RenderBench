package dev.onelsey.renderbench.benchmark;

import java.util.Arrays;
import java.util.Locale;

public record FrameStats(
        int frames,
        double seconds,
        double averageFps,
        double minimumFps,
        double maximumFps,
        double onePercentLow,
        double pointOnePercentLow,
        double averageFrametimeMs,
        double p95FrametimeMs,
        double p99FrametimeMs,
        double p999FrametimeMs,
        double worstFrametimeMs,
        int over16ms,
        int over33ms,
        int over50ms,
        int over100ms
) {
    public static FrameStats calculate(long[] nanos) {
        if (nanos.length == 0) {
            return new FrameStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        long[] sorted = Arrays.copyOf(nanos, nanos.length);
        Arrays.sort(sorted);

        long sum = 0L;
        int over16 = 0;
        int over33 = 0;
        int over50 = 0;
        int over100 = 0;

        for (long value : nanos) {
            sum += value;
            if (value > 16_666_667L) over16++;
            if (value > 33_333_333L) over33++;
            if (value > 50_000_000L) over50++;
            if (value > 100_000_000L) over100++;
        }

        double seconds = sum / 1_000_000_000.0;
        double avgFps = nanos.length / seconds;
        double minFps = fpsFromNanos(sorted[sorted.length - 1]);
        double maxFps = fpsFromNanos(sorted[0]);
        double oneLow = lowFps(sorted, 0.01);
        double pointOneLow = lowFps(sorted, 0.001);

        return new FrameStats(
                nanos.length,
                seconds,
                avgFps,
                minFps,
                maxFps,
                oneLow,
                pointOneLow,
                (sum / (double) nanos.length) / 1_000_000.0,
                percentile(sorted, 0.95) / 1_000_000.0,
                percentile(sorted, 0.99) / 1_000_000.0,
                percentile(sorted, 0.999) / 1_000_000.0,
                sorted[sorted.length - 1] / 1_000_000.0,
                over16,
                over33,
                over50,
                over100
        );
    }

    private static double fpsFromNanos(long nanos) {
        return nanos <= 0 ? 0.0 : 1_000_000_000.0 / nanos;
    }

    private static double lowFps(long[] sortedAscending, double fraction) {
        int count = Math.max(1, (int) Math.ceil(sortedAscending.length * fraction));
        long sum = 0L;
        for (int i = sortedAscending.length - count; i < sortedAscending.length; i++) {
            sum += sortedAscending[i];
        }
        return fpsFromNanos(Math.round(sum / (double) count));
    }

    private static long percentile(long[] sortedAscending, double percentile) {
        int index = (int) Math.ceil(percentile * sortedAscending.length) - 1;
        index = Math.max(0, Math.min(index, sortedAscending.length - 1));
        return sortedAscending[index];
    }

    public String formattedBlock() {
        return String.format(Locale.ROOT,
                "Frames:                   %d%n" +
                "Measured frame time:      %.3f s%n" +
                "Average framerate:        %.1f FPS%n" +
                "Minimum framerate:        %.1f FPS%n" +
                "Maximum framerate:        %.1f FPS%n" +
                "1%% low framerate:         %.1f FPS%n" +
                "0.1%% low framerate:       %.1f FPS%n" +
                "Average frametime:        %.3f ms%n" +
                "P95 frametime:            %.3f ms%n" +
                "P99 frametime:            %.3f ms%n" +
                "P99.9 frametime:          %.3f ms%n" +
                "Worst frametime:          %.3f ms%n" +
                "Frames > 16.67 ms:        %d%n" +
                "Frames > 33.33 ms:        %d%n" +
                "Frames > 50 ms:           %d%n" +
                "Frames > 100 ms:          %d%n",
                frames, seconds, averageFps, minimumFps, maximumFps,
                onePercentLow, pointOnePercentLow, averageFrametimeMs,
                p95FrametimeMs, p99FrametimeMs, p999FrametimeMs,
                worstFrametimeMs, over16ms, over33ms, over50ms, over100ms);
    }
}
