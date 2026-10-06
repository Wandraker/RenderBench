package dev.onelsey.renderbench.benchmark;

import dev.onelsey.renderbench.benchmark.route.BenchmarkRoute;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BenchmarkReportWriter {
    private static final DateTimeFormatter DIR_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private BenchmarkReportWriter() {
    }

    public static Path write(BenchmarkRunResult result) throws IOException {
        Path directory = result.environment().gameDirectory()
                .resolve("renderbench")
                .resolve(LocalDateTime.now().format(DIR_TIME));
        Files.createDirectories(directory);

        FrameStats outbound = FrameStats.calculate(result.outboundFrames());
        FrameStats returning = FrameStats.calculate(result.returnFrames());

        Files.writeString(directory.resolve("summary.txt"), summary(result, outbound, returning), StandardCharsets.UTF_8);
        writeCsv(directory.resolve("frames-outbound.csv"), result.outboundFrames());
        writeCsv(directory.resolve("frames-return.csv"), result.returnFrames());
        Files.writeString(directory.resolve("environment.txt"), environment(result), StandardCharsets.UTF_8);
        return directory;
    }

    private static String summary(BenchmarkRunResult result, FrameStats outbound, FrameStats returning) {
        BenchmarkConfig config = result.config();
        ClientEnvironmentSnapshot env = result.environment();
        BenchmarkRoute route = result.route();
        double requestedSpeed = config.effectiveSpeedBlocksPerSecond();
        double outboundSeconds = result.outboundDurationNanos() / 1_000_000_000.0;
        double returnSeconds = result.returnDurationNanos() / 1_000_000_000.0;
        double totalSeconds = result.totalDurationNanos() / 1_000_000_000.0;
        String speedSource = config.customSpeedBlocksPerSecond() > 0.0
                ? String.format(Locale.ROOT, "Custom %.3f blocks/s", config.customSpeedBlocksPerSecond())
                : String.format(Locale.ROOT, "%s %.2f blocks/s", config.speedPreset().label(), config.speedPreset().blocksPerSecond());

        return String.format(Locale.ROOT,
                "RenderBench %s%n" +
                "Minecraft:                %s%n" +
                "Fabric Loader:            %s%n" +
                "Fabric API:               %s%n" +
                "Seed:                     %d%n" +
                "Resolution:               %dx%d%n" +
                "Render distance:          %d chunks%n" +
                "Simulation distance:      %d chunks%n" +
                "Framerate limit:          %d%n" +
                "%n" +
                "ROUTE%n" +
                "Start:                    %.3f / %.3f / %.3f%n" +
                "Route Y:                  %.3f%n" +
                "Direction:                %s%n" +
                "Direction vector:         %.6f / %.6f%n" +
                "Camera yaw/pitch:         %.2f / %.2f%n" +
                "Distance:                 %.1f blocks each way%n" +
                "Speed profile:            %s%n" +
                "Requested speed:          %.3f blocks/s%n" +
                "Warmup:                   %d s%n" +
                "Turn pause:               %d s%n" +
                "%n" +
                "WORLD LOAD%n" +
                "MinecraftServer.loadLevel: %s%n" +
                "Load start -> world frame: %s%n" +
                "%n" +
                "OUTBOUND / NEW-CHUNK ROUTE%n" +
                "Route wall time:          %.3f s%n" +
                "Achieved route speed:     %.3f blocks/s%n" +
                "%s" +
                "%n" +
                "RETURN / GENERATED ROUTE%n" +
                "Route wall time:          %.3f s%n" +
                "Achieved route speed:     %.3f blocks/s%n" +
                "%s" +
                "%n" +
                "TOTAL BENCHMARK%n" +
                "Wall time:                %.3f s%n",
                env.renderBenchVersion(),
                env.minecraftVersion(),
                env.fabricLoaderVersion(),
                env.fabricApiVersion(),
                result.seed(),
                env.width(), env.height(),
                env.renderDistance(),
                env.simulationDistance(),
                env.framerateLimit(),
                route.startX(), route.startY(), route.startZ(),
                route.routeY(),
                config.direction().label(),
                route.directionX(), route.directionZ(),
                route.yaw(), route.pitch(),
                route.distanceBlocks(),
                speedSource,
                requestedSpeed,
                config.warmupSeconds(),
                config.turnPauseSeconds(),
                formatOptionalMillis(WorldLoadTracker.loadLevelMillis()),
                formatOptionalMillis(WorldLoadTracker.firstWorldFrameMillis()),
                outboundSeconds,
                outboundSeconds > 0 ? route.distanceBlocks() / outboundSeconds : 0.0,
                outbound.formattedBlock(),
                returnSeconds,
                returnSeconds > 0 ? route.distanceBlocks() / returnSeconds : 0.0,
                returning.formattedBlock(),
                totalSeconds
        );
    }

    private static String environment(BenchmarkRunResult result) {
        BenchmarkConfig config = result.config();
        ClientEnvironmentSnapshot env = result.environment();
        BenchmarkRoute route = result.route();
        return String.format(Locale.ROOT,
                "renderbench=%s%n" +
                "platform_adapter=minecraft%n" +
                "minecraft=%s%n" +
                "fabric_loader=%s%n" +
                "fabric_api=%s%n" +
                "java=%s%n" +
                "os=%s%n" +
                "resolution=%dx%d%n" +
                "render_distance=%d%n" +
                "simulation_distance=%d%n" +
                "framerate_limit=%d%n" +
                "seed=%d%n" +
                "distance=%.3f%n" +
                "speed=%.3f%n" +
                "speed_preset=%s%n" +
                "custom_speed=%.3f%n" +
                "direction=%s%n" +
                "direction_x=%.9f%n" +
                "direction_z=%.9f%n",
                env.renderBenchVersion(),
                env.minecraftVersion(), env.fabricLoaderVersion(), env.fabricApiVersion(),
                env.javaVersion(), env.osVersion(),
                env.width(), env.height(),
                env.renderDistance(), env.simulationDistance(),
                env.framerateLimit(), result.seed(), route.distanceBlocks(),
                config.effectiveSpeedBlocksPerSecond(), config.speedPreset().name(),
                config.customSpeedBlocksPerSecond(), config.direction().name(),
                route.directionX(), route.directionZ());
    }

    private static String formatOptionalMillis(double value) {
        if (value < 0) return "not captured";
        return String.format(Locale.ROOT, "%.3f ms", value);
    }

    private static void writeCsv(Path path, long[] nanos) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("frame,frametime_ms,instant_fps\n");
            for (int i = 0; i < nanos.length; i++) {
                double ms = nanos[i] / 1_000_000.0;
                double fps = nanos[i] <= 0 ? 0.0 : 1_000_000_000.0 / nanos[i];
                writer.write(String.format(Locale.ROOT, "%d,%.6f,%.3f%n", i + 1, ms, fps));
            }
        }
    }
}
