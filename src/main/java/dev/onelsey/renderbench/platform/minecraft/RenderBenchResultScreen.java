package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.benchmark.BenchmarkRunResult;
import dev.onelsey.renderbench.benchmark.FrameStats;
import dev.onelsey.renderbench.benchmark.WorldLoadTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import dev.onelsey.renderbench.platform.PlatformServices;

import java.nio.file.Path;
import java.util.Locale;

public final class RenderBenchResultScreen extends Screen {
    private final BenchmarkRunResult result;
    private final Path reportDirectory;
    private final FrameStats outbound;
    private final FrameStats returning;
    private Component status;
    private int statusColor = 0xFFAAAAAA;

    public RenderBenchResultScreen(BenchmarkRunResult result, Path reportDirectory) {
        super(Component.translatable("renderbench.result.title"));
        this.result = result;
        this.reportDirectory = reportDirectory;
        this.outbound = FrameStats.calculate(result.outboundFrames());
        this.returning = FrameStats.calculate(result.returnFrames());
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int buttonWidth = 150;
        int gap = 8;
        int rowWidth = buttonWidth * 2 + gap;
        int x = center - rowWidth / 2;
        int y = Math.max(330, this.height - 52);

        this.addRenderableWidget(Button.builder(Component.translatable("renderbench.button.save_card"), button -> saveCard())
                .bounds(x, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("renderbench.button.open_results"), button -> openResultsFolder())
                .bounds(x + buttonWidth + gap, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("renderbench.button.new_test"), button -> {
            this.minecraft.gui.setScreen(new RenderBenchScreen());
        }).bounds(x, y + 24, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("renderbench.button.close"), button -> onClose())
                .bounds(x + buttonWidth + gap, y + 24, buttonWidth, 20).build());
    }

    private void openResultsFolder() {
        try {
            PlatformServices.get().openPath(reportDirectory);
        } catch (Exception exception) {
            status = Component.translatable("renderbench.result.folder_open_failed", exception.getMessage());
            statusColor = 0xFFFF5555;
        }
    }

    private void saveCard() {
        try {
            Path saved = ResultCardExporter.write(result, reportDirectory);
            status = Component.translatable("renderbench.result.card_saved", saved.getFileName().toString());
            statusColor = 0xFF55FF55;
        } catch (Exception exception) {
            status = Component.translatable("renderbench.result.card_save_failed", exception.getMessage());
            statusColor = 0xFFFF5555;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF15243A, 0xFF0B111A);
        drawVoxelBackdrop(graphics);

        int cardWidth = Math.min(760, this.width - 36);
        int cardLeft = (this.width - cardWidth) / 2;
        int cardTop = 18;
        int cardBottom = Math.min(this.height - 62, 322);
        graphics.fill(cardLeft, cardTop, cardLeft + cardWidth, cardBottom, 0xE519202A);
        graphics.outline(cardLeft, cardTop, cardWidth, cardBottom - cardTop, 0x806B89A8);

        int innerLeft = cardLeft + 18;
        int innerRight = cardLeft + cardWidth - 18;
        int middle = (innerLeft + innerRight) / 2;
        int columnGap = 8;
        int columnTop = cardTop + 55;
        int columnBottom = cardTop + 205;
        graphics.fill(innerLeft, columnTop, middle - columnGap, columnBottom, 0x99101620);
        graphics.outline(innerLeft, columnTop, middle - columnGap - innerLeft, columnBottom - columnTop, 0x604F6B83);
        graphics.fill(middle + columnGap, columnTop, innerRight, columnBottom, 0x99101620);
        graphics.outline(middle + columnGap, columnTop, innerRight - (middle + columnGap), columnBottom - columnTop, 0x604F6B83);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int cardWidth = Math.min(760, this.width - 36);
        int cardLeft = (this.width - cardWidth) / 2;
        int cardTop = 18;
        int cardBottom = Math.min(this.height - 62, 322);

        graphics.centeredText(this.font, Component.translatable("renderbench.result.title"), this.width / 2, cardTop + 13, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable(
                "renderbench.result.subtitle",
                result.environment().minecraftVersion(),
                result.environment().width(),
                result.environment().height(),
                result.environment().renderDistance()
        ), this.width / 2, cardTop + 28, 0xFFB9C9D8);

        int innerLeft = cardLeft + 18;
        int innerRight = cardLeft + cardWidth - 18;
        int middle = (innerLeft + innerRight) / 2;
        int columnGap = 8;
        int columnTop = cardTop + 55;
        int columnBottom = cardTop + 205;

        drawResultColumn(graphics, innerLeft, columnTop, middle - columnGap, columnBottom,
                Component.translatable("renderbench.result.new_chunks"), outbound);
        drawResultColumn(graphics, middle + columnGap, columnTop, innerRight, columnBottom,
                Component.translatable("renderbench.result.generated_chunks"), returning);

        int infoY = cardTop + 216;
        drawCentered(graphics, Component.translatable(
                "renderbench.result.route",
                format0(result.route().distanceBlocks()),
                format2(result.config().effectiveSpeedBlocksPerSecond()),
                directionText()
        ), infoY, 0xFFE6EDF3);

        drawCentered(graphics, Component.translatable(
                "renderbench.result.total_time",
                format2(result.totalDurationNanos() / 1_000_000_000.0)
        ), infoY + 14, 0xFFB9C9D8);

        String loadLevel = formatOptionalSeconds(WorldLoadTracker.loadLevelMillis());
        String firstFrame = formatOptionalSeconds(WorldLoadTracker.firstWorldFrameMillis());
        drawCentered(graphics, Component.translatable("renderbench.result.world_load", loadLevel, firstFrame), infoY + 28, 0xFFB9C9D8);

        drawCentered(graphics, Component.translatable("renderbench.result.explanation"), infoY + 47, 0xFF8FA9BE);
        drawCentered(graphics, Component.translatable("renderbench.result.metric_hint"), infoY + 61, 0xFF8FA9BE);

        if (status != null) {
            drawCentered(graphics, status, Math.min(this.height - 66, cardBottom + 8), statusColor);
        }
    }

