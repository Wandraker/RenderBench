package dev.onelsey.renderbench.benchmark.route;

public record BenchmarkRoute(
        double startX,
        double startY,
        double startZ,
        double routeY,
        double directionX,
        double directionZ,
        float yaw,
        float pitch,
        double distanceBlocks
) {
    public double xAt(double distance) {
        return startX + directionX * clampDistance(distance);
    }

    public double zAt(double distance) {
        return startZ + directionZ * clampDistance(distance);
    }

    public double endX() {
        return xAt(distanceBlocks);
    }

    public double endZ() {
        return zAt(distanceBlocks);
    }

    private double clampDistance(double distance) {
        return Math.max(0.0, Math.min(distanceBlocks, distance));
    }
}
