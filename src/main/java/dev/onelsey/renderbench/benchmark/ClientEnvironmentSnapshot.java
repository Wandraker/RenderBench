package dev.onelsey.renderbench.benchmark;

import java.nio.file.Path;

public record ClientEnvironmentSnapshot(
        int width,
        int height,
        int renderDistance,
        int simulationDistance,
        int framerateLimit,
        String renderBenchVersion,
        String minecraftVersion,
        String fabricLoaderVersion,
        String fabricApiVersion,
        String javaVersion,
        String osVersion,
        Path gameDirectory
) {
}
