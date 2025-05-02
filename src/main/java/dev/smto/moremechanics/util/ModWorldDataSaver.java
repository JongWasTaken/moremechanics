package dev.smto.moremechanics.util;

import dev.smto.moremechanics.MoreMechanics;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;

public class ModWorldDataSaver extends PersistentState {
    public ArrayList<GlobalPos> existingChunkLoaders = new ArrayList<>();
    public ArrayList<GlobalPos> existingPeaceBeacons = new ArrayList<>();

    @Override
    public NbtCompound writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for(GlobalPos pos : this.existingChunkLoaders) {
            try {
                list.add(CodecUtils.toNbt(pos, GlobalPos.CODEC));
            } catch (Throwable ignored) {}
        }
        tag.put("existingChunkLoaders", list);

        NbtList list2 = new NbtList();
        for(GlobalPos pos : this.existingPeaceBeacons) {
            try {
                list2.add(CodecUtils.toNbt(pos, GlobalPos.CODEC));
            } catch (Throwable ignored) {}
        }
        tag.put("existingPeaceBeacons", list2);
        return tag;
    }

    public static ModWorldDataSaver createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        ModWorldDataSaver state = new ModWorldDataSaver();
        ArrayList<GlobalPos> chunkLoaders = new ArrayList<>();
        if (tag.contains("existingChunkLoaders")) {
            NbtList nbtList = (NbtList) tag.get("existingChunkLoaders");
            assert nbtList != null;
            for (NbtElement nbtElement : nbtList) {
                try {
                    chunkLoaders.add(CodecUtils.fromNbt(nbtElement, GlobalPos.CODEC));
                } catch (Throwable ignored) {}
            }
            state.existingChunkLoaders = chunkLoaders;
        }
        ArrayList<GlobalPos> peaceBeacons = new ArrayList<>();
        if (tag.contains("existingPeaceBeacons")) {
            NbtList nbtList = (NbtList) tag.get("existingPeaceBeacons");
            assert nbtList != null;
            for (NbtElement nbtElement : nbtList) {
                try {
                    peaceBeacons.add(CodecUtils.fromNbt(nbtElement, GlobalPos.CODEC));
                } catch (Throwable ignored) {}
            }
            state.existingPeaceBeacons = peaceBeacons;
        }
        return state;
    }

    public static ModWorldDataSaver createNew() {
        ModWorldDataSaver state = new ModWorldDataSaver();
        state.existingChunkLoaders = new ArrayList<>();
        state.existingPeaceBeacons = new ArrayList<>();
        return state;
    }

    private static final Type<ModWorldDataSaver> TYPE = new Type<>(
            ModWorldDataSaver::createNew,
            ModWorldDataSaver::createFromNbt,
            null
    );

    public static ModWorldDataSaver get(MinecraftServer server) {
        ServerWorld serverWorld = server.getWorld(World.OVERWORLD);
        assert serverWorld != null;
        ModWorldDataSaver state = serverWorld.getPersistentStateManager().getOrCreate(ModWorldDataSaver.TYPE, MoreMechanics.MOD_ID);
        state.markDirty();
        return state;
    }
}