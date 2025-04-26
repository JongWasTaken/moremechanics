package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.block.ExperienceStorageBlock;
import dev.smto.moremechanics.util.Degrees;
import dev.smto.moremechanics.util.DisplayTransformations;
import dev.smto.moremechanics.util.ExperienceUtils;
import dev.smto.moremechanics.util.Transformation;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;

public class ExperienceStorageBlockEntity extends ManagedDisplayBlockEntity implements SidedInventory {
    private static final int MAX_EXPERIENCE = Integer.MAX_VALUE;
    private static final String DISPLAY_COMMAND_TAG = MoreMechanics.id("experience_storage_display").toString();

    private static final HashMap<Direction, Transformation> UPPER_TEXT_TRANSFORMATIONS = new HashMap<>() {{
        this.put(Direction.NORTH, new Transformation().setTranslation(0.5f,1.001f,0.5f).rotateX(Degrees.D90));
        this.put(Direction.EAST, new Transformation().setTranslation(0.5f,1.001f,0.5f).rotateX(Degrees.D90).rotateZ(Degrees.D90));
        this.put(Direction.SOUTH, new Transformation().setTranslation(0.5f,1.001f,0.5f).rotateX(Degrees.D90).rotateZ(Degrees.D180));
        this.put(Direction.WEST, new Transformation().setTranslation(0.5f,1.001f,0.5f).rotateX(Degrees.D90).rotateZ(Degrees.D270));
    }};

    private static final HashMap<Direction, Transformation> LOWER_TEXT_TRANSFORMATIONS = new HashMap<>() {{
        this.put(Direction.NORTH, new Transformation().setTranslation(0.5f,1.001f,0.7f).rotateX(Degrees.D90));
        this.put(Direction.EAST, new Transformation().setTranslation(0.3f,1.001f,0.5f).rotateX(Degrees.D90).rotateZ(Degrees.D90));
        this.put(Direction.SOUTH, new Transformation().setTranslation(0.5f,1.001f,0.3f).rotateX(Degrees.D90).rotateZ(Degrees.D180));
        this.put(Direction.WEST, new Transformation().setTranslation(0.7f,1.001f,0.5f).rotateX(Degrees.D90).rotateZ(Degrees.D270));
    }};

    private final Transformation itemTransformation = DisplayTransformations.getForItem(Direction.NORTH).setScale(0.65f);

    public ExperienceStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.EXPERIENCE_STORAGE, pos, state);
    }

    @Override
    protected String getDisplayCommandTag() {
        return ExperienceStorageBlockEntity.DISPLAY_COMMAND_TAG;
    }

    @Override
    protected Transformation getDisplayTransformation(World world, BlockPos pos, int index, DisplayType forType) {
        if (index == 0) return super.getDisplayTransformation(world, pos, 0, forType);
        if (index == 1) return this.itemTransformation.rotateY(0.01f);
        if (index == 2) return ExperienceStorageBlockEntity.UPPER_TEXT_TRANSFORMATIONS.get(this.direction);
        return ExperienceStorageBlockEntity.LOWER_TEXT_TRANSFORMATIONS.get(this.direction);
    }

    @Override
    protected DisplayData getDisplayData(World world, BlockPos pos, int index, DisplayType forType) {
        if (forType == DisplayType.BLOCK) return DisplayData.create(MoreMechanics.Blocks.DUMMY_EXPERIENCE_STORAGE.getDefaultState());
        if (index == 1) return DisplayData.create(Items.EXPERIENCE_BOTTLE.getDefaultStack());
        if (index == 2) {
            return DisplayData.create(
                    TextDisplayData.create(Text.literal(this.getPrettyAmount()).formatted(Formatting.GREEN), 0)
            );
        }
        return DisplayData.create(
                TextDisplayData.create(Text.literal("Levels").formatted(Formatting.GREEN), 0)
        );
    }

    private static final DisplaySpec[] DISPLAY_SPECS = { DisplaySpec.BLOCK, DisplaySpec.ITEM, DisplaySpec.TEXT, DisplaySpec.TEXT };

    @Override
    protected DisplaySpec[] getDisplaySpec() {
        return ExperienceStorageBlockEntity.DISPLAY_SPECS;
    }

    private final boolean[] displayTickEnabled = { false, true, true, true};

    @Override
    protected boolean[] getDisplayTickEnabled() {
        return this.displayTickEnabled;
    }

    public static void tick(World world, BlockPos pos, ExperienceStorageBlockEntity blockEntity) {
        if (world instanceof ServerWorld w) {
            blockEntity.ensureDisplay(w, pos);
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
        }
    }

    private int storedExperience;
    private Direction direction = Direction.NORTH;

    public void setDirection(Direction direction) {
        this.direction = direction;
        this.markDirty();
    }

    public int getStoredAmount() {
        return this.storedExperience;
    }

    public String getPrettyAmount() {
        String r = "";
        var values = ExperienceUtils.getLevelsAndPoints(this.getStoredAmount());
        double r2 = (double) values.getRight();
        if (r2 > 0.0F && r2 < 1.0F) {
                //r = new DecimalFormat(".00").format(r2);
                r = "." + values.getRight().toString().split("\\.")[1].substring(0,2);
        }
        return values.getLeft() + r;
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
        if(tag.contains("direction")) {
            this.direction = Direction.byId(tag.getInt("direction"));
        }
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("storedExperience", this.storedExperience);
        if (this.direction != null) tag.putInt("direction", this.direction.getId());
        super.writeNbt(tag, DynamicRegistryManager.EMPTY);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[1];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.isOf(MoreMechanics.Items.SOLIDIFIED_EXPERIENCE) || stack.isOf(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return null;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return null;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (this.getWorld() == null) return;
        if (slot == 0) {
            if (stack.isOf(MoreMechanics.Items.SOLIDIFIED_EXPERIENCE)) {
                this.addExperience(10 * stack.getCount());
                stack.setCount(0);
                ExperienceStorageBlock.reopenMenus();
                return;
            }
            if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
                int xp = 0;
                for (int i = 0; i < stack.getCount(); i++) {
                    xp += 3 + this.getWorld().random.nextInt(5) + this.getWorld().random.nextInt(5);
                }
                this.addExperience(xp);
                stack.setCount(0);
                ExperienceStorageBlock.reopenMenus();
                return;
            }
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    @Override
    public void clear() {}
}