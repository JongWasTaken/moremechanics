package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.ManagedDisplayBlockEntity;
import dev.smto.moremechanics.block.entity.MechanicalPlacerBlockEntity;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class MechanicalPlacerBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private final Identifier id;

    public MechanicalPlacerBlock(Identifier id) {
        super(Settings.copy(Blocks.STONE).nonOpaque().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
        this.setDefaultState(this.stateManager.getDefaultState().with(MechanicalPlacerBlock.FACING, Direction.NORTH).with(MechanicalPlacerBlock.POWERED, Boolean.FALSE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MechanicalPlacerBlock.FACING, MechanicalPlacerBlock.POWERED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(MechanicalPlacerBlock.FACING, ctx.getPlayerLookDirection().getOpposite());
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
        return new MechanicalPlacerBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            ItemScatterer.onStateReplaced(state, newState, world, pos);
            world.removeBlockEntity(pos);
            world.addBlockBreakParticles(pos, Blocks.DISPENSER.getDefaultState());
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MechanicalPlacerBlockEntity) {
            player.openHandledScreen((NamedScreenHandlerFactory)blockEntity);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        ManagedDisplayBlockEntity.onPlacedBlock(world, pos, state, placer, itemStack);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof MechanicalPlacerBlockEntity) {
                MechanicalPlacerBlockEntity.tick(world, pos, (MechanicalPlacerBlockEntity) te);
            }
        };
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        boolean bl = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
        boolean bl2 = state.get(MechanicalPlacerBlock.POWERED);
        if (bl && !bl2) {
            world.setBlockState(pos, state.with(MechanicalPlacerBlock.POWERED, Boolean.TRUE), Block.NOTIFY_LISTENERS);
        } else if (!bl && bl2) {
            world.setBlockState(pos, state.with(MechanicalPlacerBlock.POWERED, Boolean.FALSE), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(MoreMechanics.Items.MECHANICAL_PLACER);
    }
    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
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
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(MechanicalPlacerBlock.FACING, rotation.rotate(state.get(MechanicalPlacerBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(MechanicalPlacerBlock.FACING)));
    }

    @Override
    public void addTooltip(ItemStack stack, List<Text> tooltip) {
        tooltip.add(Text.translatable("block.moremechanics.mechanical_placer.description").formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.add(Text.translatable("block.moremechanics.mechanical_placer.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }

    @Override
    public Block getDummyBlock() {
        return MoreMechanics.Blocks.DUMMY_MECHANICAL_PLACER;
    }
}
