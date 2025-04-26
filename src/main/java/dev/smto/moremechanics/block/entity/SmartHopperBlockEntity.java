package dev.smto.moremechanics.block.entity;

import java.util.HashMap;
import java.util.List;
import java.util.function.BooleanSupplier;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.util.GuiUtils;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SmartHopperBlockEntity extends LootableContainerBlockEntity implements Hopper {
    public static final int TRANSFER_COOLDOWN = 4;
    public static final int INVENTORY_SIZE = 5;
    private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private ItemStack filter = ItemStack.EMPTY;
    private int transferCooldown = -1;
    private long lastTickTime;
    private Direction facing;
    private FilterType filterType = FilterType.NONE;

    public SmartHopperBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.SMART_HOPPER, pos, state);
        this.facing = state.get(HopperBlock.FACING);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (!this.readLootTable(nbt)) {
            Inventories.readNbt(nbt, this.inventory, registries);
        }
        ItemStack.fromNbt(registries, nbt.getCompound("Filter")).ifPresent(this::setFilter);
        this.transferCooldown = nbt.getInt("TransferCooldown");
        if (nbt.contains("FilterType")) this.filterType = FilterType.values()[nbt.getInt("FilterType")];
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (!this.writeLootTable(nbt)) {
            Inventories.writeNbt(nbt, this.inventory, registries);
        }
        if (!this.filter.isEmpty()) nbt.put("Filter", this.filter.toNbt(registries));
        nbt.putInt("FilterType", this.filterType.ordinal());
        nbt.putInt("TransferCooldown", this.transferCooldown);
    }

    public ItemStack getFilter() {
        return this.filter;
    }

    public void setFilter(ItemStack stack) {
        this.filter = stack;
        this.markDirty();
    }

    public FilterType getFilterType() {
        return this.filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
        this.markDirty();
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        this.generateLoot(null);
        return Inventories.splitStack(this.getHeldStacks(), slot, amount);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.generateLoot(null);
        this.getHeldStacks().set(slot, stack);
        stack.capCount(this.getMaxCount(stack));
    }

    @Override
    public void setCachedState(BlockState state) {
        super.setCachedState(state);
        this.facing = state.get(HopperBlock.FACING);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.moremechanics.smart_hopper");
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, SmartHopperBlockEntity blockEntity) {
        blockEntity.transferCooldown--;
        blockEntity.lastTickTime = world.getTime();
        if (!blockEntity.needsCooldown()) {
            blockEntity.setTransferCooldown(0);
            SmartHopperBlockEntity.insertAndExtract(world, pos, state, blockEntity, () -> SmartHopperBlockEntity.extract(world, blockEntity));
        }
    }

    private static boolean insertAndExtract(World world, BlockPos pos, BlockState state, SmartHopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClient) {
            return false;
        } else {
            if (!blockEntity.needsCooldown() && state.get(HopperBlock.ENABLED)) {
                boolean bl = false;
                if (!blockEntity.isEmpty()) {
                    bl = SmartHopperBlockEntity.insert(world, pos, blockEntity);
                }

                if (!blockEntity.isFull()) {
                    bl |= booleanSupplier.getAsBoolean();
                }

                if (bl) {
                    blockEntity.setTransferCooldown(SmartHopperBlockEntity.TRANSFER_COOLDOWN);
                    BlockEntity.markDirty(world, pos, state);
                    return true;
                }
            }

            return false;
        }
    }

    private boolean isFull() {
        for (ItemStack itemStack : this.inventory) {
            if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxCount()) {
                return false;
            }
        }

        return true;
    }

    // take stack from hopper and insert into another inv
    private static boolean insert(World world, BlockPos pos, SmartHopperBlockEntity blockEntity) {
        Inventory inventory = SmartHopperBlockEntity.getOutputInventory(world, pos, blockEntity);
        if (inventory == null) {
            return false;
        } else {
            Direction direction = blockEntity.facing.getOpposite();
            if (SmartHopperBlockEntity.isInventoryFull(inventory, direction)) {
                return false;
            } else {
                for (int i = 0; i < blockEntity.size(); i++) {
                    ItemStack itemStack = blockEntity.getStack(i);
                    if (!itemStack.isEmpty()) {
                        // filtering
                        if (!blockEntity.getFilter().isEmpty()) {
                            if (!blockEntity.getFilterType().check(itemStack, blockEntity.getFilter())) continue;
                        }
                        int j = itemStack.getCount();
                        ItemStack itemStack2 = SmartHopperBlockEntity.transfer(blockEntity, inventory, blockEntity.removeStack(i, 1), direction);
                        if (itemStack2.isEmpty()) {
                            inventory.markDirty();
                            return true;
                        }

                        itemStack.setCount(j);
                        if (j == 1) {
                            blockEntity.setStack(i, itemStack);
                        }
                    }
                }

                return false;
            }
        }
    }

    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        if (inventory instanceof SidedInventory sidedInventory) {
            return sidedInventory.getAvailableSlots(side);
        } else {
            int i = inventory.size();
            if (i < SmartHopperBlockEntity.AVAILABLE_SLOTS_CACHE.length) {
                int[] is = SmartHopperBlockEntity.AVAILABLE_SLOTS_CACHE[i];
                if (is != null) {
                    return is;
                } else {
                    int[] js = SmartHopperBlockEntity.indexArray(i);
                    SmartHopperBlockEntity.AVAILABLE_SLOTS_CACHE[i] = js;
                    return js;
                }
            } else {
                return SmartHopperBlockEntity.indexArray(i);
            }
        }
    }

    private static int[] indexArray(int size) {
        int[] is = new int[size];
        int i = 0;

        while (i < is.length) {
            is[i] = i++;
        }

        return is;
    }

    private static boolean isInventoryFull(Inventory inventory, Direction direction) {
        int[] is = SmartHopperBlockEntity.getAvailableSlots(inventory, direction);

        for (int i : is) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.getCount() < itemStack.getMaxCount()) {
                return false;
            }
        }

        return true;
    }

    public static boolean extract(World world, Hopper hopper) {
        BlockPos blockPos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
        BlockState blockState = world.getBlockState(blockPos);
        Inventory inventory = SmartHopperBlockEntity.getInputInventory(world, hopper, blockPos, blockState);
        if (inventory != null) {
            Direction direction = Direction.DOWN;

            for (int i : SmartHopperBlockEntity.getAvailableSlots(inventory, direction)) {
                if (SmartHopperBlockEntity.extract(hopper, inventory, i, direction)) {
                    return true;
                }
            }

            return false;
        } else {
            boolean bl = hopper.canBlockFromAbove() && blockState.isFullCube(world, blockPos) && !blockState.isIn(BlockTags.DOES_NOT_BLOCK_HOPPERS);
            if (!bl) {
                for (ItemEntity itemEntity : SmartHopperBlockEntity.getInputItemEntities(world, hopper)) {
                    if (SmartHopperBlockEntity.extract(hopper, itemEntity)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side) {
        ItemStack itemStack = inventory.getStack(slot);
        if (!itemStack.isEmpty() && SmartHopperBlockEntity.canExtract(hopper, inventory, itemStack, slot, side)) {
            int i = itemStack.getCount();
            ItemStack itemStack2 = SmartHopperBlockEntity.transfer(inventory, hopper, inventory.removeStack(slot, 1), null);
            if (itemStack2.isEmpty()) {
                inventory.markDirty();
                return true;
            }

            itemStack.setCount(i);
            if (i == 1) {
                inventory.setStack(slot, itemStack);
            }
        }

        return false;
    }

    // hopper gets stack from physical item, then discards it
    public static boolean extract(Inventory inventory, ItemEntity itemEntity) {
        boolean bl = false;
        ItemStack itemStack = itemEntity.getStack().copy();
        ItemStack itemStack2 = SmartHopperBlockEntity.transfer(null, inventory, itemStack, null);
        if (itemStack2.isEmpty()) {
            bl = true;
            itemEntity.setStack(net.minecraft.item.ItemStack.EMPTY);
            itemEntity.discard();
        } else {
            itemEntity.setStack(itemStack2);
        }

        return bl;
    }

    public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
        if (to instanceof SidedInventory sidedInventory && side != null) {
            int[] is = sidedInventory.getAvailableSlots(side);

            for (int i = 0; i < is.length && !stack.isEmpty(); i++) {
                stack = SmartHopperBlockEntity.transfer(from, to, stack, is[i], side);
            }

            return stack;
        }

        int j = to.size();

        for (int i = 0; i < j && !stack.isEmpty(); i++) {
            stack = SmartHopperBlockEntity.transfer(from, to, stack, i, side);
        }

        return stack;
    }

    private static boolean canInsert(Inventory inventory, ItemStack stack, int slot, @Nullable Direction side) {
        if (!inventory.isValid(slot, stack)) {
            return false;
        } else {
            if (inventory instanceof SidedInventory sidedInventory && !sidedInventory.canInsert(slot, stack, side)) {
                return false;
            }

            return true;
        }
    }

    private static boolean canExtract(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing) {
        if (!fromInventory.canTransferTo(hopperInventory, slot, stack)) {
            return false;
        } else {
            return !(fromInventory instanceof SidedInventory sidedInventory) || sidedInventory.canExtract(slot, stack, facing);
        }
    }

    private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction side) {
        ItemStack itemStack = to.getStack(slot);
        if (SmartHopperBlockEntity.canInsert(to, stack, slot, side)) {
            boolean bl = false;
            boolean bl2 = to.isEmpty();
            if (itemStack.isEmpty()) {
                to.setStack(slot, stack);
                stack = net.minecraft.item.ItemStack.EMPTY;
                bl = true;
            } else if (SmartHopperBlockEntity.canMergeItems(itemStack, stack)) {
                int i = stack.getMaxCount() - itemStack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.decrement(j);
                itemStack.increment(j);
                bl = j > 0;
            }

            if (bl) {
                if (bl2 && to instanceof SmartHopperBlockEntity SpeedHopperBlockEntity && !SpeedHopperBlockEntity.isDisabled()) {
                    int j = 0;
                    if (from instanceof SmartHopperBlockEntity hopperBlockEntity2 && SpeedHopperBlockEntity.lastTickTime >= hopperBlockEntity2.lastTickTime) {
                        j = 1;
                    }

                    SpeedHopperBlockEntity.setTransferCooldown(SmartHopperBlockEntity.TRANSFER_COOLDOWN - j);
                }

                to.markDirty();
            }
        }

        return stack;
    }

    @Nullable
    private static Inventory getOutputInventory(World world, BlockPos pos, SmartHopperBlockEntity blockEntity) {
        return SmartHopperBlockEntity.getInventoryAt(world, pos.offset(blockEntity.facing));
    }

    @Nullable
    private static Inventory getInputInventory(World world, Hopper hopper, BlockPos pos, BlockState state) {
        return SmartHopperBlockEntity.getInventoryAt(world, pos, state, hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
    }

    public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
        Box box = hopper.getInputAreaShape().offset(hopper.getHopperX() - 0.5, hopper.getHopperY() - 0.5, hopper.getHopperZ() - 0.5);
        return world.getEntitiesByClass(ItemEntity.class, box, EntityPredicates.VALID_ENTITY);
    }

    @Nullable
    public static Inventory getInventoryAt(World world, BlockPos pos) {
        return SmartHopperBlockEntity.getInventoryAt(world, pos, world.getBlockState(pos), (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
    }

    @Nullable
    private static Inventory getInventoryAt(World world, BlockPos pos, BlockState state, double x, double y, double z) {
        Inventory inventory = SmartHopperBlockEntity.getBlockInventoryAt(world, pos, state);
        if (inventory == null) {
            inventory = SmartHopperBlockEntity.getEntityInventoryAt(world, x, y, z);
        }

        return inventory;
    }

    @Nullable
    private static Inventory getBlockInventoryAt(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof InventoryProvider) {
            return ((InventoryProvider)block).getInventory(state, world, pos);
        } else if (state.hasBlockEntity() && world.getBlockEntity(pos) instanceof Inventory inventory) {
            if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                inventory = ChestBlock.getInventory((ChestBlock)block, state, world, pos, true);
            }

            return inventory;
        } else {
            return null;
        }
    }

    @Nullable
    private static Inventory getEntityInventoryAt(World world, double x, double y, double z) {
        List<Entity> list = world.getOtherEntities((Entity)null, new Box(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntityPredicates.VALID_INVENTORIES);
        return !list.isEmpty() ? (Inventory)list.get(world.random.nextInt(list.size())) : null;
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        return first.getCount() <= first.getMaxCount() && net.minecraft.item.ItemStack.areItemsAndComponentsEqual(first, second);
    }

    @Override
    public double getHopperX() {
        return (double)this.pos.getX() + 0.5;
    }

    @Override
    public double getHopperY() {
        return (double)this.pos.getY() + 0.5;
    }

    @Override
    public double getHopperZ() {
        return (double)this.pos.getZ() + 0.5;
    }

    @Override
    public boolean canBlockFromAbove() {
        return true;
    }

    private void setTransferCooldown(int transferCooldown) {
        this.transferCooldown = transferCooldown;
    }

    private boolean needsCooldown() {
        return this.transferCooldown > 0;
    }

    private boolean isDisabled() {
        return this.transferCooldown > 8;
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    public void openFakeScreen(ServerPlayerEntity player) {
        Gui.open(this, player);
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new HopperScreenHandler(syncId, playerInventory, this);
    }

    private static class Gui extends SimpleGui {
        public static void open(SmartHopperBlockEntity blockEntity, ServerPlayerEntity player) {
            new SmartHopperBlockEntity.Gui(blockEntity, player).open();
        }

        private final SmartHopperBlockEntity blockEntity;

        public Gui(SmartHopperBlockEntity blockEntity, ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X1, player, false);
            this.blockEntity = blockEntity;
        }

        @Override
        public GuiElementInterface getSlot(int index) {
            var gui = super.getSlot(index);
            if (gui == null) gui = () -> ItemStack.EMPTY;
            return gui;
        }

        /*
        @Override
        public ItemStack quickMove(int index) {
            ItemStack newStack = ItemStack.EMPTY;
            Slot slot = this.getSlotRedirectOrPlayer(index);
            if (slot != null && slot.hasStack() && !(slot instanceof VirtualSlot)) {
                ItemStack sourceStack = slot.getStack();
                //if (true) return sourceStack;
                if (sourceStack.isEmpty()) return sourceStack;
                newStack = sourceStack.copy();
                for (int i = 0; i < this.getSize(); i++) {
                    if (this.getSlot(i).getItemStack().isEmpty()) {
                        this.setSlot(i,sourceStack);
                        slot.markDirty();
                        return ItemStack.EMPTY;
                    }
                }

            } else if (slot instanceof VirtualSlot) {
                return slot.getStack();
            }

            return newStack;
        }
        */

        @Override
        public boolean onClick(int index, ClickType type, SlotActionType action, GuiElementInterface element) {
            if (index > 4 && index != 8) return false;
            ItemStack cursorStack = this.player.currentScreenHandler.getCursorStack();
            ItemStack slotStack = this.getSlot(index).getItemStack();
            if (cursorStack.isEmpty()) {
                if (type.isRight) {
                    int count = slotStack.getCount() / 2;
                    cursorStack = slotStack.copyWithCount(count);
                    slotStack.decrement(count);
                }
                else if (type.isLeft && !type.shift) {
                    cursorStack = slotStack.copy();
                    slotStack = ItemStack.EMPTY;
                }
                /*
                else if (type.isLeft) { // with shift
                    MoreMechanics.LOGGER.warn("yep");
                    this.quickMove(index);
                    this.player.currentScreenHandler.sendContentUpdates();
                    this.player.currentScreenHandler.updateToClient();
                    return true;
                }
                */
            }
            else {
                if (type.isRight) {
                    if (slotStack.isEmpty()) slotStack = cursorStack.copyWithCount(1);
                    else slotStack.increment(1);
                    cursorStack.decrement(1);
                }
                else if (type.isLeft && !type.shift) {
                    if (slotStack.isEmpty()) {
                        slotStack = cursorStack.copy();
                        cursorStack = ItemStack.EMPTY;
                    }
                    else {
                        if (ItemStack.areItemsEqual(cursorStack, slotStack)) {
                            int count = Math.min(cursorStack.getCount(), slotStack.getMaxCount() - slotStack.getCount());
                            slotStack.increment(count);
                            cursorStack.decrement(count);
                        } else {
                            ItemStack temp = slotStack.copy();
                            slotStack = cursorStack.copy();
                            cursorStack = temp.copy();
                        }
                    }
                }
                /*
                else if (type.isLeft) { // with shift
                    this.quickMove(index);
                }
                 */
            }

            this.setSlot(index, slotStack);
            this.player.currentScreenHandler.setCursorStack(cursorStack);
            this.save();
            return super.onClick(index, type, action, element);
        }

        private static final HashMap<FilterType, GuiElementBuilder> FILTER_TOGGLES = new HashMap<>() {{
            this.put(FilterType.NONE, GuiElementBuilder.from(Items.BLACK_STAINED_GLASS_PANE.getDefaultStack())
                    .setItemName(Text.translatable("gui.moremechanics.smart_hopper.filter_type.none"))
                    .hideDefaultTooltip()
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.Letters.X)
                    .setLore(List.of(
                            Text.translatable("tooltip.moremechanics.smart_hopper.filter_type.none.description").formatted(MoreMechanics.getTooltipFormatting()),
                            Text.translatable("tooltip.moremechanics.smart_hopper.filter_type.all.description").formatted(MoreMechanics.getTooltipFormatting())
                    ))
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        gui.setSlot(7, Gui.FILTER_TOGGLES.get(FilterType.ITEM).build());
                        ((Gui)gui).blockEntity.setFilterType(FilterType.ITEM);
                        ((Gui)gui).blockEntity.markDirty();
                    })
            );
            this.put(FilterType.ITEM, GuiElementBuilder.from(Items.BLACK_STAINED_GLASS_PANE.getDefaultStack())
                    .setItemName(Text.translatable("gui.moremechanics.smart_hopper.filter_type.item"))
                    .hideDefaultTooltip()
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.Shapes.ITEM)
                    .setLore(List.of(
                            Text.translatable("tooltip.moremechanics.smart_hopper.filter_type.item.description").formatted(MoreMechanics.getTooltipFormatting()),
                            Text.translatable("tooltip.moremechanics.smart_hopper.filter_type.all.description").formatted(MoreMechanics.getTooltipFormatting())
                    ))
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        gui.setSlot(7, Gui.FILTER_TOGGLES.get(FilterType.STACK).build());
                        ((Gui)gui).blockEntity.setFilterType(FilterType.STACK);
                        ((Gui)gui).blockEntity.markDirty();
                    })
            );
            this.put(FilterType.STACK, GuiElementBuilder.from(Items.BLACK_STAINED_GLASS_PANE.getDefaultStack())
                    .setItemName(Text.translatable("gui.moremechanics.smart_hopper.filter_type.stack"))
                    .hideDefaultTooltip()
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.Shapes.STACK)
                    .setLore(List.of(
                            Text.translatable("tooltip.moremechanics.smart_hopper.filter_type.stack.description").formatted(MoreMechanics.getTooltipFormatting()),
                            Text.translatable("tooltip.moremechanics.smart_hopper.filter_type.all.description").formatted(MoreMechanics.getTooltipFormatting())
                    ))
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        gui.setSlot(7, Gui.FILTER_TOGGLES.get(FilterType.NONE).build());
                        ((Gui)gui).blockEntity.setFilterType(FilterType.NONE);
                        ((Gui)gui).blockEntity.markDirty();
                    })
            );
        }};

        @Override
        public void beforeOpen() {
            for (ItemStack itemStack : this.blockEntity.inventory) {
                this.addSlot(itemStack);
            }
            this.addSlot(GuiUtils.Elements.FILLER);
            this.addSlot(GuiUtils.Elements.FILLER);
            this.addSlot(Gui.FILTER_TOGGLES.get(this.blockEntity.getFilterType()).build());
            this.addSlot(this.blockEntity.getFilter());

            this.setTitle(Text.translatable("block.moremechanics.smart_hopper"));
        }

        private void save() {
            for (int i = 0; i < 5; i++) {
                this.blockEntity.setStack(i, this.getSlot(i).getItemStack());
            }
            this.blockEntity.setFilter(this.getSlot(8).getItemStack());
            this.blockEntity.markDirty();
        }

        @Override
        public void onClose() {
            this.save();
            super.onClose();
        }
    }

    public enum FilterType {
        NONE((s,f) -> true),
        ITEM(ItemStack::areItemsEqual),
        STACK(ItemStack::areItemsAndComponentsEqual);

        private final FilterTypeCheck check;

        FilterType(FilterTypeCheck check) {
            this.check = check;
        }

        public boolean check(ItemStack stack, ItemStack filter) {
            return this.check.check(stack, filter);
        }
    }

    @FunctionalInterface
    public interface FilterTypeCheck {
        boolean check(ItemStack stack, ItemStack filter);
    }
}
