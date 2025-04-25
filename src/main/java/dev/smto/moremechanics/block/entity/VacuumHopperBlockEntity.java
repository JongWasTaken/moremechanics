package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.util.DisplayTransformations;
import dev.smto.moremechanics.util.Transformation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class VacuumHopperBlockEntity extends ManagedDisplayBlockEntity implements SidedInventory, NamedScreenHandlerFactory {
    private static final String DISPLAY_COMMAND_TAG = MoreMechanics.id("vacuum_hopper_display").toString();

    private final Box checkBox;
    private final SimpleInventory inventory = new SimpleInventory(27);
    private int storedExperience = 0;

    public VacuumHopperBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.VACUUM_HOPPER, pos, state);
        this.checkBox = new Box(
                new Vec3d(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2),
                new Vec3d(pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3)
        );
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, DynamicRegistryManager.EMPTY);
        Inventories.readNbt(tag, this.inventory.heldStacks, registryLookup);
        if (tag.contains("storedExperience")) {
            this.storedExperience = tag.getInt("storedExperience");
        }
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(tag, this.inventory.heldStacks, registryLookup);
        tag.putInt("storedExperience", this.storedExperience);
        super.writeNbt(tag, DynamicRegistryManager.EMPTY);
    }

    public static void tick(World world, BlockPos pos, VacuumHopperBlockEntity blockEntity) {
        blockEntity.ensureDisplay(world, pos);
        for (Entity otherEntity : world.getOtherEntities(null, blockEntity.checkBox, entity -> true)) {
            if (otherEntity instanceof ItemEntity itemEntity) {
                var stack = itemEntity.getStack();
                stack = blockEntity.inventory.addStack(stack);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setStack(stack);
                }
                continue;
            }
            if (otherEntity instanceof ExperienceOrbEntity experienceOrbEntity) {
                blockEntity.storedExperience += experienceOrbEntity.getExperienceAmount();
                experienceOrbEntity.discard();
                continue;
            }
        }

        int xpToConvert = blockEntity.storedExperience / 10;
        while(xpToConvert > 0) {
            if(blockEntity.inventory.addStack(new ItemStack(MoreMechanics.Items.SOLIDIFIED_EXPERIENCE, 1)).isEmpty()) {
                blockEntity.storedExperience -= 10;
                xpToConvert--;
            } else break;
        }

        Objects.requireNonNull(world.getServer()).getPlayerManager()
                .sendToAround(
                        null,
                        blockEntity.pos.getX(), blockEntity.pos.getY(), blockEntity.pos.getZ(),
                        10,
                        world.getRegistryKey(),
                        new ParticleS2CPacket(ParticleTypes.REVERSE_PORTAL, false, blockEntity.pos.getX() + 0.5, blockEntity.pos.getY() + 0.65, blockEntity.pos.getZ() + 0.5, 0, 0, 0, 0.01f, 1)
                );

        blockEntity.markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return IntStream.rangeClosed(0, 26).toArray();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
        //return !this.getStack(slot).isEmpty();
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return this.inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return this.inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.setStack(slot, stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.moremechanics.vacuum_hopper");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }

    @Override
    public void onOpen(PlayerEntity player) {
        this.inventory.onOpen(player);
    }

    @Override
    public void onClose(PlayerEntity player) {
        this.inventory.onClose(player);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return this.inventory.isValid(slot, stack);
    }

    @Override
    public int getMaxCountPerStack() {
        return this.inventory.getMaxCountPerStack();
    }

    @Override
    public int getMaxCount(ItemStack stack) {
        return this.inventory.getMaxCount(stack);
    }

    @Override
    public int count(Item item) {
        return this.inventory.count(item);
    }

    @Override
    public boolean containsAny(Predicate<ItemStack> predicate) {
        return this.inventory.containsAny(predicate);
    }

    @Override
    public boolean containsAny(Set<Item> items) {
        return this.inventory.containsAny(items);
    }

    @Override
    public boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
        return this.inventory.canTransferTo(hopperInventory, slot, stack);
    }

    @Override
    protected String getDisplayCommandTag() {
        return VacuumHopperBlockEntity.DISPLAY_COMMAND_TAG;
    }

    @Override
    protected DisplayData getDisplayData(World world, BlockPos pos, int index, DisplayType forType) {
        return new DisplayData(Blocks.HOPPER.getDefaultState(), MoreMechanics.Items.VACUUM_HOPPER.getDefaultStack(), DisplayData.INVALID.textData());
    }

    private static final DisplaySpec[] DISPLAY_SPECS = { DisplaySpec.ITEM };

    @Override
    protected DisplaySpec[] getDisplaySpec() {
        return VacuumHopperBlockEntity.DISPLAY_SPECS;
    }

    private static final Transformation TRANSFORMATION = DisplayTransformations.getForItem(null);

    @Override
    protected Transformation getDisplayTransformation(World world, BlockPos pos, int index, DisplayType forType) {
        return VacuumHopperBlockEntity.TRANSFORMATION;
    }
}