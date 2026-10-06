package dev.onelsey.renderbench.benchmark;

import dev.onelsey.renderbench.benchmark.route.BenchmarkRoute;

public record BenchmarkRunResult(
        long seed,
        BenchmarkConfig config,
        ClientEnvironmentSnapshot environment,
        BenchmarkRoute route,
        long totalDurationNanos,
        long outboundDurationNanos,
        long returnDurationNanos,
        long[] outboundFrames,
        long[] returnFrames
) {
}
