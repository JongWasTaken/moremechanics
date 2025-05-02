package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// So this is basically a re-creation of the cool castle doors from TwilightForest
public class MultiDoorBlock extends Block implements PolymerTexturedBlock, MoreMechanicsContent {
    private final Identifier id;
    private final BlockState visibleBlockState;
    public static final BooleanProperty ACTIVATED = Properties.ENABLED;
    public static final BooleanProperty INVISIBLE = Properties.POWERED;

    public MultiDoorBlock(Identifier id) {
        super(Settings.copy(Blocks.IRON_BLOCK).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.setDefaultState(this.getDefaultState().with(MultiDoorBlock.ACTIVATED, false).with(MultiDoorBlock.INVISIBLE, false));
        this.id = id;
        this.visibleBlockState = PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(MoreMechanics.id("block/" + id.getPath())));
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
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(Text.translatable("block.moremechanics.any_multi_door_block.description").formatted(MoreMechanics.getTooltipFormatting()));
    }

    @Override
    public final BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        if (state.get(MultiDoorBlock.INVISIBLE)) {
            return Blocks.STRUCTURE_VOID.getDefaultState();
        }
        return this.visibleBlockState;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.WOOD;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MultiDoorBlock.ACTIVATED, MultiDoorBlock.INVISIBLE);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return this.activateDoor(world, pos, state);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!(sourceBlock.equals(this)) && world.isReceivingRedstonePower(pos)) {
            this.activateDoor(world, pos, state);
        }
    }

    private ActionResult activateDoor(World world, BlockPos pos, BlockState state) {
        if (state.get(MultiDoorBlock.INVISIBLE) || state.get(MultiDoorBlock.ACTIVATED)) return ActionResult.FAIL;
        this.setActive(world, pos, state);
        return ActionResult.SUCCESS_SERVER;
    }

    private void setActive(World world, BlockPos pos, BlockState originState) {
        if (originState.getBlock().equals(this)) {
            world.setBlockState(pos, originState.with(MultiDoorBlock.ACTIVATED, true));
        }
        world.scheduleBlockTick(pos, originState.getBlock(), 2 + world.getRandom().nextInt(5));
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockState newStateBase = this.getDefaultState();
        if (state.get(MultiDoorBlock.INVISIBLE)) {
            if (state.get(MultiDoorBlock.ACTIVATED)) {
                world.setBlockState(pos, newStateBase.with(MultiDoorBlock.INVISIBLE, false).with(MultiDoorBlock.ACTIVATED, false));
            } else {
                this.setActive(world, pos, state);
            }
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.125f, world.getRandom().nextFloat() * 0.25F + 0.25F);
        } else {
            if (state.get(MultiDoorBlock.ACTIVATED)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                world.setBlockState(pos, newStateBase.with(MultiDoorBlock.INVISIBLE, true).with(MultiDoorBlock.ACTIVATED, false), 27);
                world.scheduleBlockTick(pos, this, 80);

                world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.125f, world.getRandom().nextFloat() * 0.25F + 1.75F);

                List<Packet<? super ClientPlayPacketListener>> particles = new ArrayList<>();
                for (int dx = 0; dx < 4; ++dx) {
                    for (int dy = 0; dy < 4; ++dy) {
                        for (int dz = 0; dz < 4; ++dz) {
                            particles.add(new ParticleS2CPacket(ParticleTypes.WHITE_SMOKE, false, false,
                                    pos.getX() + (dx + 0.5D) / 4,
                                    pos.getY() + (dy + 0.5D) / 4,
                                    pos.getZ() + (dz + 0.5D) / 4,
                                    (float) random.nextGaussian() * 0.2F, (float) random.nextGaussian() * 0.2F, (float) random.nextGaussian() * 0.2F,
                                    0.1f, 1));
                        }
                    }
                }
                world.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 32.0, world.getRegistryKey(), new BundleS2CPacket(particles));

                for (Direction d : Direction.values()) {
                    BlockState state2 = world.getBlockState(pos.offset(d));
                    if (state2.getBlock().equals(this) && !state2.get(MultiDoorBlock.INVISIBLE) && !state2.get(MultiDoorBlock.ACTIVATED)) {
                        this.setActive(world, pos.offset(d), state2);
                    }
                }
            }
        }
    }
}
