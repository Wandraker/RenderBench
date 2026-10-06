package dev.onelsey.renderbench.benchmark;

import dev.onelsey.renderbench.benchmark.route.BenchmarkRoute;
import dev.onelsey.renderbench.benchmark.route.RoutePlayback;
import dev.onelsey.renderbench.benchmark.route.RouteRecorder;
import dev.onelsey.renderbench.platform.BenchmarkPlayerAccess;
import dev.onelsey.renderbench.platform.BenchmarkServerAccess;
import dev.onelsey.renderbench.platform.PlatformServices;

import java.nio.file.Path;
import java.util.UUID;

public final class BenchmarkController {
    private static volatile BenchmarkPhase phase = BenchmarkPhase.IDLE;
    private static volatile boolean stopRequested;

    private static BenchmarkConfig pendingConfig;
    private static ClientEnvironmentSnapshot pendingEnvironment;
    private static UUID pendingPlayerId;
    private static volatile boolean pendingStart;

    private static BenchmarkConfig config;
    private static ClientEnvironmentSnapshot environment;
    private static UUID playerId;
    private static String dimensionId;
    private static BenchmarkRoute route;
    private static RoutePlayback playback;
    private static int phaseTicks;

    private static long seed;
    private static long benchmarkStartNanos;
    private static long outboundStartNanos;
    private static long returnStartNanos;
    private static long outboundDurationNanos;
    private static long returnDurationNanos;
    private static volatile Path lastReportDirectory;
    private static volatile BenchmarkRunResult lastResult;

    private BenchmarkController() {
    }

    public static synchronized boolean requestStart(BenchmarkConfig newConfig, ClientEnvironmentSnapshot newEnvironment, UUID newPlayerId) {
        if (isRunning() || pendingStart || newPlayerId == null) return false;
        pendingConfig = newConfig;
        pendingEnvironment = newEnvironment;
        pendingPlayerId = newPlayerId;
        pendingStart = true;
        stopRequested = false;
        return true;
    }

    public static void requestStop() {
        if (pendingStart) {
            synchronized (BenchmarkController.class) {
                pendingStart = false;
                pendingConfig = null;
                pendingEnvironment = null;
                pendingPlayerId = null;
                phase = BenchmarkPhase.ABORTED;
            }
            PlatformServices.get().postTranslatedMessage("renderbench.message.stopped");
            return;
        }
        if (isRunning()) stopRequested = true;
    }

    public static void onServerTick(BenchmarkServerAccess server) {
        if (pendingStart && !isRunning()) {
            initialize(server);
        }

        if (!isRunning()) return;

        BenchmarkPlayerAccess player = server.findPlayer(playerId);
        if (player == null || !player.dimensionId().equals(dimensionId)) {
            abort(player, "renderbench.message.stopped_player_dimension_changed");
            return;
        }

        if (stopRequested) {
            abort(player, "renderbench.message.stopped_by_user");
            return;
        }

        switch (phase) {
            case WARMUP -> tickWarmup(player);
            case OUTBOUND -> tickOutbound(player);
            case TURN_PAUSE -> tickTurnPause(player);
            case RETURN -> tickReturn(player);
            default -> {
            }
        }
    }

    private static synchronized void initialize(BenchmarkServerAccess server) {
        if (!pendingStart || pendingConfig == null || pendingPlayerId == null) return;

        BenchmarkPlayerAccess player = server.findPlayer(pendingPlayerId);
        if (player == null) {
            pendingStart = false;
            phase = BenchmarkPhase.ABORTED;
            PlatformServices.get().postTranslatedMessage("renderbench.message.player_not_found");
            return;
        }

        config = pendingConfig;
        environment = pendingEnvironment;
        playerId = pendingPlayerId;
        pendingConfig = null;
        pendingEnvironment = null;
        pendingPlayerId = null;
        pendingStart = false;

        dimensionId = player.dimensionId();
        seed = player.seed();
        route = RouteRecorder.captureStraightRoute(
                config,
                player.x(),
                player.y(),
                player.z(),
                player.yaw(),
                player.pitch()
        );
        playback = new RoutePlayback(route);

        phaseTicks = 0;
        outboundDurationNanos = 0L;
        returnDurationNanos = 0L;
        benchmarkStartNanos = System.nanoTime();
        lastReportDirectory = null;
        lastResult = null;
        FrameRecorder.reset();

        move(player, playback.x(), playback.y(), playback.z());
        phase = config.warmupSeconds() > 0 ? BenchmarkPhase.WARMUP : BenchmarkPhase.OUTBOUND;
        if (phase == BenchmarkPhase.OUTBOUND) outboundStartNanos = System.nanoTime();
        PlatformServices.get().postTranslatedMessage(
                "renderbench.message.started",
                String.format(java.util.Locale.ROOT, "%.0f", config.distanceBlocks()),
                String.format(java.util.Locale.ROOT, "%.2f", config.effectiveSpeedBlocksPerSecond())
        );
    }

