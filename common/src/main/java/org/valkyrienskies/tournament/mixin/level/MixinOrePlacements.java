package org.valkyrienskies.tournament.mixin.level;

import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

@Mixin(OrePlacements.class)
public class MixinOrePlacements {
    @Inject(at = @At("TAIL"), method = "bootstrap")
    private static void bootstrap(BootstapContext<PlacedFeature> context, CallbackInfo ci) {
        TournamentEvents.INSTANCE.getBootstrapPlacedFeatures()
                .emit(context);
    }
}