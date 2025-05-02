package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.TankBlockEntity;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.function.Consumer;

public class TankBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    private final Identifier id;

    public TankBlock(Identifier id) {
        super(Settings.copy(Blocks.GLASS).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
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
    public BlockEntity createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TankBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            if (world.getBlockEntity(pos) instanceof TankBlockEntity ent) {
                ItemScatterer.spawn(world, pos, ent);
                ItemScatterer.onStateReplaced(state, world, pos);
            }
            world.removeBlockEntity(pos);
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!player.isCreative()) {
            if (world.getBlockEntity(pos) instanceof TankBlockEntity t) {
                world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, t.getAsStack()));
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getMainHandStack().isEmpty()) {
            if (world.getBlockEntity(pos) instanceof TankBlockEntity t) {
                return t.onUse((ServerPlayerEntity) player);
            }
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.FAIL;
        if (world.getBlockEntity(pos) instanceof TankBlockEntity t && player instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(
                    new BlockUpdateS2CPacket(pos.offset(hit.getSide()), world.getBlockState(pos.offset(hit.getSide())))
            );
            if (stack.getItem() instanceof BucketItem) {
                return t.interactBucket(sp, stack);
            }
            if (stack.getItem() instanceof PotionItem || stack.getItem() instanceof GlassBottleItem) {
                return t.interactBottle(sp, stack);
            }
        }
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) return;
        if (world.getBlockEntity(pos) instanceof TankBlockEntity ent) {
            ent.ensureDisplay(world, pos);
            ent.loadStack(itemStack);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof TankBlockEntity) {
                TankBlockEntity.tick(world, pos, (TankBlockEntity) te);
            }
        };
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        if (world.getBlockEntity(pos) instanceof TankBlockEntity t) {
            return t.getAsStack();
        }
        return new ItemStack(this);
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
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(Text.translatable("block.moremechanics.tank.description").formatted(MoreMechanics.getTooltipFormatting()));
        if (stack.contains(MoreMechanics.DataComponentTypes.TANK_CONTENTS)) {
            var info = stack.get(MoreMechanics.DataComponentTypes.TANK_CONTENTS);
            tooltip.accept(Text.translatable("block.minecraft." + Registries.FLUID.getId(info.fluid()).getPath()).append(Text.literal(": " + info.amount() + "mB")).formatted(Formatting.BLUE));
        }
    }
}
