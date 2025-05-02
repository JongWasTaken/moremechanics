package dev.smto.moremechanics.item;

import dev.smto.moremechanics.MoreMechanics;
import dev.smto.moremechanics.api.ItemInteractionPrecedence;
import dev.smto.moremechanics.api.MoreMechanicsContent;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DebugStickStateComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SurvivalDebugStickItem extends DebugStickItem implements PolymerItem, MoreMechanicsContent, ItemInteractionPrecedence {
    private final Identifier id;

    public SurvivalDebugStickItem(Identifier id) {
        super(new Settings().rarity(Rarity.EPIC).maxCount(1).registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.STICK;
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
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable("item.moremechanics.survival_debug_stick.description").formatted(MoreMechanics.getTooltipFormatting()));
        textConsumer.accept(Text.translatable("item.moremechanics.survival_debug_stick.description.2").formatted(MoreMechanics.getTooltipFormatting()));
    }

    private boolean use(PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, ItemStack stack) {
        Block block = state.getBlock();
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Collection<Property<?>> collection = stateManager.getProperties();

        if (!this.isBlockAllowedToModify(state.getBlock()) || collection.isEmpty()) {
            SurvivalDebugStickItem.sendMessage(player, Text.translatable("item.moremechanics.survival_debug_stick.invalid"));
            return false;
        }

        @SuppressWarnings("DataFlowIssue") Map<RegistryEntry<Block>, Property<?>> originalCompound = stack.getComponents().contains(DataComponentTypes.DEBUG_STICK_STATE) ? stack.getComponents().get(DataComponentTypes.DEBUG_STICK_STATE).properties() : DebugStickStateComponent.DEFAULT.properties();
        HashMap<RegistryEntry<Block>,Property<?>> compound = new HashMap<>(originalCompound);

        var blockName = Registries.BLOCK.getEntry(block);
        Property<?> property = compound.get(blockName);

        if (player.isSneaking()) {
            property = this.getNextProperty(collection, property, state);
            compound.put(blockName, property);
            SurvivalDebugStickItem.sendMessage(player,
                    MutableText.of(new TranslatableTextContent("item.moremechanics.survival_debug_stick.selected", null,
                            List.of(property.getName(), SurvivalDebugStickItem.getValueString(state, property)).toArray(new String[0]))
                    )
                );
        } else {
            // change value of property
            if (property == null) {
                property = this.getNextProperty(collection, null, state);
            }

            BlockState newState = SurvivalDebugStickItem.cycle(state, property);
            world.setBlockState(pos, newState, 18);
            SurvivalDebugStickItem.sendMessage(player,
                    MutableText.of(new TranslatableTextContent("item.moremechanics.survival_debug_stick.modified", null,
                            List.of(property.getName(), SurvivalDebugStickItem.getValueString(state, property)).toArray(new String[0]))
                    )
            );
        }
        stack.set(DataComponentTypes.DEBUG_STICK_STATE, new DebugStickStateComponent(compound));
        return true;
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property) {
        T next = SurvivalDebugStickItem.cycle(property.getValues(), state.get(property));
        // WallBlocks with UP=FALSE and all properties set to NONE will literally become invisible, so we restrict UP to never be FALSE
        if (property.equals(Properties.UP) && next.equals(Boolean.FALSE)) next = (T) Boolean.TRUE;
        return state.with(property, next);
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

    private Property<?> getNextProperty(Collection<Property<?>> collection, @Nullable Property<?> property, BlockState state) {
        int len = collection.size();
        do {
            property = Util.next(collection, property);
            len--;
        } while (len > 0 && !this.isPropertyModifiable(property, state));
        return property;
    }

    private boolean isPropertyModifiable(Property<?> property, BlockState state) {
        if(property.equals(Properties.WATERLOGGED)) return false;
        if(property.equals(Properties.STAGE)) return false;

        return this.isBlockAllowedToModify(state.getBlock());
    }

    private boolean isBlockAllowedToModify(Block block) {
        if (block != null) {
            if(MoreMechanics.Config.survivalDebugStickBlockWhitelist.stream().map(i -> Registries.BLOCK.get(Identifier.tryParse(i))).toList().contains(block)) {
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
    public boolean canMine(ItemStack stack, BlockState state, World world, BlockPos pos, LivingEntity user) {
        if (!world.isClient && user instanceof PlayerEntity miner) {
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
