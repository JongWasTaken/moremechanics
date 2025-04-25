package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.api.PlayerMovementListener;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class ElevatorBlock extends Block implements PlayerMovementListener, PolymerTexturedBlock, MoreMechanicsContent {
    private static final String DISPLAY_COMMAND_TAG = MoreMechanics.id("elevator_display").toString();

    private final Identifier id;

    public ElevatorBlock(Identifier id) {
        super(Settings.copy(Blocks.STONE).nonOpaque().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.BLOCK;
    }

    private Pair<Boolean, Float> checkBlock(BlockState state) {
        if (state.isAir()) {
            return new Pair<>(true, 0.0f);
        }
        if (state.isIn(BlockTags.SNOW) || state.isIn(BlockTags.PRESSURE_PLATES)) {
            return new Pair<>(true, 0.0f);
        }
        if (state.isIn(BlockTags.SLABS)) {
            return new Pair<>(true, 0.5f);
        }
        if (state.isIn(BlockTags.WOOL_CARPETS)) {
            return new Pair<>(true, 0.075f);
        }
        return new Pair<>(false, 0.0f);
    }

    public static final List<Block> BLOCK_BLACKLIST = List.of(
            Blocks.BARRIER,
            Blocks.STRUCTURE_VOID,
            Blocks.LIGHT
    );

    @Override
    public boolean onJump(BlockPos position, ServerPlayerEntity player)
    {
        position = position.up();
        ServerWorld world = (ServerWorld) player.getWorld();
        int count = 0;
        while(count < 128 && position.getY() < 316)
        {
            if(world.getBlockState(position).getBlock() instanceof ElevatorBlock)
            {
                BlockPos tpPosition = position.up();
                var check = this.checkBlock(world.getBlockState(tpPosition));
                if(check.getLeft() && world.getBlockState(tpPosition.up()).isAir())
                {
                    if (player.teleport(tpPosition.getX() + 0.5f, tpPosition.getY() + check.getRight(), tpPosition.getZ() + 0.5f, false)) {
                        world.playSound(null, tpPosition, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f );
                        world.spawnParticles(ParticleTypes.POOF, tpPosition.getX() + 0.5f, tpPosition.getY(), tpPosition.getZ() + 0.5f, 5, 0, 0, 0, 0.25f);
                    }
                    return true;
                }
            }
            position = position.up();
            count++;
        }
        return false;
    }

    @Override
    public void onCrouch(BlockPos position, ServerPlayerEntity player)
    {
        position = position.down();
        ServerWorld world = (ServerWorld) player.getWorld();
        int count = 0;
        while(count < 128 && position.getY() >= -64)
        {
            if(world.getBlockState(position).getBlock() instanceof ElevatorBlock)
            {
                BlockPos tpPosition = position.up();
                var check = this.checkBlock(world.getBlockState(tpPosition));
                if(check.getLeft() && world.getBlockState(tpPosition.up()).isAir())
                {
                    if (player.teleport(tpPosition.getX() + 0.5f, tpPosition.getY() + check.getRight(), tpPosition.getZ() + 0.5f, false)) {
                        world.playSound(null, tpPosition, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f);
                        world.spawnParticles(ParticleTypes.POOF, tpPosition.getX() + 0.5f, tpPosition.getY(), tpPosition.getZ() + 0.5f, 5, 0, 0, 0, 0.25f);
                    }
                    return;
                }
            }
            position = position.down();
            count++;
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            for (DisplayEntity.BlockDisplayEntity blockDisplayEntity : this.getDisplayEntities(world, pos)) {
                blockDisplayEntity.discard();
            }
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient) return;
        var ent = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        ent.setBlockState(MoreMechanics.Blocks.DUMMY_ELEVATOR.getDefaultState());
        ent.addCommandTag(ElevatorBlock.DISPLAY_COMMAND_TAG);
        ent.setPosition(pos.getX(), pos.getY(), pos.getZ());
        ent.setBrightness(null);
        world.spawnEntity(ent);
    }

    private List<DisplayEntity.BlockDisplayEntity> getDisplayEntities(World world, BlockPos pos) {
        return world.getEntitiesByType(EntityType.BLOCK_DISPLAY, new Box(pos).expand(0.0000001), e -> e.getCommandTags().contains(ElevatorBlock.DISPLAY_COMMAND_TAG) && e.getBlockPos().equals(pos));
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockState newState = MoreMechanics.Blocks.DUMMY_ELEVATOR.getDefaultState();
        ActionResult result = ActionResult.CONSUME;
        var handItem = player.getStackInHand(hand).getItem();
        if (handItem instanceof BlockItem blockItem && !(blockItem.getBlock() instanceof ElevatorBlock)) {
            if ((blockItem.getBlock() instanceof PolymerBlock pB)) {
                if (!ElevatorBlock.BLOCK_BLACKLIST.contains(pB.getPolymerBlockState(newState, PacketContext.create()).getBlock())){
                    newState = blockItem.getPlacementState(new ItemPlacementContext(world, player, hand, stack, hit));
                    if (newState == null) newState = blockItem.getBlock().getDefaultState();
                }
            } else {
                newState = blockItem.getPlacementState(new ItemPlacementContext(world, player, hand, stack, hit));
                if (newState == null) newState = blockItem.getBlock().getDefaultState();
            }
        }

        boolean updated = false;

        for (DisplayEntity.BlockDisplayEntity blockDisplayEntity : this.getDisplayEntities(world, pos)) {
            blockDisplayEntity.setBlockState(newState);
            updated = true;
        }
        if (updated) result = ActionResult.SUCCESS_SERVER;
        player.getInventory().markDirty();
        return result;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(MoreMechanics.Blocks.ELEVATOR);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public void addTooltip(ItemStack stack, List<Text> tooltip) {
        tooltip.add(Text.translatable("block.moremechanics.elevator.description").formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.add(Text.translatable("block.moremechanics.elevator.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }

    @Override
    public Block getDummyBlock() {
        return MoreMechanics.Blocks.DUMMY_ELEVATOR;
    }
}