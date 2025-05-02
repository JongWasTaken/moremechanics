package dev.smto.moremechanics.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.smto.moremechanics.MoreMechanics;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import java.util.NoSuchElementException;

public class CodecUtils {
    public static <T> String toJsonString(T input, Codec<T> codec) throws NoSuchElementException {
        return codec.encodeStart(JsonOps.INSTANCE, input).resultOrPartial(MoreMechanics.LOGGER::error).orElseThrow().toString();
    }

    public static <T> T fromJsonString(String input, Codec<T> codec) throws NoSuchElementException {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(input)).resultOrPartial(MoreMechanics.LOGGER::error).orElseThrow();
    }

    public static <T> NbtElement toNbt(T input, Codec<T> codec) throws NoSuchElementException {
        return codec.encodeStart(NbtOps.INSTANCE, input).resultOrPartial(MoreMechanics.LOGGER::error).orElseThrow();
    }

    public static <T> T fromNbt(NbtElement input, Codec<T> codec) throws NoSuchElementException {
        return codec.parse(NbtOps.INSTANCE, input).resultOrPartial(MoreMechanics.LOGGER::error).orElseThrow();
    }


    public static <T> void modifyDefaultComponentValue(Item item, ComponentType<T> target, T value) {
        item.components = ComponentMap.builder().addAll(item.getComponents()).add(target, value).build();
    }

    public static void setMaxStackSize(Item item, int size) {
        CodecUtils.modifyDefaultComponentValue(item, DataComponentTypes.MAX_STACK_SIZE, size);
    }
}
