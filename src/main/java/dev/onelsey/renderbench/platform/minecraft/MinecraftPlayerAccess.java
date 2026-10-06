package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.platform.BenchmarkPlayerAccess;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public final class MinecraftPlayerAccess implements BenchmarkPlayerAccess {
    private final ServerPlayer player;

    public MinecraftPlayerAccess(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public String dimensionId() {
        return player.level().dimension().identifier().toString();
    }

    @Override
    public long seed() {
        return player.level().getSeed();
    }

    @Override
    public double x() {
        return player.getX();
    }

    @Override
    public double y() {
        return player.getY();
    }

    @Override
    public double z() {
        return player.getZ();
    }

    @Override
    public float yaw() {
        return player.getYRot();
    }

    @Override
    public float pitch() {
        return player.getXRot();
    }

    @Override
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.teleportTo(player.level(), x, y, z, Set.of(), yaw, pitch, false);
    }
}
