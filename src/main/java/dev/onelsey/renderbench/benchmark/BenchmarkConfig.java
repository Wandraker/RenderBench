package dev.onelsey.renderbench.benchmark;

public record BenchmarkConfig(
        double distanceBlocks,
        SpeedPreset speedPreset,
        double customSpeedBlocksPerSecond,
        Direction direction,
        double customY,
        int warmupSeconds,
        int turnPauseSeconds
) {
    public double effectiveSpeedBlocksPerSecond() {
        return customSpeedBlocksPerSecond > 0.0
                ? customSpeedBlocksPerSecond
                : speedPreset.blocksPerSecond();
    }

    public boolean useCurrentY() {
        return customY == 0.0;
    }
}
