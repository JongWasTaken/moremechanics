package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.util.DisplayTransformations;
import dev.smto.moremechanics.util.ExperienceUtils;
import dev.smto.moremechanics.util.Transformation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
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
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class ExperienceDrainBlockEntity extends ManagedDisplayBlockEntity implements SidedInventory {
    private static final String DISPLAY_COMMAND_TAG = MoreMechanics.id("experience_hopper_display").toString();

    private final Box checkBox;
    private final SimpleInventory inventory = new SimpleInventory(1);
    private int storedExperience = 0;

    public ExperienceDrainBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.EXPERIENCE_DRAIN, pos, state);
        this.checkBox = new Box(
                new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)
        );
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, registryLookup);
        Inventories.readNbt(tag, this.inventory.heldStacks, registryLookup);
        if (tag.contains("storedExperience")) {
            this.storedExperience = tag.getInt("storedExperience");
        }
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(tag, this.inventory.heldStacks, registryLookup);
        tag.putInt("storedExperience", this.storedExperience);
        super.writeNbt(tag, registryLookup);
    }

    private void fixPlayerLevels(ServerPlayerEntity player) {
        int transfer = (int) (player.experienceProgress * player.getNextLevelExperience());
        int i = this.addExperience(transfer);
        player.addExperience(-i);
    }

    public int addExperience(int amount) {
        int add = Math.min(Math.max(0, amount), Integer.MAX_VALUE - this.storedExperience);
        this.storedExperience += add;
        this.markDirty();
        return add;
    }

    private boolean cooldown = false;

    public static void tick(World world, BlockPos pos, ExperienceDrainBlockEntity blockEntity) {
        blockEntity.ensureDisplay(world, pos);
        //if (blockEntity.inventory.getStack(0).getCount() != 64 && blockEntity.storedExperience < 100) {
            if (!blockEntity.cooldown) {
                for (Entity otherEntity : world.getOtherEntities(null, blockEntity.checkBox, entity -> true)) {
                    if (otherEntity instanceof ServerPlayerEntity player) {
                        blockEntity.fixPlayerLevels(player);
                        int lvl = Math.max(player.experienceLevel - 1, 0);
                        if (lvl != 0) {
                            int xp = ExperienceUtils.getBarCapacityAtLevel(lvl);
                            int i = blockEntity.addExperience(xp);
                            player.addExperience(-i);
                            player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                    Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP), SoundCategory.MASTER,
                                    player.getX(), player.getY(), player.getZ(),
                                    0.3F, world.random.nextFloat(), 0
                            ));
                        }
                        continue;
                    }
                    if (otherEntity instanceof ExperienceOrbEntity experienceOrbEntity) {
                        blockEntity.storedExperience += experienceOrbEntity.getExperienceAmount();
                        experienceOrbEntity.discard();
                        continue;
                    }
                }
                blockEntity.cooldown = true;
            } else blockEntity.cooldown = false;
        //}

        int xpToConvert = blockEntity.storedExperience / 10;
        while(xpToConvert > 0) {
            if(blockEntity.inventory.addStack(new ItemStack(MoreMechanics.Items.SOLIDIFIED_EXPERIENCE, 1)).isEmpty()) {
                blockEntity.storedExperience -= 10;
                xpToConvert--;
            } else break;
        }

        blockEntity.markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[] { 0 };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
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
        return ExperienceDrainBlockEntity.DISPLAY_COMMAND_TAG;
    }

    @Override
    protected DisplayData getDisplayData(World world, BlockPos pos, int index, DisplayType forType) {
        return new DisplayData(Blocks.IRON_BLOCK.getDefaultState(), MoreMechanics.Items.EXPERIENCE_DRAIN.getDefaultStack(), DisplayData.INVALID.textData());
    }

    private static final DisplaySpec[] DISPLAY_SPECS = { DisplaySpec.ITEM };

    @Override
    protected DisplaySpec[] getDisplaySpec() {
        return ExperienceDrainBlockEntity.DISPLAY_SPECS;
    }

    private static final Transformation TRANSFORMATION = DisplayTransformations.getForItem(null).setScale(1.01f).addTranslation(0.0f, 0.0045f, 0.0f);

    @Override
    protected Transformation getDisplayTransformation(World world, BlockPos pos, int index, DisplayType forType) {
        return ExperienceDrainBlockEntity.TRANSFORMATION;
    }
}