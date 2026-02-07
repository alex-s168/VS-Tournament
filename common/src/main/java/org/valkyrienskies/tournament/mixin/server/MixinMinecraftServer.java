package org.valkyrienskies.tournament.mixin.server;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

@Mixin(
    value = MinecraftServer.class,
    priority = 1500 /* hopefully after VS2's MixinMinecraftServer */
)
class MixinMinecraftServer {
    @Inject(
        method = "createLevels",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getDataStorage()Lnet/minecraft/world/level/storage/DimensionDataStorage;"
        )
    )
    private void postCreateLevels(final CallbackInfo ci) {
        var self = (MinecraftServer) (Object) this;
        TournamentEvents.postCreateDimensions.emit(self);
    }
}