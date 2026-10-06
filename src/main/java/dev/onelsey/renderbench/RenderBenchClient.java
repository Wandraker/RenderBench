package dev.onelsey.renderbench;

import dev.onelsey.renderbench.platform.PlatformServices;
import dev.onelsey.renderbench.platform.minecraft.MinecraftPlatform;
import net.fabricmc.api.ClientModInitializer;

public final class RenderBenchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinecraftPlatform platform = new MinecraftPlatform();
        PlatformServices.install(platform);
        platform.initialize();
    }
}
