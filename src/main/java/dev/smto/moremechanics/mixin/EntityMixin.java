package dev.smto.moremechanics.mixin;

import dev.smto.moremechanics.util.PlayerMovementEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "setSneaking", at = @At("HEAD"), cancellable = true)
    void MoreMechanics$sneakHook(boolean sneaking, CallbackInfo ci) {
        if ((Entity)(Object)this instanceof ServerPlayerEntity s) {
            if(sneaking) {
                if(!PlayerMovementEvents.SNEAK.invoker().allowSneak(s)) {
                    ci.cancel();
                }
            }
        }
    }
}