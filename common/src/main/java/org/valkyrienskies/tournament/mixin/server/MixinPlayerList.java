package org.valkyrienskies.tournament.mixin.server;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

@Mixin(
    value = PlayerList.class,
    priority = 1500 /* hopefully inject after VS2's MixinPlayerList */
)
class MixinPlayerList {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void afterPlayerJoin(final Connection netManager, final ServerPlayer player, final CallbackInfo ci) {
        TournamentEvents.postPlayerJoin.emit(new TournamentEvents.PlayerJoin(netManager, player));
    }
}