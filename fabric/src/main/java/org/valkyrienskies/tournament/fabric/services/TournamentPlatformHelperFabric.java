package org.valkyrienskies.tournament.fabric.services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.tournament.services.TournamentPlatformHelper;

public class TournamentPlatformHelperFabric implements TournamentPlatformHelper {

    @Nullable
    @Override
    public BakedModel loadBakedModel(@NotNull ModelResourceLocation modelLocation) {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(modelLocation);
    }
}
