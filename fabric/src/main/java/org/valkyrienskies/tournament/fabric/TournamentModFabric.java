package org.valkyrienskies.tournament.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.event.EmittableEvent;
import org.valkyrienskies.tournament.*;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;
import org.valkyrienskies.tournament.registry.CreativeTabs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// TODO: port this to kotlin

public class TournamentModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new ValkyrienSkiesModFabric().onInitialize();

        for (var config : TournamentConfigUpdater.ALL_CONFIGS) {
            ForgeConfigRegistry.INSTANCE.register(
                    TournamentMod.MOD_ID,
                    config.forgeType(),
                    config.spec,
                    config.path
            );
        }

        ModConfigEvents.reloading(TournamentMod.MOD_ID).register(TournamentConfigUpdater::update);
        ModConfigEvents.loading(TournamentMod.MOD_ID).register(TournamentConfigUpdater::update);

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                TournamentItems.INSTANCE.getTAB(),
                CreativeTabs.INSTANCE.create()
        );

        ServerTickEvents.END_SERVER_TICK.register(TickScheduler.INSTANCE::tickServer);

        TournamentMod.init();

        TournamentEvents.registerResourceManagers.emit(new ResourceListenerRegistrar());
    }

    private static class ResourceListenerRegistrar implements TournamentEvents.ResourceListenerRegistrar {
        @Override
        public void registerListener(@NotNull ResourceLocation id, @NotNull PreparableReloadListener listener) {
            ResourceManagerHelper.get(PackType.SERVER_DATA)
                    .registerReloadListener(new IdentifiableResourceReloadListener() {
                        @Override
                        public ResourceLocation getFabricId() {
                            return id;
                        }

                        @Override
                        public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                            return listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
                        }
                    });
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
            TournamentMod.initClient();
            TournamentMod.initClientRenderers(new ClientRenderersFabric());

            ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) ->
                    TournamentModels.INSTANCE.getMODELS().forEach(out));
        }

        private static class ClientRenderersFabric implements TournamentMod.ClientRenderers {
            @Override
            public <T extends BlockEntity> void registerBlockEntityRenderer(
                    @NotNull BlockEntityType<T> t,
                    @NotNull BlockEntityRendererProvider<T> r) {
                BlockEntityRendererRegistry.register(t, r);
            }
        }
    }
}
