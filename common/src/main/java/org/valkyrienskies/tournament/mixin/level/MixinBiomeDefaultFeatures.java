package org.valkyrienskies.tournament.mixin.level;

import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;
import org.valkyrienskies.tournament.TournamentWorldGen;

@Mixin(BiomeDefaultFeatures.class)
public class MixinBiomeDefaultFeatures {
    @Inject(at = @At("TAIL"), method = "addDefaultOres(Lnet/minecraft/world/level/biome/BiomeGenerationSettings$Builder;Z)V")
    private static void addDefaultOres(BiomeGenerationSettings.Builder builder, boolean bl, CallbackInfo ci) {
        TournamentEvents.worldGenFeatures.emit(builder);
    }
}
