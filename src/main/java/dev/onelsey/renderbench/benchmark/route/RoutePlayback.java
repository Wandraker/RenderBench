package dev.onelsey.renderbench.benchmark.route;

public final class RoutePlayback {
    private final BenchmarkRoute route;
    private double distance;

    public RoutePlayback(BenchmarkRoute route) {
        this.route = route;
    }

    public void resetToStart() {
        distance = 0.0;
    }

    public void resetToEnd() {
        distance = route.distanceBlocks();
    }

    public void advance(double blocks) {
        distance = Math.min(route.distanceBlocks(), distance + Math.max(0.0, blocks));
    }

    public void rewind(double blocks) {
        distance = Math.max(0.0, distance - Math.max(0.0, blocks));
    }

    public boolean atStart() {
        return distance <= 0.000001;
    }

    public boolean atEnd() {
        return distance >= route.distanceBlocks() - 0.000001;
    }

    public double distance() {
        return distance;
    }

    public double x() {
        return route.xAt(distance);
    }

    public double y() {
        return route.routeY();
    }

    public double z() {
        return route.zAt(distance);
    }
}
