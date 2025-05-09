package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
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
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;

public class CamouflageBlock extends Block implements PolymerTexturedBlock, MoreMechanicsContent {
    private static final String DISPLAY_COMMAND_TAG = "moremechanics:camouflage_display";

    private final Identifier id;

    public CamouflageBlock(Identifier id) {
        super(Settings.copy(Blocks.STONE).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
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

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            for (DisplayEntity.BlockDisplayEntity blockDisplayEntity : this.getDisplayEntities(world, pos)) {
                blockDisplayEntity.discard();
            }
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient) return;
        var ent = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        ent.setBlockState(MoreMechanics.Blocks.DUMMY_CAMOUFLAGE_BLOCK.getDefaultState());
        ent.addCommandTag(CamouflageBlock.DISPLAY_COMMAND_TAG);
        ent.setPosition(pos.getX(), pos.getY(), pos.getZ());
        ent.setBrightness(null);
        world.spawnEntity(ent);
    }

    private List<DisplayEntity.BlockDisplayEntity> getDisplayEntities(World world, BlockPos pos) {
        return world.getEntitiesByType(EntityType.BLOCK_DISPLAY, new Box(pos).expand(0.0000001), e -> {
            return e.getCommandTags().contains(CamouflageBlock.DISPLAY_COMMAND_TAG) && e.getBlockPos().equals(pos);
        });
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.FAIL;
        BlockState newState = MoreMechanics.Blocks.DUMMY_CAMOUFLAGE_BLOCK.getDefaultState();
        ActionResult result = ActionResult.CONSUME;
        var handItem = player.getStackInHand(hand).getItem();
        if (handItem instanceof BlockItem blockItem && !(blockItem.getBlock() instanceof ElevatorBlock)) {
            if ((blockItem.getBlock() instanceof PolymerBlock pB)) {
                if (!ElevatorBlock.BLOCK_BLACKLIST.contains(pB.getPolymerBlockState(newState, PacketContext.create()).getBlock())){
                    newState = blockItem.getPlacementState(new ItemPlacementContext(world, player, hand, stack, hit));
                    if (newState == null) newState = blockItem.getBlock().getDefaultState();
                }
            } if (blockItem.getBlock() instanceof MoreMechanicsContent mmc) {
                if (!mmc.getDummyBlock().equals(Blocks.AIR)) {
                    newState = mmc.getDummyBlock().getDefaultState();
                } else return result;
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
        if (player instanceof ServerPlayerEntity sp) {
            // fixes the inventory desync
            sp.networkHandler.sendPacket(new SetPlayerInventoryS2CPacket(player.getInventory().getSelectedSlot(), player.getStackInHand(hand)));
        }
        return result;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(MoreMechanics.Blocks.CAMOUFLAGE_BLOCK);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.STRUCTURE_VOID.getDefaultState();
    }

    @Override
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(Text.translatable("block.moremechanics.camouflage_block.description").formatted(MoreMechanics.getTooltipFormatting()));
    }

    @Override
    public Block getDummyBlock() {
        return MoreMechanics.Blocks.DUMMY_CAMOUFLAGE_BLOCK;
    }
}