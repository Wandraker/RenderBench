package dev.onelsey.renderbench.benchmark;

public enum Direction {
    POS_X("+X", null, 1.0, 0.0, -90.0f),
    NEG_X("-X", null, -1.0, 0.0, 90.0f),
    POS_Z("+Z", null, 0.0, 1.0, 0.0f),
    NEG_Z("-Z", null, 0.0, -1.0, 180.0f),
    LOOK("Current look", "renderbench.direction.current_look", 0.0, 0.0, Float.NaN);

    private final String label;
    private final String translationKey;
    private final double x;
    private final double z;
    private final float yaw;

    Direction(String label, String translationKey, double x, double z, float yaw) {
        this.label = label;
        this.translationKey = translationKey;
        this.x = x;
        this.z = z;
        this.yaw = yaw;
    }

    public String label() {
        return label;
    }

    public String translationKey() {
        return translationKey;
    }

    public Direction next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public double x(float currentYaw) {
        if (this != LOOK) return x;
        double radians = Math.toRadians(currentYaw);
        return -Math.sin(radians);
    }

    public double z(float currentYaw) {
        if (this != LOOK) return z;
        double radians = Math.toRadians(currentYaw);
        return Math.cos(radians);
    }

    public float yaw(float currentYaw) {
        return this == LOOK ? currentYaw : yaw;
    }
}
