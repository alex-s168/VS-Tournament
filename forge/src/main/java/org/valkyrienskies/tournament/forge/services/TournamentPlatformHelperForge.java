package org.valkyrienskies.tournament.forge.services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.tournament.services.TournamentPlatformHelper;

public class TournamentPlatformHelperForge implements TournamentPlatformHelper {
    @Nullable
    @Override
    public BakedModel loadBakedModel(@NotNull ModelResourceLocation modelLocation) {
        ModelBakery mb = Minecraft.getInstance().getModelManager().getModelBakery();
        return mb.getBakedTopLevelModels()
                .getOrDefault(
                    modelLocation,
                    null
                );
    }
}
