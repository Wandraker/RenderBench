package dev.onelsey.renderbench.benchmark;

public enum SpeedPreset {
    VANILLA_FLY("Vanilla fly", "renderbench.speed.vanilla_fly", 10.92),
    VANILLA_SPRINT_FLY("Ctrl sprint-fly", "renderbench.speed.vanilla_sprint_fly", 21.60);

    private final String label;
    private final String translationKey;
    private final double blocksPerSecond;

    SpeedPreset(String label, String translationKey, double blocksPerSecond) {
        this.label = label;
        this.translationKey = translationKey;
        this.blocksPerSecond = blocksPerSecond;
    }

    public String label() {
        return label;
    }

    public String translationKey() {
        return translationKey;
    }

    public double blocksPerSecond() {
        return blocksPerSecond;
    }

    public SpeedPreset next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
