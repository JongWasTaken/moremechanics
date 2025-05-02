package dev.smto.moremechanics.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

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
                                false, false,
                                pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5,
                                0, 0, 0,
                                speed, 1
                        )
                );
    }

    public static void createBlockBreakParticles(ServerWorld world, BlockPos pos, BlockState state) {
        List<Packet<? super ClientPlayPacketListener>> particles = new ArrayList<>();
        if (!state.isAir() && state.hasBlockBreakParticles()) {
            VoxelShape voxelShape = state.getOutlineShape(world, pos);
            voxelShape.forEachBox(
                    (minX, minY, minZ, maxX, maxY, maxZ) -> {
                        double dx = Math.min(1.0, maxX - minX);
                        double e = Math.min(1.0, maxY - minY);
                        double f = Math.min(1.0, maxZ - minZ);
                        int i = Math.max(2, MathHelper.ceil(dx / 0.25));
                        int j = Math.max(2, MathHelper.ceil(e / 0.25));
                        int k = Math.max(2, MathHelper.ceil(f / 0.25));

                        for (int l = 0; l < i; l++) {
                            for (int m = 0; m < j; m++) {
                                for (int n = 0; n < k; n++) {
                                    double g = (l + 0.5) / i;
                                    double h = (m + 0.5) / j;
                                    double o = (n + 0.5) / k;
                                    double p = g * dx + minX;
                                    double q = h * e + minY;
                                    double r = o * f + minZ;
                                    particles.add(
                                            new ParticleS2CPacket(
                                                    new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                                                    false, false,
                                                    pos.getX() + p, pos.getY() + q, pos.getZ() + r,
                                                    (float) (g - 0.5), (float) (h - 0.5), (float) (o - 0.5), 1.2f, 0)
                                    );
                                }
                            }
                        }
                    }
            );
        }
        world.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 32.0, world.getRegistryKey(), new BundleS2CPacket(particles));
    }
}
