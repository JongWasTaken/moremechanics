package dev.smto.moremechanics.item;

import dev.smto.moremechanics.MoreMechanics;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DebugStickStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.api.ItemInteractionPrecedence;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;

public class SurvivalDebugStickItem extends DebugStickItem implements PolymerItem, MoreMechanicsContent, ItemInteractionPrecedence {
    private final Identifier id;

    public SurvivalDebugStickItem(Identifier id) {
        super(new Settings().rarity(Rarity.EPIC).maxCount(1).registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.DEBUG_STICK;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.id;
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.ITEM;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        //TranslationUtils.setTooltipText(tooltip, stack, 1, 2);
    }

    private boolean use(PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, ItemStack stack) {
        Block block = state.getBlock();
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Collection<Property<?>> collection = stateManager.getProperties();

        // check if block is modifiable by the config
        if (!this.isBlockAllowedToModify(state.getBlock()) || collection.isEmpty()) {
            SurvivalDebugStickItem.sendMessage(player, Text.translatable("item.moremechanics.survival_debug_stick.invalid"));
            return false;
        }

        @SuppressWarnings("DataFlowIssue") Map<RegistryEntry<Block>, Property<?>> originalCompound = stack.getComponents().contains(DataComponentTypes.DEBUG_STICK_STATE) ? stack.getComponents().get(DataComponentTypes.DEBUG_STICK_STATE).properties() : DebugStickStateComponent.DEFAULT.properties();
        HashMap<RegistryEntry<Block>,Property<?>> compound = new HashMap<>(originalCompound);

        var blockName = Registries.BLOCK.getEntry(block);
        Property<?> property = compound.get(blockName);

        if (player.isSneaking()) { // player.getOffHandStack().isOf(this)
            // select next property
            property = this.getNextProperty(collection, property, block);
            // save chosen property in the NBT data of Debug Stick
            compound.put(blockName, property);
            //nbtCompound.putString(blockName, property.getName());
            //player.getInventory().markDirty();

            // send the player a message of successful selecting
            SurvivalDebugStickItem.sendMessage(player,
                Text.translatable("item.moremechanics.survival_debug_stick.selected.1")
                    .append(Text.literal(" «"))
                    .append(Text.of(property.getName()))
                    .append(Text.literal("» "))
                    .append(Text.translatable("item.moremechanics.survival_debug_stick.selected.2"))
                    .append(Text.literal(" ("))
                    .append(Text.of(SurvivalDebugStickItem.getValueString(state, property)))
                    .append(Text.literal(")."))
                );
        } else {
            // change value of property
            if (property == null) {
                property = this.getNextProperty(collection, null, block);
            }

            // generate new state of chosen block with modified property
            BlockState newState = SurvivalDebugStickItem.cycle(state, property);
            // update chosen block with its new state
            world.setBlockState(pos, newState, 18);
            // send the player a message of successful modifying
            SurvivalDebugStickItem.sendMessage(player, Text.of(
                            String.format(
                                    "Property «%s» was modified (%s).",
                                    property.getName(),
                                    SurvivalDebugStickItem.getValueString(newState, property)
                            )
                    )
            );
        }
        stack.set(DataComponentTypes.DEBUG_STICK_STATE, new DebugStickStateComponent(compound));
        return true;
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property) {
        return state.with(property, SurvivalDebugStickItem.cycle(property.getValues(), state.get(property)));
    }

    private static <T> T cycle(Iterable<T> elements, @Nullable T current) {
        return Util.next(elements, current);
    }

    private static void sendMessage(PlayerEntity player, Text message) {
        ((ServerPlayerEntity)player).sendMessageToClient(message, true);
    }

    private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {
        return property.name(state.get(property));
    }

    /**
     * Choose next property that is appropriate for the configuration file
     * */
    private Property<?> getNextProperty(Collection<Property<?>> collection, @Nullable Property<?> property, @Nullable Block block) {
        int len = collection.size();
        do { // simply scrolling through the list of properties until suitable is found
            property = Util.next(collection, property);
            len--;
        } while (len > 0 && !this.isPropertyModifiable(property, block));
        return property;
    }

    /**
     * Check via config if chosen property is able to be modified in survival
     * */
    private boolean isPropertyModifiable(Property<?> property, @Nullable Block block) {
        if(property.equals(Properties.WATERLOGGED)) return false;
        if(property.equals(Properties.STAGE)) return false;
        return this.isBlockAllowedToModify(block);
    }

    private static final List<Block> ALLOWED_BLOCKS = List.of(
            Blocks.TERRACOTTA,
            Blocks.LIGHTNING_ROD,
            Blocks.END_ROD
    );

    /**
     * Check via config if chosen block is able to be modified in survival
     * */
    private boolean isBlockAllowedToModify(Block block) {
        if (block != null) {
            if(SurvivalDebugStickItem.ALLOWED_BLOCKS.contains(block)) {
                return true;
            } else {
                for (TagKey<Block> tag : MoreMechanics.Config.survivalDebugStickBlockTagWhitelist.stream().map(x -> TagKey.of(Registries.BLOCK.getKey(), Identifier.tryParse(x))).toList()) {
                    if (block.getDefaultState().isIn(tag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        if (!world.isClient) {
            this.use(miner, state, world, pos, miner.getStackInHand(Hand.MAIN_HAND));
        }

        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity playerEntity = context.getPlayer();
        World world = context.getWorld();
        if (!world.isClient && playerEntity != null) {
            BlockPos blockPos = context.getBlockPos();
            if (!this.use(playerEntity, world.getBlockState(blockPos), world, blockPos, context.getStack())) {
                return ActionResult.FAIL;
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
