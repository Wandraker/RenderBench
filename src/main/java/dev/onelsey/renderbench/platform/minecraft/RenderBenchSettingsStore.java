package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.benchmark.Direction;
import dev.onelsey.renderbench.benchmark.SpeedPreset;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class RenderBenchSettingsStore {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("renderbench.properties");

    private RenderBenchSettingsStore() {
    }

    public static RenderBenchSettings load() {
        RenderBenchSettings defaults = RenderBenchSettings.defaults();
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return defaults;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            return defaults;
        }

        return new RenderBenchSettings(
                value(properties, "distance", defaults.distance()),
                enumValue(properties, "speedPreset", SpeedPreset.class, defaults.speedPreset()),
                value(properties, "customSpeed", defaults.customSpeed()),
                enumValue(properties, "direction", Direction.class, defaults.direction()),
                value(properties, "y", defaults.y()),
                value(properties, "warmup", defaults.warmup()),
                value(properties, "turnPause", defaults.turnPause())
        );
    }

    public static boolean save(RenderBenchSettings settings) {
        Properties properties = new Properties();
        properties.setProperty("distance", settings.distance());
        properties.setProperty("speedPreset", settings.speedPreset().name());
        properties.setProperty("customSpeed", settings.customSpeed());
        properties.setProperty("direction", settings.direction().name());
        properties.setProperty("y", settings.y());
        properties.setProperty("warmup", settings.warmup());
        properties.setProperty("turnPause", settings.turnPause());

        Path parent = CONFIG_PATH.getParent();
        Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "RenderBench settings");
            }
            try {
                Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
            return false;
        }
    }

    public static RenderBenchSettings reset() {
        RenderBenchSettings defaults = RenderBenchSettings.defaults();
        save(defaults);
        return defaults;
    }

    public static Path path() {
        return CONFIG_PATH;
    }

    private static String value(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static <E extends Enum<E>> E enumValue(Properties properties, String key, Class<E> type, E fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
