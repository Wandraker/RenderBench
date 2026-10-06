package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.benchmark.Direction;
import dev.onelsey.renderbench.benchmark.SpeedPreset;

public record RenderBenchSettings(
        String distance,
        SpeedPreset speedPreset,
        String customSpeed,
        Direction direction,
        String y,
        String warmup,
        String turnPause
) {
    public static RenderBenchSettings defaults() {
        return new RenderBenchSettings(
                "256",
                SpeedPreset.VANILLA_FLY,
                "0",
                Direction.POS_X,
                "0",
                "5",
                "2"
        );
    }
}
