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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;


public class MechanicalUserBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    public static final EnumProperty<Direction> FACING = Properties.FACING;

    private final Identifier id;

    private final BlockState baseStateNorth;
    private final BlockState baseStateEast;
    private final BlockState baseStateSouth;
    private final BlockState baseStateWest;
    private final BlockState baseStateUp;
    private final BlockState baseStateDown;

    public MechanicalUserBlock(Identifier id) {
        super(Settings.copy(Blocks.DISPENSER).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
        this.setDefaultState(this.stateManager.getDefaultState().with(MechanicalUserBlock.FACING, Direction.NORTH));

        var left = PolymerBlockResourceUtils.getBlocksLeft(BlockModelType.FULL_BLOCK);
        if (left < 6) {
            throw new RuntimeException("Not enough Polymer BlockStates left! Mechanical user requires 6, but only " + left + " are available!");
        }

        this.baseStateNorth = this.makeBlockState(0, 0);
        this.baseStateEast = this.makeBlockState(0, 90);
        this.baseStateSouth = this.makeBlockState(0, 180);
        this.baseStateWest = this.makeBlockState(0, 270);
        this.baseStateUp = this.makeBlockState(90, 0);
        this.baseStateDown = this.makeBlockState(270, 0);
    }

    private BlockState makeBlockState(int x, int y) {
        return PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MoreMechanics.MOD_ID, "block/" + this.id.getPath()), x, y)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MechanicalUserBlock.FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(MechanicalUserBlock.FACING, ctx.getPlayerLookDirection().getOpposite());
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
        return new MechanicalUserBlockEntity(blockPos, blockState);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MechanicalUserBlockEntity) {
            player.openHandledScreen((NamedScreenHandlerFactory)blockEntity);
        }
        return ActionResult.PASS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) return;
        if (world.getBlockEntity(pos) instanceof ExperienceStorageBlockEntity ent) {
            ServerPlayerEntity p = null;
            if (placer instanceof ServerPlayerEntity) p = (ServerPlayerEntity) placer;
            ent.setInitialized();
            ent.initDisplayEntity((ServerWorld) world, p);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof MechanicalUserBlockEntity) {
                MechanicalUserBlockEntity.tick(world, pos, (MechanicalUserBlockEntity) te);
            }
        };
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof MechanicalUserBlockEntity ent) {
            //if (!world.isClient()) ent.dropExperience((ServerWorld) world, pos);
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        if (true) return Blocks.BARRIER.getDefaultState();
        return switch (state.get(AbstractFurnaceBlock.FACING)) {
            case DOWN -> this.baseStateDown;
            case UP -> this.baseStateUp;
            case NORTH -> this.baseStateNorth;
            case SOUTH -> this.baseStateSouth;
            case WEST -> this.baseStateWest;
            case EAST -> this.baseStateEast;
        };
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.METAL;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
