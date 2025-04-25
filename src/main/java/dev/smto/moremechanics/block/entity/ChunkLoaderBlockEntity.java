package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.MoreMechanics;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ChunkLoaderBlockEntity extends BlockEntity {
    private final ChunkPos chunkPos;
    private int counter = 100;
    private FakePlayer fakePlayer;
    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.CHUNK_LOADER, pos, state);
        this.chunkPos = new ChunkPos(pos);
    }

    @Override
    public void markRemoved() {
        if (this.world instanceof ServerWorld w) {
            w.setChunkForced(this.chunkPos.x, this.chunkPos.z, false);
            if (this.fakePlayer != null) this.fakePlayer.discard();
        }
        super.markRemoved();
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, DynamicRegistryManager.EMPTY);
        if(tag.contains("counter")) {
            this.counter = tag.getInt("counter");
        }
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("counter", this.counter);
        super.writeNbt(tag, DynamicRegistryManager.EMPTY);
    }

    public static void tick(World world, ChunkLoaderBlockEntity blockEntity) {
        blockEntity.counter++;
        if (blockEntity.counter > 100) {
            if (world instanceof ServerWorld w) {
                w.setChunkForced(blockEntity.chunkPos.x, blockEntity.chunkPos.z, true);
                if (blockEntity.fakePlayer == null) {
                    blockEntity.fakePlayer = FakePlayer.get(w);
                    blockEntity.fakePlayer.setPosition(Vec3d.of(blockEntity.getPos()));
                }

            }
            blockEntity.counter = 0;
        }
        world.getServer().getPlayerManager()
                .sendToAround(
                        null,
                        blockEntity.pos.getX(), blockEntity.pos.getY(), blockEntity.pos.getZ(),
                        10,
                        world.getRegistryKey(),
                        new ParticleS2CPacket(ParticleTypes.REVERSE_PORTAL, false, blockEntity.pos.getX() + 0.5, blockEntity.pos.getY() + 0.65, blockEntity.pos.getZ() + 0.5, 0, 0, 0, 0.01f, 1)
                );
    }
}