package org.valkyrienskies.tournament.mixin.client;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Mixin(ModelBakery.class)
public abstract class MixinModelBakery {
    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> unbakedCache;

    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> topLevelModels;

    @Shadow
    protected abstract BlockModel loadBlockModel(ResourceLocation location) throws IOException;

    @Shadow
    protected abstract void cacheAndQueueDependencies(ResourceLocation location, UnbakedModel model);

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(BlockColors blockColors,
                      ProfilerFiller profilerFiller,
                      Map<ResourceLocation, BlockModel> modelResources,
                      Map<ResourceLocation, List<ModelBakery.LoadedJson>> blockStateResources,
                      CallbackInfo ci)
    {
        var self = (ModelBakery) (Object) this;
        TournamentEvents.collectModelsToBake.emit(new TournamentEvents.ModelToBakeCollector() {
            private void process(UnbakedModel model) {
                model.resolveParents(self::getModel);
            }

            @Override
            public void putModel(@NotNull ResourceLocation location, @NotNull UnbakedModel model) {
                unbakedCache.put(location, model);
                topLevelModels.put(location, model);
                process(model);
            }

            @Override
            public void loadSimpleModel(@NotNull ResourceLocation location) throws Exception {
                var blockModel = loadBlockModel(location);
                cacheAndQueueDependencies(location, blockModel);
                unbakedCache.put(location, blockModel);
                topLevelModels.put(location, blockModel);
                process(blockModel);
            }
        });
    }
}