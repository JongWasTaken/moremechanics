package dev.smto.moremechanics.block;

import com.mojang.authlib.GameProfile;
import dev.smto.moremechanics.MoreMechanics;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class MechanicalUserBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory {
    protected DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);

    private DisplayEntity.ItemDisplayEntity displayEntityBase;
    private FakePlayer fakePlayer;
    public MechanicalUserBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.CHUNK_LOADER_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, MechanicalUserBlockEntity blockEntity) {
        if (world instanceof ServerWorld w) {
            var d = world.getBlockState(pos).get(MechanicalUserBlock.FACING);
            if (blockEntity.fakePlayer == null) blockEntity.fakePlayer = FakePlayer.get(w, new GameProfile(FakePlayer.DEFAULT_UUID, "MechanicalUserFakePlayer"));
            blockEntity.initDisplayEntity(w);
            double xv = (double) world.random.nextBetween(0, 5) / 10;
            double yv = (double) world.random.nextBetween(0, 5) / 10;
            double zv = (double) world.random.nextBetween(0, 5) / 10;
            if (world.random.nextBoolean()) xv *= -1;
            if (world.random.nextBoolean()) yv *= -1;
            if (world.random.nextBoolean()) zv *= -1;
            world.getServer().getPlayerManager()
                    .sendToAround(
                            null,
                            pos.getX(), pos.getY(), pos.getZ(),
                            10,
                            world.getRegistryKey(),
                            new ParticleS2CPacket(ParticleTypes.REVERSE_PORTAL, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (float) xv, (float)yv, (float) zv, 0.01f, 1)
                    );
            if (!blockEntity.inventory.isEmpty()) {
                var item = blockEntity.findNextStack();
                if (world.getBlockState(pos.offset(d)).isAir() || !(item.getItem() instanceof BlockItem)) {
                    var blockHitResult = w.raycast(new RaycastContext(
                            pos.offset(d).toCenterPos(), pos.offset(d, 1).toCenterPos(), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, blockEntity.fakePlayer
                    ));
                    item.useOnBlock(new ItemUsageContext(w, blockEntity.fakePlayer, Hand.MAIN_HAND, item, blockHitResult));
                }
            }
        }
    }

    private ItemStack findNextStack() {
        for (ItemStack itemStack : this.inventory) {
            if (!itemStack.isEmpty()) return itemStack;
        }
        return ItemStack.EMPTY;
    }

    public void initDisplayEntity(ServerWorld world) {
        if (this.displayEntityBase == null) {
            this.displayEntityBase = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            this.displayEntityBase.setItemStack(new ItemStack(MoreMechanics.Items.CHUNK_LOADER));
            this.displayEntityBase.setBrightness(Brightness.FULL);
            this.displayEntityBase.setShadowStrength(0.0F);
            this.displayEntityBase.setShadowRadius(0.0f);
            this.displayEntityBase.setPosition(this.getPos().toCenterPos());
            this.displayEntityBase.setTransformation(new AffineTransformation(
                    null,
                    AffineTransformations.DIRECTION_ROTATIONS.get(world.getBlockState(this.pos).get(MechanicalUserBlock.FACING)).getLeftRotation(),
                    new Vector3f(1.0f,1.0f,1.0f),
                    null
            ));
            world.spawnEntity(this.displayEntityBase);
        }
    }


    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, DynamicRegistryManager.EMPTY);
        Inventories.readNbt(tag, this.inventory, BuiltinRegistries.createWrapperLookup());

    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(tag, DynamicRegistryManager.EMPTY);
        Inventories.writeNbt(tag, this.inventory, BuiltinRegistries.createWrapperLookup());
    }

    @Override
    public void markRemoved() {
        if (this.displayEntityBase != null) this.displayEntityBase.discard();
        if (this.getWorld() instanceof ServerWorld w) w.getEntitiesByClass(DisplayEntity.ItemDisplayEntity.class, new Box(this.getPos()), e -> true).forEach(Entity::discard);
        this.dropInventory();
        super.markRemoved();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[9];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return !(stack.getItem() instanceof BlockItem);
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

    public void dropInventory() {
        this.inventory.forEach(item -> {
            try {
                if (this.world != null) this.world.spawnEntity(new ItemEntity(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), item));
            } catch (Throwable ignored) {}
        });
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
        return Text.translatable("block.g3s.mechanical_user");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new Generic3x3ContainerScreenHandler(syncId, playerInventory, this);
    }
}