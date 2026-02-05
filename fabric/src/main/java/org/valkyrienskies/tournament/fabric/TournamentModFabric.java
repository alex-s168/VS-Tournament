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
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.tournament.*;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;
import org.valkyrienskies.tournament.registry.CreativeTabs;

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
