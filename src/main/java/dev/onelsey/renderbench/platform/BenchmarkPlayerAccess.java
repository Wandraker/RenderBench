package dev.onelsey.renderbench.platform;

public interface BenchmarkPlayerAccess {
    String dimensionId();

    long seed();

    double x();

    double y();

    double z();

    float yaw();

    float pitch();

    void moveTo(double x, double y, double z, float yaw, float pitch);
}
