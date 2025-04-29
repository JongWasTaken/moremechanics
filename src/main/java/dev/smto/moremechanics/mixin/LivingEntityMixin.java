package dev.smto.moremechanics.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.smto.moremechanics.util.PlayerMovementEvents;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "jump", at=@At("HEAD"), cancellable = true)
    void MoreMechanics$jumpHook(CallbackInfo ci)
    {
        if ((LivingEntity)(Object)this instanceof ServerPlayerEntity s) {
            if(!PlayerMovementEvents.JUMP.invoker().allowJump(s))
            {
                ci.cancel();
            }
        }
    }
}
