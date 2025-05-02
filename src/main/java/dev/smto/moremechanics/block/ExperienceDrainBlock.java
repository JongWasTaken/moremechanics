package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.ExperienceDrainBlockEntity;
import dev.smto.moremechanics.block.entity.ManagedDisplayBlockEntity;
import dev.smto.moremechanics.util.ParticleUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.function.Consumer;

public class ExperienceDrainBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    private final Identifier id;

    public ExperienceDrainBlock(Identifier id) {
        super(Settings.copy(Blocks.WHITE_CARPET).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
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
        return new ExperienceDrainBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            if (world.getBlockEntity(pos) instanceof ExperienceDrainBlockEntity ent) {
                ItemScatterer.spawn(world, pos, ent);
                ItemScatterer.onStateReplaced(state, world, pos);
            }
            world.removeBlockEntity(pos);
            ParticleUtils.createBlockBreakParticles(world, pos, Blocks.HOPPER.getDefaultState());
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof NamedScreenHandlerFactory s) {
            player.openHandledScreen(s);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) return;
        if (world.getBlockEntity(pos) instanceof ManagedDisplayBlockEntity ent) {
            ent.ensureDisplay(world, pos);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof ExperienceDrainBlockEntity) {
                ExperienceDrainBlockEntity.tick(world, pos, (ExperienceDrainBlockEntity) te);
            }
        };
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.WHITE_CARPET.getDefaultState();
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
        tooltip.accept(Text.translatable("block.moremechanics.experience_drain.description").formatted(MoreMechanics.getTooltipFormatting()));
        tooltip.accept(Text.translatable("block.moremechanics.experience_drain.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }
}
