package dev.smto.moremechanics.mixin;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.smto.moremechanics.MoreMechanics;

@Mixin(MobEntity.class)
public class MobEntityMixin {

    @Inject(at = @At("HEAD"), method = "interactWithItem", cancellable = true)
    public void MoreMechanics$interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack itemStackEx = player.getStackInHand(hand);
        if (itemStackEx.isOf(MoreMechanics.Items.MOB_PRISON)) {
            cir.setReturnValue(itemStackEx.useOnEntity(player, (MobEntity)(Object)this, hand));
            cir.cancel();
        }
    }
}
