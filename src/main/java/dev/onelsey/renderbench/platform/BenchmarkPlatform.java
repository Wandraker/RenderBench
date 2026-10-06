package dev.onelsey.renderbench.platform;

import dev.onelsey.renderbench.benchmark.BenchmarkRunResult;
import dev.onelsey.renderbench.benchmark.ClientEnvironmentSnapshot;

import java.nio.file.Path;
import java.util.UUID;

public interface BenchmarkPlatform {
    void initialize();

    boolean canBenchmarkCurrentWorld();

    UUID currentPlayerId();

    ClientEnvironmentSnapshot captureEnvironment();

    void openBenchmarkScreen();

    void closeCurrentScreen();

    void presentResult(BenchmarkRunResult result, Path reportDirectory);

    void openPath(Path path) throws java.io.IOException;

    void lockClientRotation(float yaw, float pitch);

    void postMessage(String text);

    void postTranslatedMessage(String translationKey, Object... args);
}
