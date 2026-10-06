package dev.onelsey.renderbench.benchmark.route;

import dev.onelsey.renderbench.benchmark.BenchmarkConfig;

public final class RouteRecorder {
    private RouteRecorder() {
    }

    public static BenchmarkRoute captureStraightRoute(
            BenchmarkConfig config,
            double startX,
            double startY,
            double startZ,
            float playerYaw,
            float playerPitch
    ) {
        double directionX = config.direction().x(playerYaw);
        double directionZ = config.direction().z(playerYaw);
        double length = Math.sqrt(directionX * directionX + directionZ * directionZ);

        if (length <= 0.000001) {
            directionX = 1.0;
            directionZ = 0.0;
        } else {
            directionX /= length;
            directionZ /= length;
        }

        double routeY = config.useCurrentY() ? startY : config.customY();
        float routeYaw = config.direction().yaw(playerYaw);

        return new BenchmarkRoute(
                startX,
                startY,
                startZ,
                routeY,
                directionX,
                directionZ,
                routeYaw,
                playerPitch,
                config.distanceBlocks()
        );
    }
}
