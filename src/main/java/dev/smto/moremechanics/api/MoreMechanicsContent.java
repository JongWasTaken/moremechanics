package dev.smto.moremechanics.api;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import dev.smto.moremechanics.MoreMechanics;

import java.util.List;

public interface MoreMechanicsContent {
    default Identifier getIdentifier() {
        throw new RuntimeException("Must override getIdentifier()");
    }
    default Registry<?> getTargetRegistry() {
        return null;
    }
    @SuppressWarnings("unchecked")
    default <T> void _register(Registry<T> registry) {
        try {
            Registry.register(registry, this.getIdentifier(), (T) this);
        } catch (Throwable x) {
            throw new RuntimeException("Failed to register content: " + this.getIdentifier() + ", " + x.getMessage(), x);
        }
    }

    default void register(Registry<?> fallbackRegistry) {
        Registry<?> target = this.getTargetRegistry();
        if (target == null) {
            MoreMechanics.LOGGER.warn("{} does not override getTargetRegistry()!", this.getIdentifier());
            target = fallbackRegistry;
        }
        this._register(target);
    }

    @SuppressWarnings("unused")
    default void register() {
        Registry<?> target = this.getTargetRegistry();
        if (target == null) {
            MoreMechanics.LOGGER.warn("{} does not override getTargetRegistry()!", this.getIdentifier());
            throw new RuntimeException("Failed to register content: " + this.getIdentifier());
        }
        this._register(target);
    }

    default void addTooltip(ItemStack stack, List<Text> tooltip) {}

    default Block getDummyBlock() {
        return Blocks.AIR;
    }

}
