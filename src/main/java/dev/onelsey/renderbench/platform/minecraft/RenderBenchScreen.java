package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.benchmark.BenchmarkConfig;
import dev.onelsey.renderbench.benchmark.BenchmarkController;
import dev.onelsey.renderbench.benchmark.Direction;
import dev.onelsey.renderbench.benchmark.SpeedPreset;
import dev.onelsey.renderbench.platform.BenchmarkPlatform;
import dev.onelsey.renderbench.platform.PlatformServices;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class RenderBenchScreen extends Screen {
    private static final int ROW_HEIGHT = 28;
    private static final int CONTROL_WIDTH = 205;

    private EditBox distanceBox;
    private EditBox customSpeedBox;
    private EditBox yBox;
    private EditBox warmupBox;
    private EditBox pauseBox;
    private Button speedPresetButton;
    private Button directionButton;
    private SpeedPreset speedPreset;
    private Direction direction;
    private Component error;
    private boolean infoMessage;

    public RenderBenchScreen() {
        super(Component.literal("RenderBench"));
        RenderBenchSettings settings = RenderBenchSettingsStore.load();
        this.speedPreset = settings.speedPreset();
        this.direction = settings.direction();
    }

    @Override
    protected void init() {
        RenderBenchSettings settings = RenderBenchSettingsStore.load();
        this.speedPreset = settings.speedPreset();
        this.direction = settings.direction();

        int center = this.width / 2;
        int controlX = center + 15;
        int top = 48;

        distanceBox = addBox(controlX, top, CONTROL_WIDTH, settings.distance(), "renderbench.label.distance");

        speedPresetButton = this.addRenderableWidget(Button.builder(presetText(), button -> {
            speedPreset = speedPreset.next();
            button.setMessage(presetText());
        }).bounds(controlX, top + ROW_HEIGHT, CONTROL_WIDTH, 20).build());

        customSpeedBox = addBox(controlX, top + ROW_HEIGHT * 2, CONTROL_WIDTH, settings.customSpeed(), "renderbench.label.custom_speed");

        directionButton = this.addRenderableWidget(Button.builder(directionText(), button -> {
            direction = direction.next();
            button.setMessage(directionText());
        }).bounds(controlX, top + ROW_HEIGHT * 3, CONTROL_WIDTH, 20).build());

        yBox = addBox(controlX, top + ROW_HEIGHT * 4, CONTROL_WIDTH, settings.y(), "renderbench.label.y");
        warmupBox = addBox(controlX, top + ROW_HEIGHT * 5, CONTROL_WIDTH, settings.warmup(), "renderbench.label.warmup");
        pauseBox = addBox(controlX, top + ROW_HEIGHT * 6, CONTROL_WIDTH, settings.turnPause(), "renderbench.label.turn_pause");

        Component startLabel = BenchmarkController.isRunning()
                ? Component.translatable("renderbench.button.stop")
                : Component.translatable("renderbench.button.start");

        int buttonY = top + ROW_HEIGHT * 7 + 8;
        this.addRenderableWidget(Button.builder(startLabel, button -> {
            if (BenchmarkController.isRunning()) {
                BenchmarkController.requestStop();
                PlatformServices.get().closeCurrentScreen();
                return;
            }
            startBenchmark();
        }).bounds(center - 200, buttonY, 125, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("renderbench.button.reset_settings"), button -> resetSettings())
                .bounds(center - 62, buttonY, 125, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("renderbench.button.close"), button -> saveThenClose())
                .bounds(center + 76, buttonY, 125, 20).build());
    }

    private EditBox addBox(int x, int y, int width, String value, String narrationKey) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.translatable(narrationKey));
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private Component presetText() {
        return Component.translatable(
                "renderbench.speed.value",
                Component.translatable(speedPreset.translationKey()),
                String.format(Locale.ROOT, "%.2f", speedPreset.blocksPerSecond())
        );
    }

    private Component directionText() {
        String key = direction.translationKey();
        return key == null ? Component.literal(direction.label()) : Component.translatable(key);
    }

    private void startBenchmark() {
        BenchmarkPlatform platform = PlatformServices.get();
        if (!platform.canBenchmarkCurrentWorld()) {
            error = Component.translatable("renderbench.error.singleplayer_required");
            infoMessage = false;
            return;
        }

        try {
            double distance = parseDouble(distanceBox, "renderbench.label.distance", 16.0, 100000.0);
            double customSpeed = parseDouble(customSpeedBox, "renderbench.label.custom_speed", 0.0, 1000.0);
            double y = parseDouble(yBox, "renderbench.label.y", -4096.0, 4096.0);
            int warmup = parseInt(warmupBox, "renderbench.label.warmup", 0, 300);
            int pause = parseInt(pauseBox, "renderbench.label.turn_pause", 0, 120);

            BenchmarkConfig config = new BenchmarkConfig(
                    distance,
                    speedPreset,
                    customSpeed,
                    direction,
                    y,
                    warmup,
                    pause
            );

            if (!persistCurrentSettings()) {
                error = Component.translatable("renderbench.error.settings_save_failed");
                infoMessage = false;
                return;
            }

            if (!BenchmarkController.requestStart(config, platform.captureEnvironment(), platform.currentPlayerId())) {
                error = Component.translatable("renderbench.error.already_running");
                infoMessage = false;
                return;
            }

            error = null;
            infoMessage = false;
            platform.closeCurrentScreen();
        } catch (ValidationException exception) {
            error = exception.component();
            infoMessage = false;
        }
    }

    private void resetSettings() {
        RenderBenchSettings defaults = RenderBenchSettingsStore.reset();
        distanceBox.setValue(defaults.distance());
        customSpeedBox.setValue(defaults.customSpeed());
        yBox.setValue(defaults.y());
        warmupBox.setValue(defaults.warmup());
        pauseBox.setValue(defaults.turnPause());
        speedPreset = defaults.speedPreset();
        direction = defaults.direction();
        speedPresetButton.setMessage(presetText());
        directionButton.setMessage(directionText());
        error = Component.translatable("renderbench.message.settings_reset");
        infoMessage = true;
    }

    private void saveThenClose() {
        persistCurrentSettings();
        PlatformServices.get().closeCurrentScreen();
    }

    @Override
    public void onClose() {
        persistCurrentSettings();
        super.onClose();
    }

    @Override
    public void removed() {
        persistCurrentSettings();
        super.removed();
    }

    private boolean persistCurrentSettings() {
        if (distanceBox == null || customSpeedBox == null || yBox == null || warmupBox == null || pauseBox == null) {
            return true;
        }
        return RenderBenchSettingsStore.save(new RenderBenchSettings(
                distanceBox.getValue().trim(),
                speedPreset,
                customSpeedBox.getValue().trim(),
                direction,
                yBox.getValue().trim(),
                warmupBox.getValue().trim(),
                pauseBox.getValue().trim()
        ));
    }

    private static double parseDouble(EditBox box, String labelKey, double min, double max) {
        double value;
        try {
            value = Double.parseDouble(box.getValue().trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new ValidationException(Component.translatable(
                    "renderbench.error.invalid_number",
                    Component.translatable(labelKey)
            ));
        }
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new ValidationException(Component.translatable(
                    "renderbench.error.allowed_range",
                    Component.translatable(labelKey),
                    formatLimit(min),
                    formatLimit(max)
            ));
        }
        return value;
    }

    private static int parseInt(EditBox box, String labelKey, int min, int max) {
        int value;
        try {
            value = Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException(Component.translatable(
                    "renderbench.error.invalid_integer",
                    Component.translatable(labelKey)
            ));
        }
        if (value < min || value > max) {
            throw new ValidationException(Component.translatable(
                    "renderbench.error.allowed_range",
                    Component.translatable(labelKey),
                    Integer.toString(min),
                    Integer.toString(max)
            ));
        }
        return value;
    }

    private static String formatLimit(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int center = this.width / 2;
        int labelX = center - 215;
        int top = 48;

        drawCentered(graphics, Component.translatable(
                "renderbench.screen.title_status",
                Component.translatable(BenchmarkController.statusTranslationKey())
        ), 18, 0xFFFFFFFF);

        drawLabel(graphics, "renderbench.label.distance", labelX, top + 6);
        drawLabel(graphics, "renderbench.label.speed_preset", labelX, top + ROW_HEIGHT + 6);
        drawLabel(graphics, "renderbench.label.custom_speed", labelX, top + ROW_HEIGHT * 2 + 6);
        drawLabel(graphics, "renderbench.label.direction", labelX, top + ROW_HEIGHT * 3 + 6);
        drawLabel(graphics, "renderbench.label.y", labelX, top + ROW_HEIGHT * 4 + 6);
        drawLabel(graphics, "renderbench.label.warmup", labelX, top + ROW_HEIGHT * 5 + 6);
        drawLabel(graphics, "renderbench.label.turn_pause", labelX, top + ROW_HEIGHT * 6 + 6);

        int footerY = top + ROW_HEIGHT * 7 + 35;
        if (error != null) {
            drawCentered(graphics, error, footerY, infoMessage ? 0xFF55FF55 : 0xFFFF5555);
        } else {
            drawCentered(graphics, Component.translatable("renderbench.hint"), footerY, 0xFFAAAAAA);
        }
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, int x, int y) {
        graphics.text(this.font, Component.translatable(key).getString(), x, y, 0xFFFFFFFF, true);
    }

    private void drawCentered(GuiGraphicsExtractor graphics, Component component, int y, int color) {
        String text = component.getString();
        int x = Math.max(8, (this.width - this.font.width(text)) / 2);
        graphics.text(this.font, text, x, y, color, true);
    }

    private static final class ValidationException extends IllegalArgumentException {
        private final Component component;

        private ValidationException(Component component) {
            super(component.getString());
            this.component = component;
        }

        private Component component() {
            return component;
        }
    }
}
