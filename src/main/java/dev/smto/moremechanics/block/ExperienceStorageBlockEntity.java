package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ExperienceStorageBlockEntity extends BlockEntity {
    private static final int MAX_EXPERIENCE = Integer.MAX_VALUE;
    private DisplayEntity.ItemDisplayEntity displayEntityBase;
    private DisplayEntity.ItemDisplayEntity displayEntityInner;
    private float innerYaw;
    private boolean initialized;
    public ExperienceStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.EXPERIENCE_STORAGE_ENTITY, pos, state);
    }

    public void setInitialized() {
        this.initialized = true;
    }

    public static void tick(World world, BlockPos pos, ExperienceStorageBlockEntity blockEntity) {
        if (world instanceof ServerWorld w) {
            blockEntity.initDisplayEntity(w, null);
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
            /*
            try {
                float yaw = blockEntity.displayEntityInner.getYaw();
                if (yaw > 360.0F) yaw = 0.0F;
                blockEntity.displayEntityInner.setYaw(yaw + 0.4F);
            } catch (Throwable ignored) {}
             */
        }
    }

    public void initDisplayEntity(ServerWorld world, @Nullable ServerPlayerEntity player) {
        if (!this.initialized) return;
        if (this.displayEntityBase == null) {
            this.displayEntityBase = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            this.displayEntityBase.setItemStack(new ItemStack(MoreMechanics.Items.EXPERIENCE_STORAGE));
            this.displayEntityBase.setBrightness(Brightness.FULL);
            this.displayEntityBase.setShadowStrength(0.0F);
            this.displayEntityBase.setPosition(this.getPos().toCenterPos());
            world.spawnEntity(this.displayEntityBase);
        }

        if (this.displayEntityInner == null) {
            this.displayEntityInner = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            this.displayEntityInner.setItemStack(new ItemStack(Items.ENDER_EYE));
            this.displayEntityInner.setBrightness(Brightness.FULL);
            this.displayEntityInner.setShadowStrength(0.0F);
            this.displayEntityInner.setPosition(this.getPos().toCenterPos());
            world.spawnEntity(this.displayEntityInner);
            if (player != null) {
                float yaw = player.getHeadYaw() + 180.0F;
                if (yaw > 360.0F) {
                    yaw -= 360.0F;
                }
                this.innerYaw = yaw;
            }
            this.displayEntityInner.setYaw(this.innerYaw);
        }
    }

    private int storedExperience;

    public int getStoredAmount() {
        return this.storedExperience;
    }

    public int addExperience(int amount) {
        int add = Math.min(Math.max(0, amount), ExperienceStorageBlockEntity.MAX_EXPERIENCE - this.storedExperience);
        this.storedExperience += add;
        this.markDirty();
        return add;
    }

    public int takeExperience(int amount) {
        int take = Math.max(0, Math.min(amount, this.storedExperience));
        this.storedExperience -= take;
        this.markDirty();
        return take;
    }

    public void dropExperience(ServerWorld world, BlockPos pos) {
        if (this.storedExperience > 0) {
            ExperienceOrbEntity.spawn(world, pos.toCenterPos(), this.storedExperience);
        }
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, DynamicRegistryManager.EMPTY);
        if(tag.contains("storedExperience")) {
            this.storedExperience = tag.getInt("storedExperience");
        }
        if(tag.contains("innerYaw")) {
            this.innerYaw = tag.getFloat("innerYaw");
            if (this.displayEntityInner != null) {
                this.displayEntityInner.setYaw(this.innerYaw);
            }
        }
        this.initialized = true;
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("storedExperience", this.storedExperience);
        tag.putFloat("innerYaw", this.innerYaw);
        super.writeNbt(tag, DynamicRegistryManager.EMPTY);
    }

    @Override
    public void markRemoved() {
        if (this.displayEntityBase != null) this.displayEntityBase.discard();
        if (this.displayEntityInner != null) this.displayEntityInner.discard();
        if (this.getWorld() instanceof ServerWorld w) w.getEntitiesByClass(DisplayEntity.ItemDisplayEntity.class, new Box(this.getPos()), e -> true).forEach(Entity::discard);
        super.markRemoved();
    }

}