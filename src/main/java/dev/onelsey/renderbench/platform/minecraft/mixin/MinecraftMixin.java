package dev.onelsey.renderbench.platform.minecraft.mixin;

import dev.onelsey.renderbench.benchmark.FrameRecorder;
import dev.onelsey.renderbench.benchmark.WorldLoadTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void renderbench$frameStart(boolean advanceGameTime, CallbackInfo ci) {
        long now = System.nanoTime();
        FrameRecorder.onFrameStart(now);
        Minecraft client = (Minecraft) (Object) this;
        if (client.level != null) {
            WorldLoadTracker.onWorldFrame();
        }
    }
}
