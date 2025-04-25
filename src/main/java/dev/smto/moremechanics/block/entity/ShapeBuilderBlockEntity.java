package dev.smto.moremechanics.block.entity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import java.util.*;

import dev.smto.moremechanics.MoreMechanics;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import dev.smto.moremechanics.util.ParticleUtils;
import dev.smto.moremechanics.util.shape.ShapeGenerator;
import dev.smto.moremechanics.util.shape.ShapeType;
import dev.smto.moremechanics.util.shape.Shapeable;

import static dev.smto.moremechanics.util.shape.ShapeUtils.lengthSq;

// A lot of this + dev.smto.moremechanics.util.shape.* has been kidnapped from OpenBlocks, which has not been updated since 2019 (MIT license)!
public class ShapeBuilderBlockEntity extends BlockEntity {

    public ShapeBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(MoreMechanics.BlockEntities.SHAPE_BUILDER, pos, state);
    }

    private static final Comparator<BlockPos> COMPARATOR = (o1, o2) -> {
        {
            // first, go from bottom to top
            int result1 = Ints.compare(o1.getX(), o2.getX());
            if (result1 != 0) return result1;
        }

        {
            // then sort by angle, to make placement more intuitive
            double angle1 = Math.atan2(o1.getZ(), o1.getX());
            double angle2 = Math.atan2(o2.getZ(), o2.getX());

            int result2 = Doubles.compare(angle1, angle2);
            if (result2 != 0) return result2;
        }

        {
            // then sort by distance, far ones first
            double length1 = lengthSq(o1.getX(), o1.getZ());
            double length2 = lengthSq(o2.getX(), o2.getZ());

            int result3 = Doubles.compare(length2, length1);
            if (result3 != 0) return result3;
        }

        // then sort by x and z to make all unique BlockPositions are included
        {
            int result4 = Ints.compare(o1.getX(), o2.getX());
            if (result4 != 0) return result4;
        }

        {
            return Ints.compare(o1.getZ(), o2.getZ());
        }
    };

    private List<BlockPos> targetPositions;

    private int negX = 5;
    private int negY = 5;
    private int negZ = 5;
    private int posX = 5;
    private int posY = 5;
    private int posZ = 5;
    private ShapeType selectedShapeType;

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, DynamicRegistryManager.EMPTY);
        //if(tag.contains("mode")) this.selectedShapeType = ShapeType.valueOf(tag.getString("mode"));
        if(tag.contains("negX")) this.negX = tag.getInt("negX");
        if(tag.contains("negY")) this.negY = tag.getInt("negY");
        if(tag.contains("negZ")) this.negZ = tag.getInt("negZ");
        if(tag.contains("posX")) this.posX = tag.getInt("posX");
        if(tag.contains("posY")) this.posY = tag.getInt("posY");
        if(tag.contains("posZ")) this.posZ = tag.getInt("posZ");
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.selectedShapeType != null) tag.putString("mode", this.selectedShapeType.name());
        tag.putInt("negX", this.negX);
        tag.putInt("negY", this.negY);
        tag.putInt("negZ", this.negZ);
        tag.putInt("posX", this.posX);
        tag.putInt("posY", this.posY);
        tag.putInt("posZ", this.posZ);
        super.writeNbt(tag, DynamicRegistryManager.EMPTY);
    }

    public static class ShapeSize {
        public int negX;
        public int negY;
        public int negZ;
        public int posX;
        public int posY;
        public int posZ;
    }

    public ShapeSize getSize() {
        ShapeSize result = new ShapeSize();
        result.negX = this.negX;
        result.negY = this.negY;
        result.negZ = this.negZ;
        result.posX = this.posX;
        result.posY = this.posY;
        result.posZ = this.posZ;
        return result;
    }

    public void setSize(ShapeSize size) {
        Preconditions.checkArgument(size.negX > 0, "NegX must be > 0");
        this.negX = size.negX;

        Preconditions.checkArgument(size.negY > 0, "NegY must be > 0");
        this.negY = size.negY;

        Preconditions.checkArgument(size.negZ > 0, "NegZ must be > 0");
        this.negZ = size.negZ;

        Preconditions.checkArgument(size.posX > 0, "PosX must be > 0");
        this.posX = size.posX;

        Preconditions.checkArgument(size.posY > 0, "PosY must be > 0");
        this.posY = size.posY;

        Preconditions.checkArgument(size.posZ > 0, "PosZ must be > 0");
        this.posZ = size.posZ;

        if (this.selectedShapeType != null) this.recreateShape();
        this.markDirty();
    }

    public int getCount() {
        if (this.targetPositions == null) this.recreateShape();
        return this.targetPositions.size();
    }

    public ShapeType getShapeType() {
        return this.selectedShapeType;
    }

    public void setShapeType(ShapeType shape) {
        this.selectedShapeType = shape;
        this.recreateShape();
        this.markDirty();
    }

    public ActionResult onUse(ServerPlayerEntity player, BlockHitResult hit) {
        if (hit.getSide().equals(Direction.UP)) {
            ShapeSelectionGui.open(player, this);
        } else SizeSelectionGui.open(player, this);
        return ActionResult.CONSUME;
    }

    public ActionResult onUseWithItem(ServerPlayerEntity player, ItemStack stack, BlockHitResult hit) {
        if (this.world == null) return ActionResult.PASS;
        if (stack.getItem() instanceof BlockItem b) {
            if (this.targetPositions == null) return ActionResult.CONSUME;
            for (BlockPos blockPos : this.targetPositions) {
                if (this.world.isAir(blockPos) && player.canModifyAt((ServerWorld) this.world, blockPos)) {
                    if (this.world.setBlockState(blockPos, b.getBlock().getDefaultState())) {
                        if (!player.isCreative()) stack.decrement(1);
                        var g = b.getBlock().getDefaultState().getSoundGroup();
                        Objects.requireNonNull(this.world.getServer()).getPlayerManager()
                                .sendToAround(
                                        null,
                                        this.pos.getX(), this.pos.getY(), this.pos.getZ(),
                                        10,
                                        this.world.getRegistryKey(),
                                        new PlaySoundS2CPacket(
                                                RegistryEntry.of(b.getBlock().getDefaultState().getSoundGroup().getPlaceSound()),
                                                SoundCategory.PLAYERS,
                                                blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                                                g.volume, g.pitch, this.world.random.nextInt()
                                        )
                                );
                        return ActionResult.SUCCESS_SERVER;
                    }
                }
            }
        } else return this.onUse(player, hit);
        return ActionResult.CONSUME;
    }

    private void displayModeChange(PlayerEntity player) {
        player.sendMessage(Text.translatable("block.moremechanics.shape_builder.change_mode").append(Text.translatable(this.selectedShapeType.translationKey)), true);
    }

    private void displayBlockCount(PlayerEntity player) {
        if (this.targetPositions == null) return;
        player.sendMessage(Text.translatable("block.moremechanics.shape_builder.total_blocks").append(Text.literal(this.targetPositions.size() + "")), true);
    }

    public static void tick(World world, ShapeBuilderBlockEntity te) {
        if (te.selectedShapeType == null) return;
        if (world.isClient) return;
        if (te.targetPositions != null) {
            BlockPos negBound = new BlockPos(
                    te.getPos().getX() + (te.negX - (2*te.negX)),
                    te.getPos().getY() + (te.negY - (2*te.negY)),
                    te.getPos().getZ() + (te.negZ - (2*te.negZ))
            );
            BlockPos posBound = new BlockPos(
                    te.getPos().getX() + te.posX,
                    te.getPos().getY() + te.posY,
                    te.getPos().getZ() + te.posZ
            );

            /*
            ParticleUtils.showParticleAround(new DustParticleEffect(ParticleUtils.BLUE, 1.0F), (ServerWorld) world, negBound, 20);
            ParticleUtils.showParticleAround(new DustParticleEffect(ParticleUtils.BLUE, 1.0F), (ServerWorld) world, posBound, 20);
            */
            for (BlockPos blockPos : te.targetPositions) {
                if (blockPos != negBound && blockPos != posBound) {
                    ParticleUtils.showParticleAround(new DustParticleEffect(ParticleUtils.WHITE, 1.0F),(ServerWorld) world, blockPos, 20);
                }
            }

        }
    }

    private void recreateShape() {
        ShapeGenerator generator = this.getShapeType().generator;
        Set<BlockPos> uniqueResults = Sets.newHashSet();
        Shapeable collector = (x, y, z) -> {
            if (this.canAddTarget(x, y, z)) uniqueResults.add(new BlockPos(x, y, z));
        };
        generator.generateShape(-this.negX, -this.negY, -this.negZ, this.posX, this.posY, this.posZ, collector);

        List<BlockPos> sortedResults = Lists.newArrayList(uniqueResults);
        sortedResults.sort(ShapeBuilderBlockEntity.COMPARATOR);

        List<BlockPos> finalResult = Lists.newArrayList();

        //StructureTemplate.transformAround()
        //final Orientation orientation = Orientation.NORTH_UP;
        //return ImmutableList.copyOf(sortedResults);


        for (BlockPos c : sortedResults) {
            finalResult.add(new BlockPos(this.getPos().getX() + c.getX(), this.getPos().getY() + c.getY(), this.getPos().getZ() + c.getZ()));
            /*
            rotatedResult.add(c.offset(orientation.getFacing()).offset(orientation.getRotation()));
            final int tx = orientation.transformX(c.getX(), c.getY(), c.getZ());
            final int ty = orientation.transformY(c.getX(), c.getY(), c.getZ());
            final int tz = orientation.transformZ(c.getX(), c.getY(), c.getZ());
            rotatedResult.add(new BlockPos(tx, ty, tz));
            */
        }
        this.targetPositions = ImmutableList.copyOf(finalResult);
    }

    protected boolean canAddTarget(int x, int y, int z) {
        return (x != 0) || (y != 0) || (z != 0);
    }

    private void notifyPlayer(PlayerEntity player) {
        //player.sendMessage(Text.translatable("block.moremechanics.shape_builder.change_box_size", -negX, -negY, -negZ, +posX, +posY, +posZ));
        this.displayBlockCount(player);
    }

    private void afterDimensionsChange(PlayerEntity player) {
        if (this.selectedShapeType != null) this.recreateShape();
        this.markDirty();
        this.notifyPlayer(player);
    }

    private static class ShapeSelectionGui extends SimpleGui {
        public static void open(ServerPlayerEntity player, ShapeBuilderBlockEntity shapeBuilder) {
            new ShapeSelectionGui(player, shapeBuilder).open();
        }
        private final ShapeBuilderBlockEntity shapeBuilder;
        private ShapeSelectionGui(ServerPlayerEntity player, ShapeBuilderBlockEntity shapeBuilder) {
            super(ScreenHandlerType.GENERIC_9X2, player, false);
            this.shapeBuilder = shapeBuilder;
        }

        @Override
        public void onOpen() {
            super.onOpen();
            this.setTitle(Text.literal("Shapes"));

            for (int i = 0; i < 18; i++) {
                this.clearSlot(i);
            }

            this.addSlot(GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.disable"))
                    .setLore(this.shapeBuilder.getShapeType() == null ? Lists.newArrayList(Text.translatable("item.moremechanics.shape_builder.selected").formatted(Formatting.GREEN)) : Lists.newArrayList())
                    .setComponent(DataComponentTypes.ITEM_MODEL, this.shapeBuilder.getShapeType() == null ? MoreMechanics.id("gui/shapes/selected") : MoreMechanics.id("gui/shapes/disable"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.selectedShapeType = null;
                        this.shapeBuilder.markDirty();
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            for (ShapeType shapeType : ShapeType.values()) {
                this.addSlot(GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable(shapeType.translationKey))
                    .setLore(this.shapeBuilder.getShapeType() == shapeType ? Lists.newArrayList(Text.translatable("item.moremechanics.shape_builder.selected").formatted(Formatting.GREEN)) : Lists.newArrayList())
                    .setComponent(DataComponentTypes.ITEM_MODEL, this.shapeBuilder.getShapeType() == shapeType ? MoreMechanics.id("gui/shapes/selected") : shapeType.getModel())
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setShapeType(shapeType);
                        this.shapeBuilder.displayModeChange(this.player);
                        this.shapeBuilder.recreateShape();
                        this.shapeBuilder.markDirty();
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            }
        }
    }

    private static class SizeSelectionGui extends SimpleGui {
        public static void open(ServerPlayerEntity player, ShapeBuilderBlockEntity shapeBuilder) {
            new SizeSelectionGui(player, shapeBuilder).open();
        }
        private final ShapeBuilderBlockEntity shapeBuilder;
        private SizeSelectionGui(ServerPlayerEntity player, ShapeBuilderBlockEntity shapeBuilder) {
            super(ScreenHandlerType.GENERIC_9X5, player, false);
            this.shapeBuilder = shapeBuilder;
        }

        @Override
        public void onOpen() {
            super.onOpen();
            this.setTitle(Text.literal("Shape Size"));
            // pos Y
            this.setSlot(4, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.decrease"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/decrease"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = Math.max(SizeSelectionGui.this.shapeBuilder.posY - 1, 1);
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            this.setSlot(5, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.literal(String.valueOf(this.shapeBuilder.posY)).append(Text.literal(" (Y+)").formatted(Formatting.GRAY)))
                    .setLore(List.of(Text.translatable("item.moremechanics.shape_builder.side_description.1").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.side_description.2").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.pos_y_description").formatted(Formatting.GRAY)))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/pos_y")));
            this.setSlot(6, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.increase"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/increase"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = Math.max(SizeSelectionGui.this.shapeBuilder.posY + 1, 1);
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));

            // neg Y
            this.setSlot(38, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.decrease"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/decrease"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = Math.max(SizeSelectionGui.this.shapeBuilder.negY - 1, 1);
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            this.setSlot(39, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.literal(String.valueOf(this.shapeBuilder.negY)).append(Text.literal(" (Y-)").formatted(Formatting.GRAY)))
                    .setLore(List.of(Text.translatable("item.moremechanics.shape_builder.side_description.1").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.side_description.2").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.neg_y_description").formatted(Formatting.GRAY)))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/neg_y")));
            this.setSlot(40, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.increase"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/increase"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = Math.max(SizeSelectionGui.this.shapeBuilder.negY + 1, 1);
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));

            // neg X
            this.setSlot(18, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.decrease"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/decrease"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = Math.max(SizeSelectionGui.this.shapeBuilder.negX - 1, 1);
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            this.setSlot(19, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.literal(String.valueOf(this.shapeBuilder.negX)).append(Text.literal(" (X-)").formatted(Formatting.GRAY)))
                    .setLore(List.of(Text.translatable("item.moremechanics.shape_builder.side_description.1").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.side_description.2").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.neg_x_description").formatted(Formatting.GRAY)))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/neg_x")));
            this.setSlot(20, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.increase"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/increase"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = Math.max(SizeSelectionGui.this.shapeBuilder.negX + 1, 1);
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));

            // pos X
            this.setSlot(24, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.decrease"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/decrease"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = Math.max(SizeSelectionGui.this.shapeBuilder.posX - 1, 1);
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            this.setSlot(25, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.literal(String.valueOf(this.shapeBuilder.posX)).append(Text.literal(" (X+)").formatted(Formatting.GRAY)))
                    .setLore(List.of(Text.translatable("item.moremechanics.shape_builder.side_description.1").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.side_description.2").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.pos_x_description").formatted(Formatting.GRAY)))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/pos_x")));
            this.setSlot(26, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.increase"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/increase"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = Math.max(SizeSelectionGui.this.shapeBuilder.posX + 1, 1);
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            // neg Z
            this.setSlot(28, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.decrease"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/decrease"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = Math.max(SizeSelectionGui.this.shapeBuilder.negZ - 1, 1);
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            this.setSlot(29, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.literal(String.valueOf(this.shapeBuilder.negZ)).append(Text.literal(" (Z-)").formatted(Formatting.GRAY)))
                    .setLore(List.of(Text.translatable("item.moremechanics.shape_builder.side_description.1").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.side_description.2").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.neg_z_description").formatted(Formatting.GRAY)))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/neg_z")));
            this.setSlot(30, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.increase"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/increase"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = Math.max(SizeSelectionGui.this.shapeBuilder.negZ + 1, 1);
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = SizeSelectionGui.this.shapeBuilder.posZ;
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));

            // pos Z
            this.setSlot(14, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.decrease"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/decrease"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = Math.max(SizeSelectionGui.this.shapeBuilder.posZ - 1, 1);
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
            this.setSlot(15, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.literal(String.valueOf(this.shapeBuilder.posZ)).append(Text.literal(" (Z+)").formatted(Formatting.GRAY)))
                    .setLore(List.of(Text.translatable("item.moremechanics.shape_builder.side_description.1").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.side_description.2").formatted(Formatting.GRAY),Text.translatable("item.moremechanics.shape_builder.pos_z_description").formatted(Formatting.GRAY)))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/pos_z")));
            this.setSlot(16, GuiElementBuilder.from(Items.BREEZE_ROD.getDefaultStack())
                    .setName(Text.translatable("item.moremechanics.shape_builder.increase"))
                    .setComponent(DataComponentTypes.ITEM_MODEL, MoreMechanics.id("gui/size/increase"))
                    .setCallback((slot, clickType, actionType, slotGuiInterface) -> {
                        this.shapeBuilder.setSize(new ShapeSize() {{
                            super.negX = SizeSelectionGui.this.shapeBuilder.negX;
                            super.negY = SizeSelectionGui.this.shapeBuilder.negY;
                            super.negZ = SizeSelectionGui.this.shapeBuilder.negZ;
                            super.posX = SizeSelectionGui.this.shapeBuilder.posX;
                            super.posY = SizeSelectionGui.this.shapeBuilder.posY;
                            super.posZ = Math.max(SizeSelectionGui.this.shapeBuilder.posZ + 1, 1);
                        }});
                        this.shapeBuilder.afterDimensionsChange(this.player);
                        this.onOpen();
                        this.player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                                this.player.getX(), this.player.getY(), this.player.getZ(),
                                1.0F, 1.0F, 0
                        ));
                    }));
        }
    }
}
