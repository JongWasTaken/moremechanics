package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.SmartHopperBlockEntity;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.List;

public class SmartHopperBlock extends HopperBlock implements PolymerTexturedBlock, MoreMechanicsContent {
    private final Identifier id;

    private final HashMap<Direction, BlockState> models = new HashMap<>();

    private void makeModels(String path) {
        this.models.put(Direction.UP, Blocks.AIR.getDefaultState());
        this.models.put(Direction.DOWN, PolymerBlockResourceUtils.requestBlock(BlockModelType.TRANSPARENT_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path))));
        this.models.put(Direction.EAST, PolymerBlockResourceUtils.requestBlock(BlockModelType.TRANSPARENT_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path + "_side"), 0, 90)));
        this.models.put(Direction.NORTH, PolymerBlockResourceUtils.requestBlock(BlockModelType.TRANSPARENT_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path + "_side"))));
        this.models.put(Direction.SOUTH, PolymerBlockResourceUtils.requestBlock(BlockModelType.TRANSPARENT_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path + "_side"), 0, 180)));
        this.models.put(Direction.WEST, PolymerBlockResourceUtils.requestBlock(BlockModelType.TRANSPARENT_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path + "_side"), 0, 270)));
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    public SmartHopperBlock(Identifier id) {
        super(Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)).mapColor(MapColor.STONE_GRAY).requiresTool().strength(3.0F, 4.8F).sounds(BlockSoundGroup.METAL).nonOpaque());
        this.id = id;
        this.makeModels(id.getPath());
        this.setDefaultState(this.stateManager.getDefaultState().with(HopperBlock.FACING, Direction.DOWN).with(HopperBlock.ENABLED, Boolean.TRUE));
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
        return new SmartHopperBlockEntity(blockPos, blockState);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof SmartHopperBlockEntity hopperBlockEntity) {
            //player.openHandledScreen(hopperBlockEntity);
            hopperBlockEntity.openFakeScreen((ServerPlayerEntity) player);
            player.incrementStat(Stats.INSPECT_HOPPER);
        }

        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof SmartHopperBlockEntity) {
                SmartHopperBlockEntity.serverTick(world, pos, state, (SmartHopperBlockEntity) te);
            }
        };
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.models.get(state.getOrEmpty(HopperBlock.FACING).orElse(Direction.DOWN));
    }

    @Override
    public void addTooltip(ItemStack stack, List<Text> tooltip) {
        tooltip.add(Text.translatable("block.moremechanics.smart_hopper.description").formatted(MoreMechanics.getTooltipFormatting()));
    }
}
