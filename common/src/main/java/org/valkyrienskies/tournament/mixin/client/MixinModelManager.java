package org.valkyrienskies.tournament.mixin.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;
import org.valkyrienskies.tournament.client.ModelManagerExt;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(ModelManager.class)
public abstract class MixinModelManager implements ModelManagerExt {

    @Shadow
    private Map<ResourceLocation, BakedModel> bakedRegistry;

    @Unique
    public @Nullable BakedModel vs_tournament$getModelOrNull(@NotNull ResourceLocation modelLocation) {
        return this.bakedRegistry.get(modelLocation);
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void apply(ModelManager.ReloadState reloadState, ProfilerFiller profiler, CallbackInfo ci) {
        var self = (ModelManager) (Object) this;
        TournamentEvents.postModelReload.emit(self);
    }
}