package dev.onelsey.renderbench.benchmark;

public enum BenchmarkPhase {
    IDLE,
    WARMUP,
    OUTBOUND,
    TURN_PAUSE,
    RETURN,
    DONE,
    ABORTED
}
