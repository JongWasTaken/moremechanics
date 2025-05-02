package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.RedstoneClockBlockEntity;
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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.function.Consumer;

public class RedstoneClockBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    private final Identifier id;

    private final HashMap<Direction, BlockState> models = new HashMap<>();

    private void makeModels(String path) {
        this.models.put(Direction.EAST, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 90)));
        this.models.put(Direction.NORTH, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path))));
        this.models.put(Direction.SOUTH, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 180)));
        this.models.put(Direction.WEST, PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + path), 0, 270)));
    }

    public RedstoneClockBlock(Identifier id) {
        super(Settings.copy(Blocks.GLASS).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
        this.makeModels(id.getPath());
        this.setDefaultState(this.stateManager.getDefaultState().with(Properties.FACING, Direction.NORTH).with(Properties.POWERED, Boolean.TRUE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.FACING, Properties.POWERED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(MechanicalBreakerBlock.FACING, ctx.getHorizontalPlayerFacing().getOpposite());
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
        return new RedstoneClockBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            ItemScatterer.onStateReplaced(state, world, pos);
            world.removeBlockEntity(pos);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof RedstoneClockBlockEntity t) {
            if (hit.getSide().equals(Direction.UP)) return t.togglePause((ServerPlayerEntity) player);
            return t.adjustInterval((ServerPlayerEntity) player);
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return state.get(Properties.POWERED);
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(Properties.POWERED) ? 15 : 0;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) return;
        if (world.getBlockEntity(pos) instanceof RedstoneClockBlockEntity ent) {
            ent.ensureDisplay(world, pos);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof RedstoneClockBlockEntity) {
                RedstoneClockBlockEntity.tick(world, pos, (RedstoneClockBlockEntity) te);
            }
        };
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.models.getOrDefault(state.get(Properties.FACING), this.models.get(Direction.NORTH));
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
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(Text.translatable("block.moremechanics.redstone_clock.description").formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.accept(Text.translatable("block.moremechanics.redstone_clock.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }
}
