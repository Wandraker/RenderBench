package dev.onelsey.renderbench.platform;

import java.util.Objects;

public final class PlatformServices {
    private static BenchmarkPlatform platform;

    private PlatformServices() {
    }

    public static synchronized void install(BenchmarkPlatform value) {
        if (platform != null) {
            throw new IllegalStateException("RenderBench platform already installed");
        }
        platform = Objects.requireNonNull(value, "platform");
    }

    public static BenchmarkPlatform get() {
        BenchmarkPlatform value = platform;
        if (value == null) {
            throw new IllegalStateException("RenderBench platform is not installed");
        }
        return value;
    }
}
