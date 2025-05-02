package dev.smto.moremechanics.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.smto.moremechanics.MoreMechanics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ModWorldDataSaver extends PersistentState {

    public static final Codec<ModWorldDataSaver> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    GlobalPos.CODEC.listOf().optionalFieldOf("existingChunkLoaders", new ArrayList<>()).xmap(list -> (ArrayList<GlobalPos>)list, list -> (List<GlobalPos>)list).forGetter(ModWorldDataSaver::getExistingChunkLoaders),
                    GlobalPos.CODEC.listOf().optionalFieldOf("existingPeaceBeacons", new ArrayList<>()).xmap(list -> (ArrayList<GlobalPos>)list, list -> (List<GlobalPos>)list).forGetter(ModWorldDataSaver::getExistingPeaceBeacons)
            ).apply(instance, ModWorldDataSaver::new)
    );
    public static final PersistentStateType<ModWorldDataSaver> STATE_TYPE = new PersistentStateType<>(
            MoreMechanics.MOD_ID, ModWorldDataSaver::new, ModWorldDataSaver.CODEC, null
    );

    private final ArrayList<GlobalPos> existingChunkLoaders;
    private final ArrayList<GlobalPos> existingPeaceBeacons;

    public ModWorldDataSaver() {
        this.existingChunkLoaders = new ArrayList<>();
        this.existingPeaceBeacons = new ArrayList<>();
    }

    public ModWorldDataSaver(ArrayList<GlobalPos> existingChunkLoaders, ArrayList<GlobalPos> existingPeaceBeacons) {
        this.existingChunkLoaders = existingChunkLoaders;
        this.existingPeaceBeacons = existingPeaceBeacons;
    }

    public ArrayList<GlobalPos> getExistingChunkLoaders() {
        return this.existingChunkLoaders;
    }

    public ArrayList<GlobalPos> getExistingPeaceBeacons() {
        return this.existingPeaceBeacons;
    }

    public static ModWorldDataSaver get(MinecraftServer server) {
        ServerWorld serverWorld = server.getWorld(World.OVERWORLD);
        assert serverWorld != null;
        ModWorldDataSaver state = serverWorld.getPersistentStateManager().getOrCreate(ModWorldDataSaver.STATE_TYPE);
        state.markDirty();
        return state;
    }
}
