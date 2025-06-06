package dev.smto.moremechanics.block;

import dev.smto.moremechanics.api.MoreMechanicsContent;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

public class DummyBlock extends Block implements PolymerTexturedBlock, MoreMechanicsContent {
    private final Identifier id;
    private final BlockState polymerBlockState;

    public DummyBlock(Identifier id, Identifier model) {
        super(Settings.copy(Blocks.BEDROCK).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
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
                PolymerBlockModel.of(model)
        );
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.polymerBlockState;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.BLOCK;
    }
}
