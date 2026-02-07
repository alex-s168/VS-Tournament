package org.valkyrienskies.tournament.mixin.server;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

import java.util.ArrayList;
import java.util.List;

@Mixin(ReloadableServerResources.class)
public class MixinReloadableServerResources {
    @Unique
    private ImmutableList<PreparableReloadListener> vs_tournament$listeners;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(RegistryAccess.Frozen registryAccess,
              FeatureFlagSet enabledFeatures,
              Commands.CommandSelection commandSelection,
              int functionCompilationLevel,
              CallbackInfo ci)
    {
        ImmutableList.Builder<PreparableReloadListener> builder = ImmutableList.builder();
        TournamentEvents.registerResourceListeners.emit(new TournamentEvents.ResourceListenerRegistrar() {
            @Override
            public @NotNull RegistryAccess getRegistryAccess() {
                return registryAccess;
            }

            @Override
            public void registerListener(@NotNull ResourceLocation id, @NotNull PreparableReloadListener listener) {
                builder.add(listener);
            }
        });
        vs_tournament$listeners = builder.build();
    }

    @ModifyReturnValue(
        method = "listeners",
        at = @At("RETURN")
    )
    private List<PreparableReloadListener> getListeners(List<PreparableReloadListener> original) {
        var out = new ArrayList<>(original);
        out.addAll(vs_tournament$listeners);
        return out;
    }
}