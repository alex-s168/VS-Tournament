package org.valkyrienskies.tournament.mixin.level;

import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

@Mixin(OreFeatures.class)
public class MixinOreFeatures {
    @Inject(at = @At("TAIL"), method = "bootstrap")
    private static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context, CallbackInfo ci) {
        TournamentEvents.bootstrapOreFeatures.emit(context);
    }
}