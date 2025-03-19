package dev.smto.moremechanics.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public interface PlayerMovementListener {
    boolean onJump(BlockPos position, ServerPlayerEntity player);
    void onCrouch(BlockPos position, ServerPlayerEntity player);
}
