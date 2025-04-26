package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.util.DisplayTransformations;
import dev.smto.moremechanics.util.Transformation;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public class RedstoneClockBlockEntity extends ManagedDisplayBlockEntity {
    private static final String DISPLAY_COMMAND_TAG = MoreMechanics.id("redstone_clock_hopper_display").toString();

    private static final int[] INTERVALS = { 5, 10, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200, 400, 600, 800, 1000, 1200, 2400, 3600, 4800, 6000, 12000, 24000, 36000, 72000 };
    private static final String[] INTERVAL_DESCRIPTION = { "0.25s", "0.5s", "1s", "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s", "20s", "30s", "40s", "50s", "1min", "2min", "3min", "4min", "5min", "10min", "20min", "30min", "1h" };
    private int selectedInterval = 2;
    private boolean paused = false;

    public RedstoneClockBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.REDSTONE_CLOCK, pos, state);
    }

    public ActionResult adjustInterval(ServerPlayerEntity player) {
        int newInterval = this.selectedInterval;
        if (player.isSneaking()) {
            newInterval--;
            if (newInterval < 0) {
                newInterval = RedstoneClockBlockEntity.INTERVALS.length - 1;
            }
        } else {
            newInterval++;
            if (newInterval > RedstoneClockBlockEntity.INTERVALS.length - 1) {
                newInterval = 0;
            }
        }
        player.sendMessage(Text.literal("Interval: " + RedstoneClockBlockEntity.INTERVAL_DESCRIPTION[newInterval]), true);
        this.selectedInterval = newInterval;
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
                SoundCategory.BLOCKS, this.pos.getX(), this.pos.getY(), this.pos.getZ(), 0.3f, 1.0f, 0
        ));
        return ActionResult.SUCCESS_SERVER;
    }

    public ActionResult togglePause(ServerPlayerEntity player) {
        this.paused = !this.paused;
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
                SoundCategory.BLOCKS, this.pos.getX(), this.pos.getY(), this.pos.getZ(), 0.3f, 1.0f, 0
        ));
        if (this.paused) player.sendMessage(Text.translatable("block.moremechanics.redstone_clock.paused"), true);
        else player.sendMessage(Text.translatable("block.moremechanics.redstone_clock.unpaused"), true);
        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, registryLookup);
        if (tag.contains("paused")) this.paused = tag.getBoolean("paused");
        if (tag.contains("interval")) {
            int interval = tag.getInt("interval");
            for (int i = 0; i < RedstoneClockBlockEntity.INTERVALS.length; i++) {
                if (RedstoneClockBlockEntity.INTERVALS[i] == interval) {
                    this.selectedInterval = i;
                    break;
                }
            }
        }
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("paused", this.paused);
        tag.putInt("interval", RedstoneClockBlockEntity.INTERVALS[this.selectedInterval]);
        super.writeNbt(tag, registryLookup);
    }

    private int tick;

    public static void tick(World world, BlockPos pos, RedstoneClockBlockEntity blockEntity) {
        blockEntity.ensureDisplay(world, pos);
        world.setBlockState(pos, world.getBlockState(pos).with(Properties.POWERED, false));
        if (blockEntity.paused) return;
        blockEntity.tick++;
        if (blockEntity.tick >= RedstoneClockBlockEntity.INTERVALS[blockEntity.selectedInterval]) {
            blockEntity.tick = 0;
            world.setBlockState(pos, world.getBlockState(pos).with(Properties.POWERED, true));
            Objects.requireNonNull(world.getServer()).getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 8, world.getRegistryKey(),
                    new PlaySoundS2CPacket(
                            Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_DISPENSER_FAIL),
                            SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 0.3f, 1.0f, 0));
        }
    }

    @Override
    protected String getDisplayCommandTag() {
        return RedstoneClockBlockEntity.DISPLAY_COMMAND_TAG;
    }

    @Override
    protected DisplayData getDisplayData(World world, BlockPos pos, int index, DisplayType forType) {
        return DisplayData.create(TextDisplayData.create(Text.literal(RedstoneClockBlockEntity.INTERVAL_DESCRIPTION[this.selectedInterval])));
    }

    private static final DisplaySpec[] DISPLAY_SPECS = { DisplaySpec.TEXT };

    @Override
    protected DisplaySpec[] getDisplaySpec() {
        return RedstoneClockBlockEntity.DISPLAY_SPECS;
    }

    private static final boolean[] DISPLAY_TICK_ENABLED = { true };

    @Override
    protected boolean[] getDisplayTickEnabled() {
        return RedstoneClockBlockEntity.DISPLAY_TICK_ENABLED;
    }

    private static final Transformation TRANSFORMATION = DisplayTransformations.getForItem(null);

    @Override
    protected Transformation getDisplayTransformation(World world, BlockPos pos, int index, DisplayType forType) {
        return RedstoneClockBlockEntity.TRANSFORMATION;
    }
}