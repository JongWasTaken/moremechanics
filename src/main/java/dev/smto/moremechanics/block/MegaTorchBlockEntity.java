package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class MegaTorchBlockEntity extends BlockEntity {
    public MegaTorchBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.MEGA_TORCH_ENTITY, pos, state);
    }
    private int chunkRadius;
    private int count = 40;

    public void updateValues() {
        double torchQuantity = 9 * 64;
        if (this.world instanceof ServerWorld w) {
            MegaTorchBlockEntity.removeSuppressedChunks(w.getRegistryKey(), this.getSuppressedChunks());
        }
        this.chunkRadius = (int) Math.sqrt(torchQuantity / 9);
        if (this.world instanceof ServerWorld w) {
            MegaTorchBlockEntity.addSuppressedChunks(w.getRegistryKey(), this.getSuppressedChunks());
        }
    }

    @Override
    public void markRemoved() {
        if (this.world instanceof ServerWorld w) {
            MegaTorchBlockEntity.removeSuppressedChunks(w.getRegistryKey(), this.getSuppressedChunks());
        }
        super.markRemoved();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.updateValues();
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, DynamicRegistryManager.EMPTY);
        this.updateValues();
    }

    private LinkedHashSet<ChunkPos> getSuppressedChunks() {
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        ChunkPos centerChunk = new ChunkPos(this.pos);
        for (int x = centerChunk.x - this.chunkRadius; x < centerChunk.x + this.chunkRadius; x++) {
            for (int z = centerChunk.z - this.chunkRadius; z < centerChunk.z + this.chunkRadius; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }

    private static final Map<RegistryKey<World>, LinkedHashSet<ChunkPos>> SUPPRESSED_CHUNK_MAP = new LinkedHashMap<>();

    public static void tick(World world, MegaTorchBlockEntity blockEntity) {
        --blockEntity.count;
        if (blockEntity.count == 0) {
            blockEntity.count = 40;
            if (world instanceof ServerWorld w) {
                MegaTorchBlockEntity.addSuppressedChunks(w.getRegistryKey(), blockEntity.getSuppressedChunks());
            }
        }
    }

    public static void addSuppressedChunks(RegistryKey<World> registryKey, LinkedHashSet<ChunkPos> linkedHashSet) {
        LinkedHashSet<ChunkPos> set = MegaTorchBlockEntity.SUPPRESSED_CHUNK_MAP.getOrDefault(registryKey, new LinkedHashSet<>());
        set.addAll(linkedHashSet);
        MegaTorchBlockEntity.SUPPRESSED_CHUNK_MAP.put(registryKey, set);
    }

    public static void removeSuppressedChunks(RegistryKey<World> registryKey, LinkedHashSet<ChunkPos> linkedHashSet) {
        if (MegaTorchBlockEntity.SUPPRESSED_CHUNK_MAP.containsKey(registryKey)) MegaTorchBlockEntity.SUPPRESSED_CHUNK_MAP.get(registryKey).removeAll(linkedHashSet);
    }

    public static boolean isChunkSuppressed(RegistryKey<World> registryKey, ChunkPos chunkPos) {
        LinkedHashSet<ChunkPos> set = MegaTorchBlockEntity.SUPPRESSED_CHUNK_MAP.getOrDefault(registryKey, new LinkedHashSet<>());
        return set.contains(chunkPos);
    }

}