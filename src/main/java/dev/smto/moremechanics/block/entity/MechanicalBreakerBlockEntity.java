package dev.smto.moremechanics.block.entity;

import com.mojang.authlib.GameProfile;
import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.block.MechanicalBreakerBlock;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.IntStream;

public class MechanicalBreakerBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory {

    protected DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);
    private FakePlayer fakePlayer;
    private int cooldown = -1;

    public MechanicalBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.MECHANICAL_BREAKER, pos, state);
    }

    public static void tick(World world, BlockPos pos, MechanicalBreakerBlockEntity blockEntity) {
        if (world instanceof ServerWorld w) {
            if (blockEntity.fakePlayer == null) blockEntity.fakePlayer = FakePlayer.get(w, new GameProfile(FakePlayer.DEFAULT_UUID, "MechanicalBreakerFakePlayer"));
            if (world.getBlockState(pos).get(MechanicalBreakerBlock.POWERED)) return;
            if (blockEntity.inventory.isEmpty()) return;

            var d = world.getBlockState(pos).get(MechanicalBreakerBlock.FACING);
            var blockHitResult = w.raycast(new RaycastContext(
                    pos.toCenterPos().offset(d, 0.501), pos.toCenterPos().offset(d, 1), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, blockEntity.fakePlayer
            ));

            if (blockEntity.cooldown == 0) {
                if (blockEntity.fakePlayer.interactionManager.tryBreakBlock(blockHitResult.getBlockPos())) {
                    blockEntity.cooldown = -1;
                }
            } else if (blockEntity.cooldown == -1) {
                if (blockHitResult.isInsideBlock()) {
                    var item = blockEntity.findNextStack();
                    blockEntity.fakePlayer.setStackInHand(Hand.MAIN_HAND, item);
                    if (item.getComponents().contains(DataComponentTypes.TOOL)) {
                        var toolComponent = Objects.requireNonNull(item.get(DataComponentTypes.TOOL));
                        var state = world.getBlockState(blockHitResult.getBlockPos());
                        if (toolComponent.isCorrectForDrops(state)) {
                            double requiredTicks = (state.getHardness(w, pos) * 1.5) * 20;
                            float toolSpeed = toolComponent.getSpeed(state);
                            int level = 0;
                            for (RegistryEntry<Enchantment> enchantment : EnchantmentHelper.getEnchantments(item).getEnchantments()) {
                                if (enchantment.getKey().isEmpty()) continue;
                                if (enchantment.getKey().get().equals(Enchantments.EFFICIENCY)) {
                                    level = EnchantmentHelper.getLevel(enchantment, item);
                                    break;
                                }
                            }
                            if (level > 0) {
                                toolSpeed = toolSpeed + (level * level + 1);
                            }
                            requiredTicks = requiredTicks / toolSpeed;
                            blockEntity.cooldown = (int) requiredTicks;
                        }
                    }
                }
            } else blockEntity.cooldown--;
        }
    }

    private ItemStack findNextStack() {
        for (ItemStack itemStack : this.inventory) {
            if (!itemStack.isEmpty()) return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, registryLookup);
        Inventories.readNbt(tag, this.inventory, registryLookup);
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(tag, registryLookup);
        Inventories.writeNbt(tag, this.inventory, registryLookup);
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    private final int[] AVAILABLE_SLOTS = IntStream.range(0, 9).toArray();

    @Override
    public int[] getAvailableSlots(Direction side) {
        return this.AVAILABLE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int size() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack itemStack = Inventories.splitStack(this.inventory, slot, amount);
        if (!itemStack.isEmpty()) {
            this.markDirty();
        }
        return itemStack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        //ItemStack itemStack = this.inventory.get(slot);
        //boolean bl = !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(itemStack, stack);
        this.inventory.set(slot, stack);
        stack.capCount(this.getMaxCount(stack));
        this.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.moremechanics.mechanical_breaker");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new Generic3x3ContainerScreenHandler(syncId, playerInventory, this);
    }
}