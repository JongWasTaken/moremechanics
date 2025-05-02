package dev.smto.moremechanics.block;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.entity.ExperienceStorageBlockEntity;
import dev.smto.moremechanics.block.entity.ManagedDisplayBlockEntity;
import dev.smto.moremechanics.util.ExperienceUtils;
import dev.smto.moremechanics.util.GuiUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ExperienceStorageBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    private final Identifier id;

    public ExperienceStorageBlock(Identifier id) {
        super(Settings.copy(Blocks.IRON_BLOCK).nonOpaque().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
    }

    @Override
    public void addTooltip(ItemStack stack, Consumer<Text> tooltip) {
        tooltip.accept(Text.translatable("block.moremechanics.experience_storage.description").formatted(MoreMechanics.getTooltipFormatting()));
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.BLOCK;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ExperienceStorageBlockEntity(blockPos, blockState);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        if (world.getBlockEntity(pos) instanceof ExperienceStorageBlockEntity ent) {
            Gui.openFor((ServerPlayerEntity) player, ent);
            return ActionResult.SUCCESS_SERVER;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        ManagedDisplayBlockEntity.onPlacedBlock(world, pos, state, placer, itemStack);
        if (placer instanceof ServerPlayerEntity player) {
            if (world.getBlockEntity(pos) instanceof ExperienceStorageBlockEntity e) {
                e.setDirection(player.getHorizontalFacing());
            }
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
        return (wo, pos, s, te) -> {
            if (te instanceof ExperienceStorageBlockEntity) {
                ExperienceStorageBlockEntity.tick(world, pos, (ExperienceStorageBlockEntity) te);
            }
        };
    }

    @Override
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            if (world.getBlockEntity(pos) instanceof ExperienceStorageBlockEntity ent) {
                if (!world.isClient()) ent.dropExperience(world, pos);
            }
            world.removeBlockEntity(pos);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    public final BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.METAL;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    private static final List<Gui> OPEN_GUIS = new ArrayList<>();

    public static void reopenMenus() {
        ExperienceStorageBlock.OPEN_GUIS.forEach(Gui::beforeOpen);
    }

    private static class Gui extends SimpleGui {
        public static void openFor(ServerPlayerEntity player, ExperienceStorageBlockEntity host) {
            var g = new Gui(player, host);
            ExperienceStorageBlock.OPEN_GUIS.add(g);
            g.open();
        }

        private final ExperienceStorageBlockEntity host;
        private final ServerPlayerEntity player;

        private Gui(ServerPlayerEntity player, ExperienceStorageBlockEntity host) {
            super(ScreenHandlerType.GENERIC_9X1, player, false);
            this.host = host;
            this.player = player;
        }

        private void fixPlayerLevels() {
            int transfer = (int) (this.player.experienceProgress * this.player.getNextLevelExperience());
            int i = this.host.addExperience(transfer);
            this.player.addExperience(-i);
        }

        private void addLevelsToPlayer(int levels) {
            int newV = ExperienceUtils.getTotalPoints(this.player.experienceLevel + levels, 0);
            int oldV = ExperienceUtils.getTotalPoints(this.player.experienceLevel, this.player.experienceProgress);
            int xp = newV - oldV;
            int i = this.host.takeExperience(xp);
            this.player.addExperience(i);
            if (Math.round(this.player.experienceProgress) == 1) {
                i = this.host.takeExperience(1);
                this.player.addExperience(i);
            }
        }

        private void refreshCounter() {
            List<Text> lore = new ArrayList<>();
            String r = "";
            var values = ExperienceUtils.getLevelsAndPoints(this.host.getStoredAmount());
            if (values.getRight() != 0.0F) r = new DecimalFormat(".00").format(values.getRight());
            lore.add(Text.literal(
                    values.getLeft() + r
            ).formatted(Formatting.LIGHT_PURPLE).append(Text.literal(" ")).append(Text.translatable("gui.moremechanics.experience_storage.stored_xp_levels_text").formatted(Formatting.GRAY)));
            this.setSlot(4, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.QUESTION_MARK)
                    .setLore(lore)
                    .setCallback(this::refreshCounter)
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.stored_xp_text"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();

            this.setSlot(0, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.PLUS_ONE)
                    .setCallback(() -> {
                        this.fixPlayerLevels();
                        int xp = ExperienceUtils.getBarCapacityAtLevel(this.player.experienceLevel - 1);
                        int i = this.host.addExperience(xp);
                        this.player.addExperience(-i);
                        this.refreshCounter();
                        GuiUtils.playClickSound(this.player);
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.add_one"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(1, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.PLUS_TEN)
                    .setCallback(() -> {
                        this.fixPlayerLevels();
                        int xp = 0;
                        for (int i = 0; i < 10; i++) {
                            xp += ExperienceUtils.getBarCapacityAtLevel(this.player.experienceLevel - 1 - i);
                        }
                        int i = this.host.addExperience(xp);
                        this.player.addExperience(-i);
                        this.refreshCounter();
                        GuiUtils.playClickSound(this.player);
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.add_ten"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(2, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.PLUS)
                    .setCallback(() -> {
                        int playerXP = ExperienceUtils.getTotalPoints(this.player.experienceLevel, this.player.experienceProgress);
                        int xp = this.host.addExperience(playerXP < 0 ? Integer.MAX_VALUE : playerXP);
                        this.player.addExperience(-xp);
                        this.refreshCounter();
                        GuiUtils.playClickSound(this.player);
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.add_all"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );

            this.setSlot(3, GuiUtils.Elements.FILLER);
            this.refreshCounter();
            this.setSlot(5, GuiUtils.Elements.FILLER);

            this.setSlot(6, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.MINUS)
                    .setCallback(() -> {
                        int xp = this.host.takeExperience(Integer.MAX_VALUE);
                        this.player.addExperience(xp);
                        this.refreshCounter();
                        GuiUtils.playClickSound(this.player);
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.take_all"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(7, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.MINUS_TEN)
                    .setCallback(() -> {
                        this.addLevelsToPlayer(10);
                        this.refreshCounter();
                        GuiUtils.playClickSound(this.player);
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.take_ten"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(8, GuiElementBuilder.from(Items.STICK.getDefaultStack())
                    .setComponent(DataComponentTypes.ITEM_MODEL, GuiUtils.Models.MINUS_ONE)
                    .setCallback(() -> {
                        this.addLevelsToPlayer(1);
                        this.refreshCounter();
                        GuiUtils.playClickSound(this.player);
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("gui.moremechanics.experience_storage.take_one"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
        }

        @Override
        public void onClose() {
            super.onClose();
            ExperienceStorageBlock.OPEN_GUIS.remove(this);
        }

        @Override
        public Text getTitle() {
            return Text.translatable("gui.moremechanics.experience_storage.title");
        }
    }
}
