package org.valkyrienskies.tournament

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

object TournamentTags {

    // TODO: why is this unused
    val MC_WOOL: TagKey<Item> = TagKey.create(
        Registries.ITEM,
        ResourceLocation("wool")
    )

}