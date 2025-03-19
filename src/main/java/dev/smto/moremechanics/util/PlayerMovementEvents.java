package dev.smto.moremechanics.util;

import dev.smto.moremechanics.api.PlayerMovementListener;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerMovementEvents {
    /**
     * An event that is called when a player jumps.
     */
    public static final Event<Jump> JUMP = EventFactory.createArrayBacked(Jump.class, callbacks -> (player) -> {
        for (Jump callback : callbacks) {
            if (!callback.allowJump(player)) {
                return false;
            }
        }

        return true;
    });
    /**
     * An event that is called when a player sneaks.
     */
    public static final Event<Sneak> SNEAK = EventFactory.createArrayBacked(Sneak.class, callbacks -> (player) -> {
        for (Sneak callback : callbacks) {
            if (!callback.allowSneak(player)) {
                return false;
            }
        }

        return true;
    });

    @FunctionalInterface
    public interface Jump {
        /**
         * Called when a player jumps.
         *
         * @param player the player
         * @return true if the jump should be allowed, false otherwise.
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean allowJump(ServerPlayerEntity player);
    }
    @FunctionalInterface
    public interface Sneak {
        /**
         * Called when a player sneaks.
         *
         * @param player the player
         * @return true if the sneak should be allowed, false otherwise.
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean allowSneak(ServerPlayerEntity player);
    }

    public static void init() {
        PlayerMovementEvents.JUMP.register((player -> {
            BlockPos testPos = player.getBlockPos().down();
            if(player.getWorld().getBlockState(testPos).getBlock() instanceof PlayerMovementListener block)
            {
                return !block.onJump(testPos, player);
            }
            return true;
        }));

        PlayerMovementEvents.SNEAK.register((player -> {
            BlockPos testPos = player.getBlockPos().down();
            if(player.getWorld().getBlockState(testPos).getBlock() instanceof PlayerMovementListener block) {
                block.onCrouch(testPos, player);
            }
            return true;
        }));
    }
}
