package dev.smto.moremechanics.block;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
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
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ExperienceStorageBlock extends Block implements PolymerTexturedBlock, BlockEntityProvider, MoreMechanicsContent {
    private final Identifier id;

    public ExperienceStorageBlock(Identifier id) {
        super(Settings.copy(Blocks.IRON_BLOCK).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
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
        if (world.isClient()) return;
        if (world.getBlockEntity(pos) instanceof ExperienceStorageBlockEntity ent) {
            ServerPlayerEntity p = null;
            if (placer instanceof ServerPlayerEntity) p = (ServerPlayerEntity) placer;
            ent.setInitialized();
            ent.initDisplayEntity((ServerWorld) world, p);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
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
    public final ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof ExperienceStorageBlockEntity ent) {
            if (!world.isClient()) ent.dropExperience((ServerWorld) world, pos);
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public final BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState(); // this.polymerBlockState;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.METAL;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    private static class Gui extends SimpleGui {
        public static void openFor(ServerPlayerEntity player, ExperienceStorageBlockEntity host) {
            new Gui(player, host).open();
        }

        private static int getBarCapacityAtLevel(int level) {
            if (level >= 30) {
                return 112 + ((level - 30) * 9);
            } else if (level >= 15) {
                return 37 + ((level - 15) * 5);
            } else if (level < 0) {
                return 0;
            } else {
                return 7 + (level * 2);
            }
        }

        private static Pair<Integer, Float> getLevelsAndPoints(int amount) {
            int level = 0;
            float exp = amount / (float) Gui.getBarCapacityAtLevel(level);
            while (exp >= 1.0F) {
                exp = (exp - 1) * Gui.getBarCapacityAtLevel(level);
                level += 1;
                exp = exp / Gui.getBarCapacityAtLevel(level);
            }
            return new Pair<>(level, exp);
        }

        private static int getTotalPoints(int level, float exp) {
            int points = 0;
            for (int i = 0; i < level; i++) {
                points += Gui.getBarCapacityAtLevel(i);
            }
            points += Math.round(Gui.getBarCapacityAtLevel(level) * exp);
            return points;
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
            int newV = Gui.getTotalPoints(this.player.experienceLevel + levels, 0);
            int oldV = Gui.getTotalPoints(this.player.experienceLevel, this.player.experienceProgress);
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
            var values = Gui.getLevelsAndPoints(this.host.getStoredAmount());
            if (values.getRight() != 0.0F) r = new DecimalFormat(".00").format(values.getRight());
            lore.add(Text.literal(
                    values.getLeft() + r
            ).formatted(Formatting.LIGHT_PURPLE).append(Text.literal(" ")).append(Text.translatable("item.moremechanics.xpstorage.stored_xp_levels_text").formatted(Formatting.GRAY)));
            this.setSlot(4, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.IRON_QUESTION_MARK))
                    .setLore(lore)
                    .setCallback(this::refreshCounter)
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.stored_xp_text"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();

            this.setSlot(0, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.QUARTZ_ONE))
                    //.addLoreLine(Text.translatable("item.moremechanics.skin_manager.add_one_desc").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback(() -> {
                        this.fixPlayerLevels();
                        int xp = Gui.getBarCapacityAtLevel(this.player.experienceLevel - 1);
                        int i = this.host.addExperience(xp);
                        this.player.addExperience(-i);
                        this.refreshCounter();
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.add_one"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(1, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.QUARTZ_TEN))
                    //.addLoreLine(Text.translatable("item.moremechanics.skin_manager.add_ten_desc").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback(() -> {
                        this.fixPlayerLevels();
                        int xp = 0;
                        for (int i = 0; i < 10; i++) {
                            xp += Gui.getBarCapacityAtLevel(this.player.experienceLevel - 1 - i);
                        }
                        int i = this.host.addExperience(xp);
                        this.player.addExperience(-i);
                        this.refreshCounter();
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.add_ten"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(2, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.IRON_PLUS))
                    //.addLoreLine(Text.translatable("item.moremechanics.skin_manager.add_all_desc").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback(() -> {
                        int playerXP = Gui.getTotalPoints(this.player.experienceLevel, this.player.experienceProgress);
                        int xp = this.host.addExperience(playerXP < 0 ? Integer.MAX_VALUE : playerXP);
                        this.player.addExperience(-xp);
                        this.refreshCounter();
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.add_all"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );

            this.refreshCounter();

            this.setSlot(6, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.IRON_MINUS))
                    //.addLoreLine(Text.translatable("item.moremechanics.xpstorage.take_all_desc").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback(() -> {
                        int xp = this.host.takeExperience(Integer.MAX_VALUE);
                        this.player.addExperience(xp);
                        this.refreshCounter();
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.take_all"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(7, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.QUARTZ_TEN))
                    //.addLoreLine(Text.translatable("item.moremechanics.xpstorage.take_ten_desc").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback(() -> {
                        this.addLevelsToPlayer(10);
                        this.refreshCounter();
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.take_ten"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );
            this.setSlot(8, GuiElementBuilder.from(Items.PLAYER_HEAD.getDefaultStack())
                    .setComponent(DataComponentTypes.PROFILE, this.getTexturedHead(HeadTexture.QUARTZ_ONE))
                    //.addLoreLine(Text.translatable("item.moremechanics.skin_manager.take_one_desc").formatted(Formatting.LIGHT_PURPLE))
                    .setCallback(() -> {
                        this.addLevelsToPlayer(1);
                        this.refreshCounter();
                    })
                    .setComponent(DataComponentTypes.ITEM_NAME, Text.translatable("item.moremechanics.xpstorage.take_one"))
                    .setComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
                    .build()
            );

        }

        private ProfileComponent getTexturedHead(HeadTexture texture) {
            return new ProfileComponent(Optional.empty(), Optional.empty(), new PropertyMap() {{
                this.put("0", new Property("textures", texture.get()));
            }});
        }

        @Override
        public Text getTitle() {
            return Text.translatable("item.moremechanics.xpstorage.title");
        }

        enum HeadTexture {
            IRON_QUESTION_MARK("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjViOTVkYTEyODE2NDJkYWE1ZDAyMmFkYmQzZTdjYjY5ZGMwOTQyYzgxY2Q2M2JlOWMzODU3ZDIyMmUxYzhkOSJ9fX0="),
            IRON_PLUS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmM0OGRkZmRjZDZkOThhMWIwYWEzYzcxZThkYWQ0ZWRkZTczMmE2OGIyYjBhNWFiMTQyNjAwZGNhNzU4N2MzMiJ9fX0="),
            IRON_MINUS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmYwNWFmZWMyYTZlYzY3NWNkNTUwNWE4ZjQ0YmI2YTRkNTU2OTM1Njg5NTI4MzIxZWFkNGVkZWY2ODVmMmQxMCJ9fX0="),

            QUARTZ_ONE("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTBhMTllMjNkMjFmMmRiMDYzY2M1NWU5OWFlODc0ZGM4YjIzYmU3NzliZTM0ZTUyZTdjN2I5YTI1In19fQ=="),
            QUARTZ_TEN("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTExNWE4ODYxZDY1NTdkMjg3M2ZlYzgyMzI3ZWExZDQ2NDNlMmZmMTkxZGJmMWVmOTE5Zjk0YTU0NSJ9fX0="),
            //QUARTZ_A("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDJjZDVhMWI1Mjg4Y2FhYTIxYTZhY2Q0Yzk4Y2VhZmQ0YzE1ODhjOGIyMDI2Yzg4YjcwZDNjMTU0ZDM5YmFiIn19fQ=="),
            //QUARTZ_QUESTION_MARK("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmMyNzEwNTI3MTllZjY0MDc5ZWU4YzE0OTg5NTEyMzhhNzRkYWM0YzI3Yjk1NjQwZGI2ZmJkZGMyZDZiNWI2ZSJ9fX0=")
            ;
            private final String texture;
            HeadTexture(String texture) {
                this.texture = texture;
            }

            public String get() {
                return this.texture;
            }
        }

    }
}
