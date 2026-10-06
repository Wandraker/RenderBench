package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.benchmark.BenchmarkRunResult;
import dev.onelsey.renderbench.benchmark.FrameStats;
import dev.onelsey.renderbench.benchmark.WorldLoadTracker;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ResultCardExporter {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private ResultCardExporter() {
    }

    public static Path write(BenchmarkRunResult result, Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path output = reportDirectory.resolve("renderbench-card.png");

        FrameStats outbound = FrameStats.calculate(result.outboundFrames());
        FrameStats returning = FrameStats.calculate(result.returnFrames());

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintBackground(g);

            int panelX = 70;
            int panelY = 55;
            int panelW = WIDTH - 140;
            int panelH = HEIGHT - 110;

            g.setColor(new Color(18, 26, 35, 232));
            g.fillRoundRect(panelX, panelY, panelW, panelH, 28, 28);
            g.setColor(new Color(104, 139, 168, 110));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(panelX, panelY, panelW, panelH, 28, 28);

            Font titleFont = new Font(Font.DIALOG, Font.BOLD, 40);
            Font subtitleFont = new Font(Font.DIALOG, Font.PLAIN, 20);
            Font sectionFont = new Font(Font.DIALOG, Font.BOLD, 24);
            Font metricLabelFont = new Font(Font.DIALOG, Font.PLAIN, 18);
            Font metricValueFont = new Font(Font.DIALOG, Font.BOLD, 27);
            Font footerFont = new Font(Font.DIALOG, Font.PLAIN, 17);

            drawCentered(g, tr("renderbench.result.title"), WIDTH / 2, panelY + 58, titleFont, Color.WHITE);
            drawCentered(g, tr("renderbench.result.subtitle",
                    result.environment().minecraftVersion(),
                    result.environment().width(),
                    result.environment().height(),
                    result.environment().renderDistance()),
                    WIDTH / 2, panelY + 92, subtitleFont, new Color(190, 207, 221));

            int gap = 22;
            int columnW = (panelW - 70 - gap) / 2;
            int leftX = panelX + 35;
            int rightX = leftX + columnW + gap;
            int columnY = panelY + 130;
            int columnH = 315;

            paintStatsPanel(g, leftX, columnY, columnW, columnH,
                    tr("renderbench.result.new_chunks"), outbound,
                    sectionFont, metricLabelFont, metricValueFont);
            paintStatsPanel(g, rightX, columnY, columnW, columnH,
                    tr("renderbench.result.generated_chunks"), returning,
                    sectionFont, metricLabelFont, metricValueFont);

            int footerY = columnY + columnH + 34;
            String route = tr("renderbench.result.route",
                    f0(result.route().distanceBlocks()),
                    f2(result.config().effectiveSpeedBlocksPerSecond()),
                    directionText(result));
            drawCentered(g, route, WIDTH / 2, footerY, footerFont, new Color(231, 238, 244));

            String total = tr("renderbench.result.total_time", f2(result.totalDurationNanos() / 1_000_000_000.0));
            drawCentered(g, total, WIDTH / 2, footerY + 27, footerFont, new Color(190, 207, 221));

            String worldLoad = tr("renderbench.result.world_load",
                    optionalSeconds(WorldLoadTracker.loadLevelMillis()),
                    optionalSeconds(WorldLoadTracker.firstWorldFrameMillis()));
            drawCentered(g, worldLoad, WIDTH / 2, footerY + 54, footerFont, new Color(190, 207, 221));

            drawCentered(g, tr("renderbench.result.metric_hint"), WIDTH / 2, footerY + 89,
                    new Font(Font.DIALOG, Font.PLAIN, 16), new Color(145, 169, 190));

            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
            g.setColor(new Color(125, 145, 162));
            g.drawString("RenderBench " + result.environment().renderBenchVersion(), panelX + 22, panelY + panelH - 18);
            String seed = "Seed " + result.seed();
            g.drawString(seed, panelX + panelW - 22 - g.getFontMetrics().stringWidth(seed), panelY + panelH - 18);
        } finally {
            g.dispose();
        }

        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("PNG writer is not available");
        }
        return output;
    }

    private static void paintBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(31, 61, 91), 0, HEIGHT, new Color(8, 14, 22)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 255, 255, 80));
        for (int i = 0; i < 14; i++) {
            int x = 55 + i * 89;
            int y = 45 + (i % 5) * 36;
            g.fillRect(x, y, 4, 4);
        }

        int ground = 610;
        g.setColor(new Color(20, 39, 43));
        g.fillRect(0, ground, WIDTH, HEIGHT - ground);
        g.setColor(new Color(35, 70, 69));
        for (int x = 0; x < WIDTH; x += 64) {
            int blocks = 1 + ((x / 64) % 5);
            for (int i = 0; i < blocks; i++) {
                g.fillRect(x, ground - 32 * (i + 1), 66, 33);
            }
        }
        g.setColor(new Color(24, 48, 51));
        for (int x = 32; x < WIDTH; x += 128) {
            g.fillRect(x, ground - 64, 66, 65);
        }
    }

    private static void paintStatsPanel(Graphics2D g, int x, int y, int w, int h, String title,
                                        FrameStats stats, Font sectionFont, Font labelFont, Font valueFont) {
        g.setColor(new Color(10, 17, 24, 190));
        g.fillRoundRect(x, y, w, h, 22, 22);
        g.setColor(new Color(80, 107, 131, 120));
        g.drawRoundRect(x, y, w, h, 22, 22);

        drawCentered(g, title, x + w / 2, y + 42, sectionFont, Color.WHITE);

        int metricY = y + 88;
        metric(g, x + 28, x + w - 28, metricY, tr("renderbench.metric.average_fps"), f1(stats.averageFps()) + " FPS", labelFont, valueFont);
        metric(g, x + 28, x + w - 28, metricY + 48, tr("renderbench.metric.one_low"), f1(stats.onePercentLow()) + " FPS", labelFont, valueFont);
        metric(g, x + 28, x + w - 28, metricY + 96, tr("renderbench.metric.point_one_low"), f1(stats.pointOnePercentLow()) + " FPS", labelFont, valueFont);
        metric(g, x + 28, x + w - 28, metricY + 144, tr("renderbench.metric.p99"), f2(stats.p99FrametimeMs()) + " ms", labelFont, valueFont);
        metric(g, x + 28, x + w - 28, metricY + 192, tr("renderbench.metric.worst"), f2(stats.worstFrametimeMs()) + " ms", labelFont, valueFont);
    }

    private static void metric(Graphics2D g, int left, int right, int y, String label, String value,
                               Font labelFont, Font valueFont) {
        g.setFont(labelFont);
        g.setColor(new Color(180, 199, 215));
        g.drawString(label, left, y);

        g.setFont(valueFont);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(value, right - fm.stringWidth(value), y + 2);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baselineY, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }


    private static String directionText(BenchmarkRunResult result) {
        String key = result.config().direction().translationKey();
        return key == null ? result.config().direction().label() : tr(key);
    }

    private static String optionalSeconds(double millis) {
        if (millis < 0) return tr("renderbench.result.not_captured");
        return String.format(Locale.ROOT, "%.2f s", millis / 1000.0);
    }

    private static String f0(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String f1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String f2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
