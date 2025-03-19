package dev.smto.moremechanics.item;

import dev.smto.moremechanics.api.MoreMechanicsContent;
import dev.smto.moremechanics.block.ChunkLoaderBlock;
import dev.smto.moremechanics.block.ShapeBuilderBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class GenericBlockItem extends BlockItem implements eu.pb4.polymer.core.api.item.PolymerItem, MoreMechanicsContent {

    private final Identifier id;
    private final Identifier model;
    private final Block target;

    public GenericBlockItem(Identifier id, Block target, Rarity rarity) {
        super(target, new Settings().rarity(rarity).useBlockPrefixedTranslationKey().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
        this.model = id;
        this.target = target;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BARRIER;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.model;
    }

    public Identifier getIdentifier() { return this.id; }

    @Override
    public Registry<?> getTargetRegistry() {
        return Registries.ITEM;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (this.target instanceof ChunkLoaderBlock) {
            tooltip.add(Text.translatable("block.moremechanics.chunk_loader.description"));
            tooltip.add(Text.translatable("block.moremechanics.chunk_loader.description.2"));
        }
        if (this.target instanceof ShapeBuilderBlock) {
            tooltip.add(Text.translatable("block.moremechanics.shape_builder.description"));
            tooltip.add(Text.translatable("block.moremechanics.shape_builder.description.2"));
            tooltip.add(Text.translatable("block.moremechanics.shape_builder.description.3"));
        }
    }
}
