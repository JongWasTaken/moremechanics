package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;


public class PeaceBeaconBlock extends Block implements PolymerTexturedBlock, MoreMechanicsContent {
    private final BlockState polymerBlockState;
    private final Identifier id;

    public PeaceBeaconBlock(Identifier id) {
        super(Settings.copy(Blocks.IRON_BARS).strength(0.5f).luminance((x) -> 15).sounds(BlockSoundGroup.METAL).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.polymerBlockState = PolymerBlockResourceUtils.requestBlock(
                BlockModelType.PLANT_BLOCK,
                PolymerBlockModel.of(Identifier.of(MoreMechanics.MOD_ID, "block/" + id.getPath()))
        );
        this.id = id;
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        world.scheduleBlockTick(pos, state.getBlock(), 0);
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.BLOCK;
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public final BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.polymerBlockState;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.STONE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void addTooltip(List<Text> tooltip) {
        tooltip.add(MutableText.of(new TranslatableTextContent("block.moremechanics.peace_beacon.description", null, List.of(MoreMechanics.Config.peaceBeaconRadius).toArray(new Integer[0]))).formatted(MoreMechanics.getTooltipFormatting()));
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Objects.requireNonNull(world.getServer()).getPlayerManager().sendToAround(
                null,
                pos.getX(), pos.getY(), pos.getZ(),
                32,
                world.getRegistryKey(),
                new ParticleS2CPacket(
                        ParticleTypes.PORTAL,
                        false,
                        pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                        random.nextFloat() * 0.5F, 0, random.nextFloat() * 0.5F,
                        0.01f, 3
                )
        );
        if (world instanceof ServerWorld w) {
            PeaceBeaconBlock.addPeacefulChunks(w.getRegistryKey(), this.getPeacefulChunks(pos));
        }
        world.scheduleBlockTick(pos, state.getBlock(), 20);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld w) {
            PeaceBeaconBlock.removePeacefulChunks(w.getRegistryKey(), this.getPeacefulChunks(pos));
        }
    }

    private LinkedHashSet<ChunkPos> getPeacefulChunks(BlockPos pos) {
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        ChunkPos centerChunk = new ChunkPos(pos);
        for (int x = centerChunk.x - MoreMechanics.Config.peaceBeaconRadius; x < centerChunk.x + MoreMechanics.Config.peaceBeaconRadius; x++) {
            for (int z = centerChunk.z - MoreMechanics.Config.peaceBeaconRadius; z < centerChunk.z + MoreMechanics.Config.peaceBeaconRadius; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }

    private static final Map<RegistryKey<World>, LinkedHashSet<ChunkPos>> SUPPRESSED_CHUNK_MAP = new LinkedHashMap<>();

    public static void addPeacefulChunks(RegistryKey<World> registryKey, LinkedHashSet<ChunkPos> linkedHashSet) {
        LinkedHashSet<ChunkPos> set = PeaceBeaconBlock.SUPPRESSED_CHUNK_MAP.getOrDefault(registryKey, new LinkedHashSet<>());
        set.addAll(linkedHashSet);
        PeaceBeaconBlock.SUPPRESSED_CHUNK_MAP.put(registryKey, set);
    }

    public static void removePeacefulChunks(RegistryKey<World> registryKey, LinkedHashSet<ChunkPos> linkedHashSet) {
        if (PeaceBeaconBlock.SUPPRESSED_CHUNK_MAP.containsKey(registryKey)) PeaceBeaconBlock.SUPPRESSED_CHUNK_MAP.get(registryKey).removeAll(linkedHashSet);
    }

    public static boolean isChunkPeaceful(RegistryKey<World> registryKey, ChunkPos chunkPos) {
        LinkedHashSet<ChunkPos> set = PeaceBeaconBlock.SUPPRESSED_CHUNK_MAP.getOrDefault(registryKey, new LinkedHashSet<>());
        return set.contains(chunkPos);
    }
}
