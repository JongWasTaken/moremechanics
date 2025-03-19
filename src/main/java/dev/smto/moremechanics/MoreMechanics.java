package dev.smto.moremechanics;

import dev.smto.simpleconfig.SimpleConfig;
import dev.smto.simpleconfig.api.ConfigAnnotations;
import dev.smto.simpleconfig.api.ConfigLogger;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.LoggerFactory;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.*;
import dev.smto.moremechanics.item.GenericBlockItem;
import dev.smto.moremechanics.item.SurvivalDebugStickItem;
import dev.smto.moremechanics.item.MobPrisonItem;
import dev.smto.moremechanics.util.*;

import java.lang.reflect.Field;
import java.util.*;

public class MoreMechanics implements ModInitializer {
	private final static Map<Item,Item> CONCRETE_POWDER_CONVERSION_MAP = new HashMap<>();
	static {
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.WHITE_CONCRETE_POWDER, net.minecraft.item.Items.WHITE_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.LIGHT_GRAY_CONCRETE_POWDER, net.minecraft.item.Items.LIGHT_GRAY_CONCRETE);
        MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.GRAY_CONCRETE_POWDER, net.minecraft.item.Items.GRAY_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.BLACK_CONCRETE_POWDER, net.minecraft.item.Items.BLACK_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.BROWN_CONCRETE_POWDER, net.minecraft.item.Items.BROWN_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.RED_CONCRETE_POWDER, net.minecraft.item.Items.RED_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.ORANGE_CONCRETE_POWDER, net.minecraft.item.Items.ORANGE_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.YELLOW_CONCRETE_POWDER, net.minecraft.item.Items.YELLOW_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.LIME_CONCRETE_POWDER, net.minecraft.item.Items.LIME_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.GREEN_CONCRETE_POWDER, net.minecraft.item.Items.GREEN_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.CYAN_CONCRETE_POWDER, net.minecraft.item.Items.CYAN_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.LIGHT_BLUE_CONCRETE_POWDER, net.minecraft.item.Items.LIGHT_BLUE_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.BLUE_CONCRETE_POWDER, net.minecraft.item.Items.BLUE_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.PURPLE_CONCRETE_POWDER, net.minecraft.item.Items.PURPLE_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.MAGENTA_CONCRETE_POWDER, net.minecraft.item.Items.MAGENTA_CONCRETE);
		MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.put(net.minecraft.item.Items.PINK_CONCRETE_POWDER, net.minecraft.item.Items.PINK_CONCRETE);
	}
	public static final String MOD_ID = "moremechanics";
	public static final boolean DEV_MODE = FabricLoader.getInstance().isDevelopmentEnvironment();
	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MoreMechanics.MOD_ID);
	public static Identifier id(String path) {
		return Identifier.of(MoreMechanics.MOD_ID, path);
	}
	public static final SimpleConfig CONFIG_MANAGER = new SimpleConfig(FabricLoader.getInstance().getConfigDir().resolve("moremechanics.conf"), Config.class, new ConfigLogger() {
		@Override
		public void debug(String message) { MoreMechanics.LOGGER.debug(message); }
		@Override
		public void error(String message) { MoreMechanics.LOGGER.error(message); }
		@Override
		public void info(String message) { MoreMechanics.LOGGER.info(message); }
		@Override
		public void warn(String message) { MoreMechanics.LOGGER.warn(message); }
	});

	@Override
	public void onInitialize() {
		TranslationUtils.loadTranslationKeys();
		PolymerResourcePackUtils.addModAssets(MoreMechanics.MOD_ID);
		PolymerResourcePackUtils.markAsRequired();
		PlayerMovementEvents.init();

		for (Field field : DataComponentTypes.class.getFields()) {
			try {
				var c = (ComponentType<?>) field.get(null);
				Registry.register(Registries.DATA_COMPONENT_TYPE, MoreMechanics.id(field.getName().toLowerCase(Locale.ROOT)), c);
				PolymerComponent.registerDataComponent(c);
			} catch (Throwable ignored) {
				MoreMechanics.LOGGER.error("Failed to register data component type: {}", field.getName());
			}
		}

		for (Field field : Blocks.class.getFields()) {
			try {
				if (field.get(null) instanceof MoreMechanicsContent block) {
					block.register(Registries.BLOCK);
				}
			} catch (Throwable ignored) {
                MoreMechanics.LOGGER.error("Failed to register block: {}", field.getName());
			}
		}

		for (Field field : BlockEntities.class.getFields()) {
			try {
				if (field.get(null) instanceof BlockEntityType<?> b) {
					Registry.register(Registries.BLOCK_ENTITY_TYPE, MoreMechanics.id(field.getName().toLowerCase(Locale.ROOT)), b);
					PolymerBlockUtils.registerBlockEntity(b);
				}
			} catch (Throwable ignored) {
                MoreMechanics.LOGGER.error("Failed to register block entity type: {}", field.getName());
			}
		}

		List<Item> groupedItems = new ArrayList<>();

		for (Field field : Items.class.getFields()) {
            try {
				if (field.get(null) instanceof Item temp) {
					if (temp instanceof MoreMechanicsContent item) {
						if (item instanceof GenericBlockItem b) {
							if (!b.getIdentifier().equals(MoreMechanics.id(field.getName().toLowerCase(Locale.ROOT)))) {
								MoreMechanics.LOGGER.error("GenericBlockItem {} has incorrect identifier: {}, expected: {}", field.getName(), b.getIdentifier(), MoreMechanics.id(field.getName().toLowerCase(Locale.ROOT)));
								throw new RuntimeException();
							}
						}
						item.register(Registries.ITEM);
						groupedItems.add(temp);
					}
				}
            } catch (Exception ignored) {
                MoreMechanics.LOGGER.error("Failed to register item: {}", field.getName());
			}
        }

		CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(net.minecraft.item.Items.DIRT, (BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) -> {
			if (!world.isClient) {
				LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
				if (!player.isCreative()) stack.decrement(1);
				player.giveOrDropStack(new ItemStack(net.minecraft.item.Items.MUD, 1));
				return ActionResult.SUCCESS;
			} else return ActionResult.PASS;
		});

        MoreMechanics.CONCRETE_POWDER_CONVERSION_MAP.forEach((concretePowder, concrete) -> {
			CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(concretePowder, (BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) -> {
				if (!world.isClient) {
					LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
					if (!player.isCreative()) stack.decrement(1);
					player.giveOrDropStack(new ItemStack(concrete, 1));
					return ActionResult.SUCCESS;
				} else return ActionResult.PASS;
			});
		});

		PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(MoreMechanics.MOD_ID,"items"), PolymerItemGroupUtils.builder()
				.icon(() -> new ItemStack(Items.MEGA_TORCH))
				.displayName(Text.of("MoreMechanics Stuff"))
				.entries((context, entries) -> {
					groupedItems.forEach(entries::add);
				}).build());

		CommandRegistrationCallback.EVENT.register((dispatcher, x, environment) -> Commands.register(dispatcher));
		MoreMechanics.LOGGER.info("MoreMechanics loaded!");
	}

	public static class Blocks {
		public static Block ELEVATOR = new Elevator(MoreMechanics.id("elevator"));
		public static Block MEGA_TORCH = new MegaTorchBlock(MoreMechanics.id("mega_torch"));
		public static Block EXPERIENCE_STORAGE = new ExperienceStorageBlock(MoreMechanics.id("experience_storage"));
		public static Block CHUNK_LOADER = new ChunkLoaderBlock(MoreMechanics.id("chunk_loader"));
		public static Block SHAPE_BUILDER = new ShapeBuilderBlock(MoreMechanics.id("shape_builder"));
		//public static Block MECHANICAL_USER = new MechanicalUserBlock(MoreMechanics.id("mechanical_user"));
	}

	public static class Items {
		public static BlockItem ELEVATOR = new GenericBlockItem(MoreMechanics.id("elevator"), Blocks.ELEVATOR, Rarity.COMMON);
		public static BlockItem MEGA_TORCH = new GenericBlockItem(MoreMechanics.id("mega_torch"), Blocks.MEGA_TORCH, Rarity.COMMON);
		public static BlockItem EXPERIENCE_STORAGE = new GenericBlockItem(MoreMechanics.id("experience_storage"), Blocks.EXPERIENCE_STORAGE, Rarity.COMMON);
		public static BlockItem CHUNK_LOADER = new GenericBlockItem(MoreMechanics.id("chunk_loader"), Blocks.CHUNK_LOADER, Rarity.COMMON);
		public static BlockItem SHAPE_BUILDER = new GenericBlockItem(MoreMechanics.id("shape_builder"), Blocks.SHAPE_BUILDER, Rarity.COMMON);
		public static Item SURVIVAL_DEBUG_STICK = new SurvivalDebugStickItem(MoreMechanics.id("survival_debug_stick"));
		public static Item MOB_PRISON = new MobPrisonItem(MoreMechanics.id("mob_prison"));
		//public static BlockItem MECHANICAL_USER = new GenericBlockItem(MoreMechanics.id("mechanical_user"), Blocks.MECHANICAL_USER, Rarity.COMMON);
	}

	public static class BlockEntities {
		public static BlockEntityType<MegaTorchBlockEntity> MEGA_TORCH_ENTITY = FabricBlockEntityTypeBuilder
				.create(MegaTorchBlockEntity::new, Blocks.MEGA_TORCH).build();
		public static BlockEntityType<ExperienceStorageBlockEntity> EXPERIENCE_STORAGE_ENTITY = FabricBlockEntityTypeBuilder
				.create(ExperienceStorageBlockEntity::new, Blocks.EXPERIENCE_STORAGE).build();
		//public static BlockEntityType<MechanicalUserBlockEntity> MECHANICAL_USER_ENTITY = FabricBlockEntityTypeBuilder
		//		.create(MechanicalUserBlockEntity::new, Blocks.MECHANICAL_USER).build();
		public static BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER_ENTITY = FabricBlockEntityTypeBuilder
				.create(ChunkLoaderBlockEntity::new, Blocks.CHUNK_LOADER).build();
		public static BlockEntityType<ShapeBuilderBlockEntity> SHAPE_BUILDER_ENTITY = FabricBlockEntityTypeBuilder
				.create(ShapeBuilderBlockEntity::new, Blocks.SHAPE_BUILDER).build();
	}

	public static class DataComponentTypes {
		public static ComponentType<NbtComponent> STORED_ENTITY = ComponentType.<NbtComponent>builder().codec(NbtComponent.CODEC).build();
	}

	public static class Config {
		@ConfigAnnotations.Holds(type = String.class)
		@ConfigAnnotations.Comment(comment = "List of block tags that can be modified with the survival debug stick.")
		public static List<String> survivalDebugStickBlockTagWhitelist = new ArrayList<>() {{
			this.add("minecraft:walls");
			this.add("minecraft:fence_gates");
			this.add("minecraft:fences");
			this.add("minecraft:stairs");
			this.add("minecraft:trapdoors");
			this.add("minecraft:doors");
            this.add("minecraft:terracotta");
            this.add("c:glass_panes");
            this.add("c:glazed_terracotta");
		}};
	}
}