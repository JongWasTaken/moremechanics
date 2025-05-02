package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.util.ModWorldDataSaver;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ChunkLoaderBlock extends Block implements PolymerTexturedBlock, MoreMechanicsContent {
    private final Identifier id;
    private final BlockState polymerBlockState;

    public ChunkLoaderBlock(Identifier id) {
        super(Settings.copy(Blocks.GOLD_BLOCK).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
        BlockModelType b = BlockModelType.BIOME_PLANT_BLOCK;
        if (PolymerBlockResourceUtils.getBlocksLeft(b) == 0) {
            b = BlockModelType.PLANT_BLOCK;
        }
        if (PolymerBlockResourceUtils.getBlocksLeft(b) == 0) {
            throw new RuntimeException("Out of block models!");
        }
        this.polymerBlockState = PolymerBlockResourceUtils.requestBlock(
                b,
                PolymerBlockModel.of(Identifier.of(MoreMechanics.MOD_ID, "block/" + id.getPath()))
        );
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.BLOCK;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world instanceof ServerWorld w) {
            var stateLoader = ModWorldDataSaver.get(w.getServer());
            var globalPos = new GlobalPos(world.getRegistryKey(), pos);
            if (!stateLoader.getExistingChunkLoaders().contains(globalPos)) {
                stateLoader.getExistingChunkLoaders().add(globalPos);
                stateLoader.markDirty();
            }
            ChunkLoaderBlock.markChunks(w, pos);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
        world.scheduleBlockTick(pos, state.getBlock(), 0);
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Objects.requireNonNull(world.getServer()).getPlayerManager().sendToAround(
                null,
                pos.getX(), pos.getY(), pos.getZ(),
                32,
                world.getRegistryKey(),
                new ParticleS2CPacket(
                        ParticleTypes.PORTAL,
                        false, false,
                        pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                        random.nextFloat() * 0.5F, 0, random.nextFloat() * 0.5F,
                        0.01f, 3
                )
        );
        if (world instanceof ServerWorld w) {
            ChunkLoaderBlock.markChunks(w, pos);
        }
        world.scheduleBlockTick(pos, state.getBlock(), 20);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock()) && world instanceof ServerWorld w) {
            var stateLoader = ModWorldDataSaver.get(w.getServer());
            var globalPos = new GlobalPos(w.getRegistryKey(), pos);
            if (stateLoader.getExistingChunkLoaders().contains(globalPos)) {
                stateLoader.getExistingChunkLoaders().remove(globalPos);
                stateLoader.markDirty();
            }
            ChunkLoaderBlock.unmarkChunks(w, pos);
        }
    }


    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.polymerBlockState;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.METAL;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(MutableText.of(new TranslatableTextContent("block.moremechanics.chunk_loader.description", null, List.of((double) MoreMechanics.Config.chunkLoaderRadius / 2).toArray(new Double[0]))).formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.accept(Text.translatable("block.moremechanics.chunk_loader.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }

    public static void markChunks(ServerWorld world, BlockPos pos) {
        for (ChunkPos affectedChunk : ChunkLoaderBlock.getAffectedChunks(pos)) {
            world.setChunkForced(affectedChunk.x, affectedChunk.z, true);
        }
    }

    public static void unmarkChunks(ServerWorld world, BlockPos pos) {
        for (ChunkPos affectedChunk : ChunkLoaderBlock.getAffectedChunks(pos)) {
            world.setChunkForced(affectedChunk.x, affectedChunk.z, false);
        }
    }

    private static LinkedHashSet<ChunkPos> getAffectedChunks(BlockPos pos) {
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        ChunkPos centerChunk = new ChunkPos(pos);
        for (int x = centerChunk.x - 3; x < centerChunk.x + 3; x++) {
            for (int z = centerChunk.z - 3; z < centerChunk.z + 3; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }
}
