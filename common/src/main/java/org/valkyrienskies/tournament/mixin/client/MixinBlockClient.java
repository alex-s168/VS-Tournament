package org.valkyrienskies.tournament.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.tournament.util.block.WithExRenderInfo;

@Mixin(Block.class)
public abstract class MixinBlockClient {
    @Inject(at = @At("HEAD"), method = "shouldRenderFace", cancellable = true)
    private static void shouldRenderFace(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof WithExRenderInfo blockEx) {
            var info = blockEx.getFaceRenderType(blockState, level, pos, face);

            if (info == WithExRenderInfo.FaceRenderType.FORCE_RENDER) {
                cir.setReturnValue(true);
                return;
            }

            if (info == WithExRenderInfo.FaceRenderType.FORCE_NOT_RENDER) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
