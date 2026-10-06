package dev.onelsey.renderbench.platform.minecraft;

import com.mojang.blaze3d.platform.InputConstants;
import dev.onelsey.renderbench.benchmark.BenchmarkController;
import dev.onelsey.renderbench.benchmark.BenchmarkRunResult;
import dev.onelsey.renderbench.benchmark.ClientEnvironmentSnapshot;
import dev.onelsey.renderbench.platform.BenchmarkPlatform;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLScancode;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public final class MinecraftPlatform implements BenchmarkPlatform {
    private static final String MOD_ID = "renderbench";

    private final KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    private final KeyMapping openKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.renderbench.open", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_C, category)
    );

    @Override
    public void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> BenchmarkController.onServerTick(new MinecraftServerAccess(server)));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    openBenchmarkScreen();
                }
            }

            if (BenchmarkController.isRunning() && client.player != null) {
                lockClientRotation(BenchmarkController.routeYaw(), BenchmarkController.routePitch());
            }
        });
    }

    @Override
    public boolean canBenchmarkCurrentWorld() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.getSingleplayerServer() != null;
    }

    @Override
    public UUID currentPlayerId() {
        Minecraft client = Minecraft.getInstance();
        return client.player == null ? null : client.player.getUUID();
    }

    @Override
    public ClientEnvironmentSnapshot captureEnvironment() {
        Minecraft client = Minecraft.getInstance();
        Path gameDir = FabricLoader.getInstance().getGameDir();
        return new ClientEnvironmentSnapshot(
                client.getWindow().getWidth(),
                client.getWindow().getHeight(),
                client.options.renderDistance().get(),
                client.options.simulationDistance().get(),
                client.options.framerateLimit().get(),
                version("renderbench"),
                version("minecraft"),
                version("fabricloader"),
                version("fabric-api"),
                System.getProperty("java.version"),
                System.getProperty("os.name") + " " + System.getProperty("os.version"),
                gameDir
        );
    }

    @Override
    public void openBenchmarkScreen() {
        Minecraft.getInstance().gui.setScreen(new RenderBenchScreen());
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public void presentResult(BenchmarkRunResult result, Path reportDirectory) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new RenderBenchResultScreen(result, reportDirectory)));
    }

    @Override
    public void openPath(Path path) throws IOException {
        Path target = path.toAbsolutePath().normalize();
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (osName.contains("win")) {
            new ProcessBuilder("explorer.exe", target.toString()).start();
            return;
        }

        if (osName.contains("mac")) {
            new ProcessBuilder("open", target.toString()).start();
            return;
        }

        if (osName.contains("linux") || osName.contains("unix") || osName.contains("bsd")) {
            IOException firstFailure = null;
            try {
                new ProcessBuilder("xdg-open", target.toString()).start();
                return;
            } catch (IOException exception) {
                firstFailure = exception;
            }

            try {
                new ProcessBuilder("gio", "open", target.toString()).start();
                return;
            } catch (IOException exception) {
                if (firstFailure != null) {
                    exception.addSuppressed(firstFailure);
                }
            }
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(target.toFile());
            return;
        }

        throw new IOException("No supported file manager integration is available for " + System.getProperty("os.name", "unknown OS"));
    }

    @Override
    public void lockClientRotation(float yaw, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.setYRot(yaw);
            client.player.setXRot(pitch);
        }
    }

    @Override
    public void postMessage(String text) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(text));
            }
        });
    }

    @Override
    public void postTranslatedMessage(String translationKey, Object... args) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable(translationKey, args));
            }
        });
    }

    private static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
