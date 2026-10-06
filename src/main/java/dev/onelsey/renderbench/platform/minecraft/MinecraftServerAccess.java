package dev.onelsey.renderbench.platform.minecraft;

import dev.onelsey.renderbench.platform.BenchmarkPlayerAccess;
import dev.onelsey.renderbench.platform.BenchmarkServerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class MinecraftServerAccess implements BenchmarkServerAccess {
    private final MinecraftServer server;

    public MinecraftServerAccess(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public BenchmarkPlayerAccess findPlayer(UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player == null ? null : new MinecraftPlayerAccess(player);
    }
}
