package org.valkyrienskies.tournament.mixin.client;

import kotlin.Unit;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(at = @At("TAIL"), method = "runTick")
    private void runTick(boolean renderLevel, CallbackInfo ci) {
        TournamentEvents.clientTick.emit(Unit.INSTANCE);
    }
}