    private static void tickWarmup(BenchmarkPlayerAccess player) {
        playback.resetToStart();
        move(player, playback.x(), playback.y(), playback.z());
        phaseTicks++;
        if (phaseTicks >= config.warmupSeconds() * 20) {
            phaseTicks = 0;
            playback.resetToStart();
            phase = BenchmarkPhase.OUTBOUND;
            outboundStartNanos = System.nanoTime();
        }
    }

    private static void tickOutbound(BenchmarkPlayerAccess player) {
        playback.advance(config.effectiveSpeedBlocksPerSecond() / 20.0);
        move(player, playback.x(), playback.y(), playback.z());

        if (playback.atEnd()) {
            outboundDurationNanos = System.nanoTime() - outboundStartNanos;
            phaseTicks = 0;
            if (config.turnPauseSeconds() > 0) {
                phase = BenchmarkPhase.TURN_PAUSE;
            } else {
                phase = BenchmarkPhase.RETURN;
                returnStartNanos = System.nanoTime();
            }
        }
    }

    private static void tickTurnPause(BenchmarkPlayerAccess player) {
        playback.resetToEnd();
        move(player, playback.x(), playback.y(), playback.z());
        phaseTicks++;
        if (phaseTicks >= config.turnPauseSeconds() * 20) {
            phaseTicks = 0;
            phase = BenchmarkPhase.RETURN;
            returnStartNanos = System.nanoTime();
        }
    }

    private static void tickReturn(BenchmarkPlayerAccess player) {
        playback.rewind(config.effectiveSpeedBlocksPerSecond() / 20.0);
        move(player, playback.x(), playback.y(), playback.z());

        if (playback.atStart()) {
            returnDurationNanos = System.nanoTime() - returnStartNanos;
            player.moveTo(route.startX(), route.startY(), route.startZ(), route.yaw(), route.pitch());
            finish();
        }
    }

    private static void finish() {
        long totalDuration = System.nanoTime() - benchmarkStartNanos;
        phase = BenchmarkPhase.DONE;
        stopRequested = false;

        BenchmarkRunResult result = new BenchmarkRunResult(
                seed,
                config,
                environment,
                route,
                totalDuration,
                outboundDurationNanos,
                returnDurationNanos,
                FrameRecorder.outboundCopy(),
                FrameRecorder.returnCopy()
        );

        lastResult = result;
        try {
            lastReportDirectory = BenchmarkReportWriter.write(result);
            PlatformServices.get().postTranslatedMessage("renderbench.message.complete", lastReportDirectory.toAbsolutePath().toString());
            PlatformServices.get().presentResult(result, lastReportDirectory);
        } catch (Exception exception) {
            PlatformServices.get().postTranslatedMessage("renderbench.message.report_failed", exception.getMessage());
        }
    }

    private static void abort(BenchmarkPlayerAccess player, String messageKey) {
        if (player != null && route != null) {
            player.moveTo(route.startX(), route.startY(), route.startZ(), route.yaw(), route.pitch());
        }
        phase = BenchmarkPhase.ABORTED;
        stopRequested = false;
        FrameRecorder.reset();
        PlatformServices.get().postTranslatedMessage(messageKey);
    }

    private static void move(BenchmarkPlayerAccess player, double x, double y, double z) {
        player.moveTo(x, y, z, route.yaw(), route.pitch());
    }

    public static BenchmarkPhase phase() {
        return phase;
    }

    public static boolean isRunning() {
        return switch (phase) {
            case WARMUP, OUTBOUND, TURN_PAUSE, RETURN -> true;
            default -> false;
        };
    }

    public static float routeYaw() {
        BenchmarkRoute current = route;
        return current == null ? 0.0f : current.yaw();
    }

    public static float routePitch() {
        BenchmarkRoute current = route;
        return current == null ? 0.0f : current.pitch();
    }

    public static Path lastReportDirectory() {
        return lastReportDirectory;
    }

    public static BenchmarkRunResult lastResult() {
        return lastResult;
    }


    public static String statusTranslationKey() {
        if (pendingStart) return "renderbench.status.starting";
        return switch (phase) {
            case IDLE -> "renderbench.status.idle";
            case WARMUP -> "renderbench.status.warmup";
            case OUTBOUND -> "renderbench.status.outbound";
            case TURN_PAUSE -> "renderbench.status.turn_pause";
            case RETURN -> "renderbench.status.return";
            case DONE -> "renderbench.status.done";
            case ABORTED -> "renderbench.status.stopped";
        };
    }

    public static String statusText() {
        if (pendingStart) return "STARTING";
        return switch (phase) {
            case IDLE -> "IDLE";
            case WARMUP -> "WARMUP";
            case OUTBOUND -> "OUTBOUND / NEW CHUNKS";
            case TURN_PAUSE -> "TURN PAUSE";
            case RETURN -> "RETURN / GENERATED CHUNKS";
            case DONE -> "DONE";
            case ABORTED -> "STOPPED";
        };
    }
}
