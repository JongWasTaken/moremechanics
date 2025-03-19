package dev.smto.moremechanics.util;

import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;

public class ParticleUtils {
    public static final int RED = ColorHelper.getArgb(0, 255, 0, 0);
    public static final int BLUE = ColorHelper.getArgb(0, 0, 0, 255);
    public static final int GREEN = ColorHelper.getArgb(0, 0, 255, 0);
    public static final int WHITE = ColorHelper.getArgb(0, 255, 255, 255);

    public static void showDustParticleAround(ServerWorld world, BlockPos pos, int radius, int color) {
        ParticleUtils.showParticleAround(new DustParticleEffect(color, 1.0F), world, pos, radius);
    }

    public static <T extends ParticleEffect> void showParticleAround(T particleEffect, ServerWorld world, BlockPos pos, int radius) {
        ParticleUtils.showParticleAround(particleEffect, world, pos, radius, 0.01f);
    }

    public static <T extends ParticleEffect> void showParticleAround(T particleEffect, ServerWorld world, BlockPos pos, int radius, float speed) {
        world.getServer().getPlayerManager()
                .sendToAround(
                        null,
                        pos.getX(), pos.getY(), pos.getZ(),
                        radius,
                        world.getRegistryKey(),
                        new ParticleS2CPacket(
                                particleEffect,
                                false,
                                pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5,
                                0, 0, 0,
                                speed, 1
                        )
                );
    }
}
