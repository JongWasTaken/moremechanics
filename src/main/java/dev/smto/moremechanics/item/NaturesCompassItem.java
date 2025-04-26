package dev.smto.moremechanics.item;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.util.Pair;
import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.util.GuiUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SimpleGuiBuilder;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class NaturesCompassItem extends Item implements PolymerItem, MoreMechanicsContent {
    private final Identifier id;
    public NaturesCompassItem(Identifier id) {
        super(new Settings().rarity(Rarity.COMMON).registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
    }

    private static final HashMap<Identifier, Item> ICONS = new HashMap<>() {{
        // https://minecraft.wiki/w/Biome
        // overworld (ocean)
        this.put(Identifier.ofVanilla("ocean"), Items.KELP);
        this.put(Identifier.ofVanilla("deep_ocean"), Items.PRISMARINE);
        this.put(Identifier.ofVanilla("warm_ocean"), Items.FIRE_CORAL);
        this.put(Identifier.ofVanilla("lukewarm_ocean"), Items.PUFFERFISH);
        this.put(Identifier.ofVanilla("deep_lukewarm_ocean"), Items.SEA_LANTERN);
        this.put(Identifier.ofVanilla("cold_ocean"), Items.ICE);
        this.put(Identifier.ofVanilla("deep_cold_ocean"), Items.PRISMARINE_SHARD);
        this.put(Identifier.ofVanilla("frozen_ocean"), Items.PACKED_ICE);
        this.put(Identifier.ofVanilla("deep_frozen_ocean"), Items.BLUE_ICE);
        this.put(Identifier.ofVanilla("mushroom_fields"), Items.BROWN_MUSHROOM);

        // overworld (highland)
        this.put(Identifier.ofVanilla("jagged_peaks"), Items.SNOW_BLOCK);
        this.put(Identifier.ofVanilla("frozen_peaks"), Items.GOAT_HORN);
        this.put(Identifier.ofVanilla("stony_peaks"), Items.CALCITE);
        this.put(Identifier.ofVanilla("meadow"), Items.ALLIUM);
        this.put(Identifier.ofVanilla("cherry_grove"), Items.PINK_PETALS);
        this.put(Identifier.ofVanilla("grove"), Items.SPRUCE_LEAVES);
        this.put(Identifier.ofVanilla("snowy_slopes"), Items.POWDER_SNOW_BUCKET);
        this.put(Identifier.ofVanilla("windswept_hills"), Items.EMERALD_ORE);
        this.put(Identifier.ofVanilla("windswept_gravelly_hills"), Items.GRAVEL);
        this.put(Identifier.ofVanilla("windswept_forest"), Items.LLAMA_SPAWN_EGG);

        // overworld (woodland)
        this.put(Identifier.ofVanilla("forest"), Items.OAK_LOG);
        this.put(Identifier.ofVanilla("flower_forest"), Items.ORANGE_TULIP);
        this.put(Identifier.ofVanilla("taiga"), Items.SPRUCE_SAPLING);
        this.put(Identifier.ofVanilla("old_growth_pine_taiga"), Items.PODZOL);
        this.put(Identifier.ofVanilla("old_growth_spruce_taiga"), Items.SPRUCE_LOG);
        this.put(Identifier.ofVanilla("snowy_taiga"), Items.SNOW);
        this.put(Identifier.ofVanilla("birch_forest"), Items.BIRCH_SAPLING);
        this.put(Identifier.ofVanilla("old_growth_birch_forest"), Items.BIRCH_WOOD);
        this.put(Identifier.ofVanilla("dark_forest"), Items.DARK_OAK_LOG);
        this.put(Identifier.ofVanilla("pale_garden"), Items.SKELETON_SKULL);
        this.put(Identifier.ofVanilla("jungle"), Items.VINE);
        this.put(Identifier.ofVanilla("sparse_jungle"), Items.JUNGLE_LOG);
        this.put(Identifier.ofVanilla("bamboo_jungle"), Items.BAMBOO);
        
        // overworld (wetland)
        this.put(Identifier.ofVanilla("river"), Items.WATER_BUCKET);
        this.put(Identifier.ofVanilla("frozen_river"), Items.ICE);
        this.put(Identifier.ofVanilla("swamp"), Items.LILY_PAD);
        this.put(Identifier.ofVanilla("mangrove_swamp"), Items.MANGROVE_PROPAGULE);
        this.put(Identifier.ofVanilla("beach"), Items.TURTLE_SCUTE);
        this.put(Identifier.ofVanilla("snowy_beach"), Items.SNOW);
        this.put(Identifier.ofVanilla("stony_shore"), Items.STONE);

        // overworld (flatland)
        this.put(Identifier.ofVanilla("plains"), Items.GRASS_BLOCK);
        this.put(Identifier.ofVanilla("sunflower_plains"), Items.SUNFLOWER);
        this.put(Identifier.ofVanilla("snowy_plains"), Items.SNOWBALL);
        this.put(Identifier.ofVanilla("ice_spikes"), Items.POLAR_BEAR_SPAWN_EGG);

        // overworld (arid-land)
        this.put(Identifier.ofVanilla("desert"), Items.SAND);
        this.put(Identifier.ofVanilla("savanna"), Items.ACACIA_LOG);
        this.put(Identifier.ofVanilla("savanna_plateau"), Items.ACACIA_SAPLING);
        this.put(Identifier.ofVanilla("windswept_savanna"), Items.ACACIA_PLANKS);
        this.put(Identifier.ofVanilla("badlands"), Items.RED_SAND);
        this.put(Identifier.ofVanilla("wooded_badlands"), Items.COARSE_DIRT);
        this.put(Identifier.ofVanilla("eroded_badlands"), Items.YELLOW_TERRACOTTA);

        // overworld (cave)
        this.put(Identifier.ofVanilla("deep_dark"), Items.SCULK);
        this.put(Identifier.ofVanilla("dripstone_caves"), Items.POINTED_DRIPSTONE);
        this.put(Identifier.ofVanilla("lush_caves"), Items.AZALEA);

        // nether
        this.put(Identifier.ofVanilla("warped_forest"), Items.WARPED_FUNGUS);
        this.put(Identifier.ofVanilla("crimson_forest"), Items.CRIMSON_FUNGUS);
        this.put(Identifier.ofVanilla("basalt_deltas"), Items.BASALT);
        this.put(Identifier.ofVanilla("nether_wastes"), Items.NETHERRACK);
        this.put(Identifier.ofVanilla("soul_sand_valley"), Items.SOUL_SAND);

        // end
        this.put(Identifier.ofVanilla("the_end"), Items.DRAGON_EGG);
        this.put(Identifier.ofVanilla("end_highlands"), Items.CHORUS_FLOWER);
        this.put(Identifier.ofVanilla("end_midlands"), Items.END_STONE_BRICKS);
        this.put(Identifier.ofVanilla("end_barrens"), Items.END_STONE);
        this.put(Identifier.ofVanilla("small_end_islands"), Items.BEDROCK);
    }};

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.COMPASS;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Identifier.ofVanilla("compass");
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.moremechanics.natures_compass");
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.PASS;
        this.openGui((ServerPlayerEntity) user, user.getStackInHand(hand));
        return ActionResult.SUCCESS_SERVER;
    }

    public void openGui(ServerPlayerEntity player, ItemStack host) {
        var biomes = new ArrayList<GuiElementBuilder>();
        for (Identifier identifier : BuiltinRegistries.createWrapperLookup().getOrThrow(RegistryKeys.BIOME)
                .streamKeys()
                .map(RegistryKey::getValue).toList()) {
            biomes.add(GuiElementBuilder
                    .from(NaturesCompassItem.ICONS.getOrDefault(identifier, Items.BARRIER).getDefaultStack())
                    .setItemName(Text.literal(identifier.toString()))
                    .hideDefaultTooltip()
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
                        Pair<BlockPos, RegistryEntry<Biome>> pair = player.getServerWorld().locateBiome((b) -> b.matchesId(identifier), player.getBlockPos(), 6400, 32, 64);
                        stopwatch.stop();
                        gui.close();
                        if (pair != null) {
                            player.sendMessage(Text.translatable("item.moremechanics.natures_compass.found"), true);
                            host.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(
                                    Optional.of(GlobalPos.create(player.getWorld().getRegistryKey(), pair.getFirst())), true));
                            return;
                        }
                        player.sendMessage(Text.translatable("item.moremechanics.natures_compass.no_biome"), true);
                    })
            );
        }
        new Gui(player, biomes).open();
    }

    private static class Gui extends SimpleGui {
        private final List<GuiElementBuilder> biomes;
        private int page = 1;

        public Gui(ServerPlayerEntity player, List<GuiElementBuilder> biomes) {
            super(ScreenHandlerType.GENERIC_9X6, player, false);
            this.biomes = biomes;
            this.setTitle(Text.translatable("item.moremechanics.natures_compass"));
        }

        @Override
        public void beforeOpen() {
            for (int i = 0; i < 45; i++) {
                try {
                    this.setSlot(i, this.biomes.get(((this.page - 1) * 45) + i));
                } catch (Throwable ignored) {
                    this.setSlot(i, ItemStack.EMPTY);
                }
            }
            this.setSlot(45, GuiElementBuilder
                    .from(Items.BARRIER.getDefaultStack())
                    .setItemName(Text.literal("Previous Page"))
                    .hideDefaultTooltip()
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        if (this.page > 1) this.page--;
                        gui.beforeOpen();
                    })
            );
            this.setSlot(49, GuiElementBuilder
                    .from(Items.SPYGLASS.getDefaultStack())
                    .setItemName(Text.literal("Search"))
                    .hideDefaultTooltip()
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        gui.beforeOpen();
                    })
            );
            this.setSlot(53, GuiElementBuilder
                    .from(Items.BARRIER.getDefaultStack())
                    .setItemName(Text.literal("Next Page"))
                    .hideDefaultTooltip()
                    .setCallback((int index, ClickType type, SlotActionType action, SlotGuiInterface gui) -> {
                        if (this.page < Math.ceil((double) this.biomes.size() / 45)) this.page++;
                        gui.beforeOpen();
                    })
            );
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.moremechanics.natures_compass.description").formatted(MoreMechanics.getTooltipFormatting()));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.contains(DataComponentTypes.LODESTONE_TRACKER);
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.ITEM;
    }
}
