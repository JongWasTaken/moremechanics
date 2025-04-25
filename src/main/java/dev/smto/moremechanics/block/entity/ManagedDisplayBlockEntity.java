package dev.smto.moremechanics.block.entity;

import dev.smto.moremechanics.util.ParticleUtils;
import dev.smto.moremechanics.util.Transformation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public abstract class ManagedDisplayBlockEntity extends BlockEntity {

    private final boolean[] defaultTickBehavior;

    protected ManagedDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.defaultTickBehavior = new boolean[this.getDisplaySpec().length];
    }

    private DisplayEntity[] displayEntities;

    protected abstract String getDisplayCommandTag();

    /**
     * Use this to set data for the display entities.<br>
     * DisplayEntities call this once after creation, and after on every tick to update themselves, if enabled with {@link #getDisplayTickEnabled()}.
     * @param world ServerWorld
     * @param pos BlockPos of BlockEntity
     * @param index The index in {@link #getDisplaySpec}
     * @param forType For easier comparison
     * @return DisplayData for the given index. Use {@link DisplayData DisplayData#create(..)}!
     */
    protected abstract DisplayData getDisplayData(World world, BlockPos pos, int index, DisplayType forType);

    /**
     * Use this to set how many/which display entities should be created and managed.<br><br>
     * Example: {@code new DisplaySpec[] { DisplaySpec.BLOCK, DisplaySpec.ITEM, DisplaySpec.TEXT }; },<br>
     * this will spawn 3 display entities: a BlockDisplay at index 0, an ItemDisplay at index 1 and a TextDisplay at index 2.
     */
    protected abstract DisplaySpec[] getDisplaySpec();

    /**
     * Use this to make a display entity update itself every tick.<br><br>
     * Example: {@code new boolean[] { false, true, true }; },<br>
     * this will set the entity at index 1 and 2 to update every tick.<br><br>
     * By default, all entities are set to not tick.
     */
    protected boolean[] getDisplayTickEnabled() {
        return this.defaultTickBehavior;
    };

    protected Transformation getDisplayTransformation(World world, BlockPos pos, int index, DisplayType forType) {
        return Transformation.DEFAULT;
    }

    public void ensureDisplay(World world, BlockPos pos) {
        if (this.displayEntities == null) {
            // clean state
            this.cleanupDisplayEntities(this.getWorld());
            this.displayEntities = new DisplayEntity[this.getDisplaySpec().length];
        }
        for (int i = 0; i < this.getDisplaySpec().length; i++) {
            if (this.displayEntities[i] != null) {
                if (this.getDisplayTickEnabled()[i]) this.getDisplaySpec()[i].update(this.displayEntities[i], this, world, pos, i);
                continue;
            }
            var t = this.getDisplaySpec()[i];
            DisplayEntity ent = t.create(this, world, pos, i);
            t.update(ent, this, world, pos, i);
            if (ent != null) {
                ent.setPosition(pos.getX(), pos.getY(), pos.getZ());
                world.spawnEntity(ent);
                this.displayEntities[i] = ent;
            } else throw new RuntimeException("Unknown display config: " + t);
        }
    }

    @Override
    public void markRemoved() {
        if (this.getWorld() != null) {
            this.cleanupDisplayEntities(this.getWorld());
            ParticleUtils.createBlockBreakParticles((ServerWorld) this.getWorld(), this.getPos(), this.getDisplayData(this.getWorld(), this.getPos(), -1, DisplayType.BLOCK).blockState());
        }
    }

    private void cleanupDisplayEntities(World world) {
        if (this.displayEntities != null) {
            for (DisplayEntity displayEntity : this.displayEntities) {
                if (displayEntity != null) displayEntity.discard();
            }
        }

        if (world instanceof ServerWorld w) {
            w.getEntitiesByClass(
                    DisplayEntity.BlockDisplayEntity.class,
                    new Box(this.getPos()).expand(0.0000001),
                    e -> e.getCommandTags().contains(this.getDisplayCommandTag()) && e.getBlockPos().equals(this.getPos())
            ).forEach(Entity::discard);
            w.getEntitiesByClass(
                    DisplayEntity.ItemDisplayEntity.class,
                    new Box(this.getPos()).expand(0.0000001),
                    e -> e.getCommandTags().contains(this.getDisplayCommandTag()) && e.getBlockPos().equals(this.getPos())
            ).forEach(Entity::discard);
            w.getEntitiesByClass(
                    DisplayEntity.TextDisplayEntity.class,
                    new Box(this.getPos()).expand(0.0000001),
                    e -> e.getCommandTags().contains(this.getDisplayCommandTag()) && e.getBlockPos().equals(this.getPos())
            ).forEach(Entity::discard);
        }
    }

    public static void onPlacedBlock(@NotNull World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) return;
        if (world.getBlockEntity(pos) instanceof ManagedDisplayBlockEntity ent) {
            ent.ensureDisplay(world, pos);
        }
    }

    public enum DisplaySpec {
        BLOCK(
                DisplayType.BLOCK,
                (ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) -> new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world),
                (DisplayEntity ent, ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) -> {
                    ((DisplayEntity.BlockDisplayEntity)ent).setBlockState(blockEntity.getDisplayData(world, pos, index, DisplayType.BLOCK).blockState());
                    ent.setTransformation(blockEntity.getDisplayTransformation(world, pos, index, DisplayType.BLOCK).toVanilla());
                }
        ),
        ITEM(
                DisplayType.ITEM,
                (ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) -> new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world),
                (DisplayEntity ent, ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) -> {
                    ((DisplayEntity.ItemDisplayEntity)ent).setItemStack(blockEntity.getDisplayData(world, pos, index, DisplayType.ITEM).itemStack());
                    ent.setTransformation(blockEntity.getDisplayTransformation(world, pos, index, DisplayType.ITEM).toVanilla());
                }
        ),
        TEXT(
                DisplayType.TEXT,
                (ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) -> new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world),
                (DisplayEntity ent, ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) -> {
                    var textData = blockEntity.getDisplayData(world, pos, index, DisplayType.TEXT).textData();
                    ((DisplayEntity.TextDisplayEntity)ent).setText(textData.text());
                    ((DisplayEntity.TextDisplayEntity)ent).setLineWidth(textData.lineWidth());
                    ((DisplayEntity.TextDisplayEntity)ent).setBackground(textData.backgroundColor());
                    ((DisplayEntity.TextDisplayEntity)ent).setTextOpacity(textData.textOpacity());
                    ((DisplayEntity.TextDisplayEntity)ent).setDisplayFlags(textData.flags());
                    ent.setTransformation(blockEntity.getDisplayTransformation(world, pos, index, DisplayType.TEXT).toVanilla());
                }
        );

        private final DisplayType type;
        private final DisplayCreateFunction creationFunc;
        private final DisplayUpdateFunction updateFunc;

        DisplaySpec(DisplayType type, DisplayCreateFunction creationFunc, DisplayUpdateFunction updateFunc) {
            this.type = type;
            this.creationFunc = creationFunc;
            this.updateFunc = updateFunc;
        }

        public DisplayType getType() {
            return this.type;
        }

        public DisplayEntity create(ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) {
            return this.creationFunc.execute(blockEntity, world, pos, index);
        }

        public void update(DisplayEntity entity, ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index) {
            this.updateFunc.execute(entity, blockEntity, world, pos, index);
        }

        @FunctionalInterface
        public interface DisplayCreateFunction {
            DisplayEntity execute(ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index);
        }
        @FunctionalInterface
        public interface DisplayUpdateFunction {
            void execute(DisplayEntity ent, ManagedDisplayBlockEntity blockEntity, World world, BlockPos pos, int index);
        }

    }

    public enum DisplayType {
        BLOCK,
        ITEM,
        TEXT
    }

    protected record DisplayData(@NotNull BlockState blockState, @NotNull ItemStack itemStack, @NotNull TextDisplayData textData) {
        public static final DisplayData INVALID = new DisplayData(Blocks.BEDROCK.getDefaultState(), Items.BARRIER.getDefaultStack(), TextDisplayData.DEFAULT);
        public static DisplayData create(BlockState blockState) {
            return new DisplayData(blockState, DisplayData.INVALID.itemStack(), DisplayData.INVALID.textData());
        }
        public static DisplayData create(ItemStack itemStack) {
            return new DisplayData(DisplayData.INVALID.blockState(), itemStack, DisplayData.INVALID.textData());
        }
        public static DisplayData create(TextDisplayData textData) {
            return new DisplayData(DisplayData.INVALID.blockState(), DisplayData.INVALID.itemStack(), textData);
        }
    }

    protected record TextDisplayData(@NotNull Text text, @NotNull Integer lineWidth, @NotNull Integer backgroundColor, @NotNull Byte textOpacity, @NotNull Byte flags) {
        public static final TextDisplayData DEFAULT = new TextDisplayData(Text.literal("INVALID").formatted(Formatting.RED), 200, 1073741824, (byte)-1, (byte)0);
        public static TextDisplayData create(Text text) {
            return new TextDisplayData(
                    text,
                    TextDisplayData.DEFAULT.lineWidth(),
                    TextDisplayData.DEFAULT.backgroundColor(),
                    TextDisplayData.DEFAULT.textOpacity(),
                    TextDisplayData.DEFAULT.flags()
            );
        }

        public static TextDisplayData create(Text text, int backgroundColor) {
            return new TextDisplayData(
                    text,
                    TextDisplayData.DEFAULT.lineWidth(),
                    backgroundColor,
                    TextDisplayData.DEFAULT.textOpacity(),
                    TextDisplayData.DEFAULT.flags()
            );
        }

        public static TextDisplayData create(Text text, int backgroundColor, byte textOpacity) {
            return new TextDisplayData(
                    text,
                    TextDisplayData.DEFAULT.lineWidth(),
                    backgroundColor,
                    textOpacity,
                    TextDisplayData.DEFAULT.flags()
            );
        }
    }
}
