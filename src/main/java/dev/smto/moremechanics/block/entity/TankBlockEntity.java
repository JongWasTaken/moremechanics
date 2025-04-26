package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.util.DisplayTransformations;
import dev.smto.moremechanics.util.TankContents;
import dev.smto.moremechanics.util.Transformation;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potions;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class TankBlockEntity extends ManagedDisplayBlockEntity implements SidedInventory {
    private static final String DISPLAY_COMMAND_TAG = MoreMechanics.id("tank_display").toString();
    private static final int MAX_AMOUNT = 16000;

    private final SimpleInventory inventory = new SimpleInventory(2);
    private int storedAmount = 0;
    private Fluid storedFluid = Fluids.EMPTY;

    private final Transformation fluidTransformation = new Transformation().setTranslation(0.5f, 0.0f, 0.5f);

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.TANK, pos, state);
    }

    public ActionResult interactBottle(ServerPlayerEntity player, ItemStack bottle) {
        boolean filled = false;
        if (bottle.contains(DataComponentTypes.POTION_CONTENTS)) {
            try {
                filled = bottle.get(DataComponentTypes.POTION_CONTENTS).matches(Potions.WATER);
            } catch (Throwable ignored) {}
        }
        if (!filled) { // pickup
            if (this.storedAmount >= 333 && this.storedFluid != Fluids.EMPTY) {
                this.storedAmount -= 333;
                if (this.storedAmount == 0) {
                    this.storedFluid = Fluids.EMPTY;
                }
                this.markDirty();
                this.updateFluidTransformation();
                if (player == null) return ActionResult.SUCCESS.withNewHandStack(PotionContentsComponent.createStack(Items.POTION, Potions.WATER));
                player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(bottle, player, PotionContentsComponent.createStack(Items.POTION, Potions.WATER)));
                return ActionResult.SUCCESS_SERVER;
            }
        } else { // fill
            if (this.storedFluid == Fluids.EMPTY || this.storedFluid == Fluids.WATER) {
                if (this.storedAmount == 0) {
                    this.storedFluid = Fluids.WATER;
                    this.storedAmount += 333;
                    this.markDirty();
                    this.updateFluidTransformation();
                    if (player == null) return ActionResult.SUCCESS.withNewHandStack(new ItemStack(Items.GLASS_BOTTLE));
                    player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(bottle, player, new ItemStack(Items.GLASS_BOTTLE)));
                    return ActionResult.SUCCESS_SERVER;
                } else {
                    if (this.storedAmount <= 15667) {
                        this.storedFluid = Fluids.WATER;
                        this.storedAmount += 333;
                        this.markDirty();
                        this.updateFluidTransformation();
                        if (player == null) return ActionResult.SUCCESS.withNewHandStack(new ItemStack(Items.GLASS_BOTTLE));
                        player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(bottle, player, new ItemStack(Items.GLASS_BOTTLE)));
                        return ActionResult.SUCCESS_SERVER;
                    }
                }
                if (player == null) return ActionResult.FAIL;
                return ActionResult.CONSUME;
            }
        }
        if (player == null) return ActionResult.FAIL;
        return ActionResult.PASS;
    }

    public ActionResult interactBucket(ServerPlayerEntity player, ItemStack bucket) {
        Fluid bucketFluid = ((BucketItem)bucket.getItem()).fluid;
        if (bucketFluid == Fluids.EMPTY) { // pickup
            if (this.storedAmount >= 1000 && this.storedFluid != Fluids.EMPTY) {
                this.storedAmount -= 1000;
                Fluid cache = this.storedFluid;
                if (this.storedAmount == 0) {
                    this.storedFluid = Fluids.EMPTY;
                }
                this.markDirty();
                this.updateFluidTransformation();
                if (player == null) return ActionResult.SUCCESS.withNewHandStack(cache.getBucketItem().getDefaultStack());
                player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(bucket, player, cache.getBucketItem().getDefaultStack()));
                return ActionResult.SUCCESS_SERVER;
            }
        } else { // fill
            if (this.storedFluid == Fluids.EMPTY || this.storedFluid == bucketFluid) {
                if (this.storedAmount == 0) {
                    this.storedFluid = bucketFluid;
                    this.storedAmount += 1000;
                    this.markDirty();
                    this.updateFluidTransformation();
                    if (player == null) return ActionResult.SUCCESS.withNewHandStack(new ItemStack(Items.BUCKET));
                    player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(bucket, player, BucketItem.getEmptiedStack(bucket, player)));
                    return ActionResult.SUCCESS_SERVER;
                } else {
                    if (this.storedAmount <= 15000) {
                        this.storedFluid = bucketFluid;
                        this.storedAmount += 1000;
                        this.markDirty();
                        this.updateFluidTransformation();
                        if (player == null) return ActionResult.SUCCESS.withNewHandStack(new ItemStack(Items.BUCKET));
                        player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(bucket, player, BucketItem.getEmptiedStack(bucket, player)));
                        return ActionResult.SUCCESS_SERVER;
                    }
                }
                if (player == null) return ActionResult.FAIL;
                return ActionResult.CONSUME;
            }
        }
        if (player == null) return ActionResult.FAIL;
        return ActionResult.PASS;
    }

    public ActionResult onUse(ServerPlayerEntity player) {
        if ((this.storedFluid == Fluids.WATER && this.storedAmount < 333) || (this.storedFluid == Fluids.LAVA && this.storedAmount < 1000)) {
            this.storedFluid = Fluids.EMPTY;
            this.storedAmount = 0;
            this.updateFluidTransformation();
            this.markDirty();
            player.sendMessage(Text.translatable("block.moremechanics.tank.emptied"), true);
        }
        else {
            if (this.storedFluid == Fluids.EMPTY) {
                player.sendMessage(Text.translatable("block.moremechanics.tank.empty"), true);
            } else {
                player.sendMessage(Text.translatable("block.minecraft." + Registries.FLUID.getId(this.storedFluid).getPath()).append(Text.literal(": " + this.storedAmount + "mB")), true);
            }
        }
        return ActionResult.SUCCESS_SERVER;
    }

    public void loadStack(ItemStack stack) {
        if (stack.contains(MoreMechanics.DataComponentTypes.TANK_CONTENTS)) {
            var content = stack.get(MoreMechanics.DataComponentTypes.TANK_CONTENTS);
            this.storedFluid = Objects.requireNonNull(content).fluid();
            this.storedAmount = content.amount();
            this.updateFluidTransformation();
            this.markDirty();
        }
    }

    public ItemStack getAsStack() {
        ItemStack stack = MoreMechanics.Items.TANK.getDefaultStack();
        if (this.storedFluid != Fluids.EMPTY) {
            stack.set(MoreMechanics.DataComponentTypes.TANK_CONTENTS, new TankContents(this.storedFluid, this.storedAmount));
        }
        return stack;
    }

    private void updateFluidTransformation() {
        if (this.storedFluid != Fluids.EMPTY) {
            float val = (float) this.storedAmount / TankBlockEntity.MAX_AMOUNT;
            this.fluidTransformation.setScale(0.99f, val, 0.99f).setTranslation(0.5f, (val / 2) + 0.001f, 0.5f);
        }
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, registryLookup);
        Inventories.readNbt(tag, this.inventory.heldStacks, registryLookup);
        if (tag.contains("storedAmount")) {
            this.storedAmount = tag.getInt("storedAmount");
        }
        if (tag.contains("storedFluid")) {
            try {
                this.storedFluid = Registries.FLUID.get(Identifier.tryParse(tag.getString("storedFluid")));
            } catch (Throwable ignored) {}
        }
        this.updateFluidTransformation();
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(tag, this.inventory.heldStacks, registryLookup);
        tag.putInt("storedAmount", this.storedAmount);
        tag.putString("storedFluid", Registries.FLUID.getId(this.storedFluid).toString());
        super.writeNbt(tag, registryLookup);
        this.updateFluidTransformation();
    }

    public static void tick(World world, BlockPos pos, TankBlockEntity blockEntity) {
        blockEntity.ensureDisplay(world, pos);
        if (!blockEntity.inventory.getStack(0).isEmpty() && blockEntity.inventory.getStack(1).isEmpty()) {
            ItemStack stack = blockEntity.inventory.getStack(0);
            ActionResult result = ActionResult.PASS;
            if (stack.getItem() instanceof BucketItem) {
                result = blockEntity.interactBucket(null, stack);
            }
            if (stack.getItem() instanceof PotionItem || stack.getItem() instanceof GlassBottleItem) {
                result = blockEntity.interactBottle(null, stack);
            }
            if (result.isAccepted()) {
                blockEntity.inventory.setStack(0, ItemStack.EMPTY);
                blockEntity.inventory.setStack(1, ((ActionResult.Success)result).getNewHandStack());
            }
            else {
                blockEntity.inventory.setStack(0, ItemStack.EMPTY);
                blockEntity.inventory.setStack(1, stack);
            }
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return IntStream.range(1, 2).toArray();
        return IntStream.range(0, 1).toArray();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (!this.inventory.getStack(0).isEmpty()) return false;
        if (stack.getItem() instanceof PotionItem) {
            if (stack.contains(DataComponentTypes.POTION_CONTENTS)) {
                try {
                    return stack.get(DataComponentTypes.POTION_CONTENTS).matches(Potions.WATER);
                } catch (Throwable ignored) {}
            }
            return false;
        }
        return stack.getItem() instanceof BucketItem || stack.getItem() instanceof GlassBottleItem;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 1;
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
        return TankBlockEntity.DISPLAY_COMMAND_TAG;
    }

    private static ItemStack makeStack(String model) {
        var stack = new ItemStack(Items.STICK);
        stack.set(DataComponentTypes.ITEM_MODEL, MoreMechanics.id(model));
        return stack;
    }

    private static final ItemStack WATER_STACK = TankBlockEntity.makeStack("display_water");
    private static final ItemStack LAVA_STACK = TankBlockEntity.makeStack("display_lava");

    @Override
    protected DisplayData getDisplayData(World world, BlockPos pos, int index, DisplayType forType) {
        if (index == 1) return DisplayData.create(this.storedFluid == Fluids.EMPTY ? ItemStack.EMPTY : (this.storedFluid == Fluids.WATER ? TankBlockEntity.WATER_STACK : TankBlockEntity.LAVA_STACK));
        return DisplayData.create(MoreMechanics.Blocks.DUMMY_TANK.getDefaultState());
    }

    private static final DisplaySpec[] DISPLAY_SPECS = { DisplaySpec.BLOCK, DisplaySpec.ITEM };

    @Override
    protected DisplaySpec[] getDisplaySpec() {
        return TankBlockEntity.DISPLAY_SPECS;
    }

    private static final Transformation BLOCK_TRANSFORMATION = DisplayTransformations.getForBlock(null);

    @Override
    protected Transformation getDisplayTransformation(World world, BlockPos pos, int index, DisplayType forType) {
        if (index == 1) return this.fluidTransformation;
        return TankBlockEntity.BLOCK_TRANSFORMATION;
    }

    private static final boolean[] DISPLAY_TICK_ENABLED = { false, true };

    @Override
    protected boolean[] getDisplayTickEnabled() {
        return TankBlockEntity.DISPLAY_TICK_ENABLED;
    }
}