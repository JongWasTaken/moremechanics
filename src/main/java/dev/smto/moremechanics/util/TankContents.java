package dev.smto.moremechanics.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public record TankContents(Fluid fluid, int amount) {
    public static final Codec<TankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("fluid").xmap(Registries.FLUID::get, Registries.FLUID::getId).forGetter(TankContents::fluid),
            Codec.INT.fieldOf("amount").forGetter(TankContents::amount)
    ).apply(instance, TankContents::new));
}
