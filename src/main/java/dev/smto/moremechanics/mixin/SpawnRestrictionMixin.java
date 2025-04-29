package dev.smto.moremechanics.mixin;

import dev.smto.moremechanics.block.PeaceBeaconBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnRestriction.class)
public class SpawnRestrictionMixin {
    @Inject(at = @At("HEAD"), method = "canSpawn", cancellable = true)
    private static <T extends Entity> void MoreMechanics$canSpawn(EntityType<T> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {
        if (spawnReason.equals(SpawnReason.NATURAL) && PeaceBeaconBlock.isChunkPeaceful(world.toServerWorld().getRegistryKey(), new ChunkPos(pos))) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
