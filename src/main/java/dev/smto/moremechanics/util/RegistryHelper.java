package dev.smto.moremechanics.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public class RegistryHelper {
    public static <T> RegistryEntry<T> getRegistryEntry(T value, RegistryKey<Registry<T>> registryKey) {
        Registry<T> reg = (Registry<T>) Registries.REGISTRIES.get(registryKey.getValue());
        if (reg == null) return null;
        return reg.getOrThrow(RegistryKey.of(registryKey, reg.getId(value)));
    }
}
