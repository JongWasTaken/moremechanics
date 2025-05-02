package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.ManagedDisplayBlockEntity;
import dev.smto.moremechanics.block.entity.MechanicalBreakerBlockEntity;
import dev.smto.moremechanics.util.ParticleUtils;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
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
import net.minecraft.server.world.ServerWorld;
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

import java.util.HashMap;
import java.util.function.Consumer;

public class MechanicalBreakerBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private final Identifier id;

    private final HashMap<Direction, BlockState> models = new HashMap<>();

    private void makeModels(String path) {
        this.models.put(Direction.UP, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 270, 0)));
        this.models.put(Direction.DOWN, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 90, 0)));
        this.models.put(Direction.EAST, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 90)));
        this.models.put(Direction.NORTH, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 0)));
        this.models.put(Direction.SOUTH, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 180)));
        this.models.put(Direction.WEST, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 270)));
    }

    public MechanicalBreakerBlock(Identifier id) {
        super(Settings.copy(Blocks.STONE).nonOpaque().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
        this.makeModels(id.getPath());
        this.setDefaultState(this.stateManager.getDefaultState().with(MechanicalBreakerBlock.FACING, Direction.NORTH).with(MechanicalBreakerBlock.POWERED, Boolean.FALSE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MechanicalBreakerBlock.FACING, MechanicalBreakerBlock.POWERED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(MechanicalBreakerBlock.FACING, ctx.getPlayerLookDirection().getOpposite());
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
        return new MechanicalBreakerBlockEntity(blockPos, blockState);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MechanicalBreakerBlockEntity) {
            player.openHandledScreen((NamedScreenHandlerFactory)blockEntity);
        }
        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        ManagedDisplayBlockEntity.onPlacedBlock(world, pos, state, placer, itemStack);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof MechanicalBreakerBlockEntity) {
                MechanicalBreakerBlockEntity.tick(world, pos, (MechanicalBreakerBlockEntity) te);
            }
        };
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        boolean bl = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
        boolean bl2 = state.get(MechanicalBreakerBlock.POWERED);
        if (bl && !bl2) {
            world.setBlockState(pos, state.with(MechanicalBreakerBlock.POWERED, Boolean.TRUE), Block.NOTIFY_LISTENERS);
        } else if (!bl && bl2) {
            world.setBlockState(pos, state.with(MechanicalBreakerBlock.POWERED, Boolean.FALSE), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            if (world.getBlockEntity(pos) instanceof MechanicalBreakerBlockEntity ent) {
                ItemScatterer.spawn(world, pos, ent);
                ItemScatterer.onStateReplaced(state, world, pos);
            }
            world.removeBlockEntity(pos);
            ParticleUtils.createBlockBreakParticles(world, pos, Blocks.DISPENSER.getDefaultState());
        }
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.models.get(state.get(MechanicalBreakerBlock.FACING));
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
        return state.with(MechanicalBreakerBlock.FACING, rotation.rotate(state.get(MechanicalBreakerBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(MechanicalBreakerBlock.FACING)));
    }

    @Override
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(Text.translatable("block.moremechanics.mechanical_breaker.description").formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.accept(Text.translatable("block.moremechanics.mechanical_breaker.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }
}
