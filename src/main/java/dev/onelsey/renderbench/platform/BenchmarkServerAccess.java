package dev.onelsey.renderbench.platform;

import java.util.UUID;

public interface BenchmarkServerAccess {
    BenchmarkPlayerAccess findPlayer(UUID playerId);
}
