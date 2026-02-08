package org.valkyrienskies.tournament.client

import net.minecraft.client.resources.model.BakedModel
import net.minecraft.resources.ResourceLocation

interface ModelManagerExt {
    fun `vs_tournament$getModelOrNull`(key: ResourceLocation): BakedModel?
}