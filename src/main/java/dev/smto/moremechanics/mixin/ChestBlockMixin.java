package dev.smto.moremechanics.mixin;

import dev.smto.moremechanics.api.TransparentToChests;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class ChestBlockMixin {
    @Inject(method = "hasBlockOnTop", at = @At("HEAD"), cancellable = true)
    private static void MoreMechanics$hasBlockOnTop(BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockPos = pos.up();
        BlockState state = world.getBlockState(blockPos);
        if (state.getBlock() instanceof TransparentToChests) cir.setReturnValue(false);
        else cir.setReturnValue(state.isSolidBlock(world, blockPos));
        cir.cancel();
    }
}
