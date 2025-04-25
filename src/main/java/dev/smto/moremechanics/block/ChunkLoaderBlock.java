package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.ChunkLoaderBlockEntity;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class ChunkLoaderBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
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
    public BlockEntity createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ChunkLoaderBlockEntity(blockPos, blockState);
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof ChunkLoaderBlockEntity) {
                ChunkLoaderBlockEntity.tick(world, (ChunkLoaderBlockEntity) te);
            }
        };
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
    public void addTooltip(ItemStack stack, List<Text> tooltip) {
        tooltip.add(Text.translatable("block.moremechanics.chunk_loader.description").formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.add(Text.translatable("block.moremechanics.chunk_loader.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }
}