    private void drawResultColumn(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom,
                                  Component title, FrameStats stats) {
        graphics.centeredText(this.font, title, (left + right) / 2, top + 10, 0xFFFFFFFF);

        int labelX = left + 12;
        int valueX = right - 12;
        int y = top + 34;
        drawMetric(graphics, labelX, valueX, y, "renderbench.metric.average_fps", format1(stats.averageFps()), " FPS");
        drawMetric(graphics, labelX, valueX, y + 23, "renderbench.metric.one_low", format1(stats.onePercentLow()), " FPS");
        drawMetric(graphics, labelX, valueX, y + 46, "renderbench.metric.point_one_low", format1(stats.pointOnePercentLow()), " FPS");
        drawMetric(graphics, labelX, valueX, y + 69, "renderbench.metric.p99", format2(stats.p99FrametimeMs()), " ms");
        drawMetric(graphics, labelX, valueX, y + 92, "renderbench.metric.worst", format2(stats.worstFrametimeMs()), " ms");
    }

    private void drawMetric(GuiGraphicsExtractor graphics, int labelX, int valueRightX, int y,
                            String labelKey, String value, String suffix) {
        String label = Component.translatable(labelKey).getString();
        String renderedValue = value + suffix;
        graphics.text(this.font, label, labelX, y, 0xFFB9C9D8, false);
        graphics.text(this.font, renderedValue, valueRightX - this.font.width(renderedValue), y, 0xFFFFFFFF, true);
    }

    private void drawVoxelBackdrop(GuiGraphicsExtractor graphics) {
        int base = Math.max(0, this.height - 145);
        graphics.fill(0, base, this.width, this.height, 0xFF111B20);
        int step = 32;
        for (int x = 0; x < this.width; x += step) {
            int h = 18 + ((x / step) % 5) * 9;
            graphics.fill(x, base - h, Math.min(this.width, x + step + 1), base, 0xFF20363B);
        }
        for (int x = 16; x < this.width; x += 64) {
            int y = 52 + ((x / 64) % 4) * 19;
            graphics.fill(x, y, x + 3, y + 3, 0x70FFFFFF);
        }
    }

    private void drawCentered(GuiGraphicsExtractor graphics, Component component, int y, int color) {
        String text = component.getString();
        graphics.text(this.font, text, Math.max(8, (this.width - this.font.width(text)) / 2), y, color, false);
    }


    private String directionText() {
        String key = result.config().direction().translationKey();
        return key == null ? result.config().direction().label() : Component.translatable(key).getString();
    }

    private static String formatOptionalSeconds(double millis) {
        if (millis < 0) return Component.translatable("renderbench.result.not_captured").getString();
        return String.format(Locale.ROOT, "%.2f s", millis / 1000.0);
    }

    private static String format0(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String format1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String format2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
