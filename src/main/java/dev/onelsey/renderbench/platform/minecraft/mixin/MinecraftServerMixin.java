package dev.onelsey.renderbench.platform.minecraft.mixin;

import dev.onelsey.renderbench.benchmark.WorldLoadTracker;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void renderbench$loadLevelStart(CallbackInfo ci) {
        WorldLoadTracker.onLoadLevelStart();
    }

    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void renderbench$loadLevelEnd(CallbackInfo ci) {
        WorldLoadTracker.onLoadLevelEnd();
    }
}
