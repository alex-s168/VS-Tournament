package org.valkyrienskies.tournament

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block

object TournamentTags {
    val ASSEMBLER_BLACKLIST: TagKey<Block> = TagKey.create(
        Registries.BLOCK,
        ResourceLocation(TournamentMod.MOD_ID, "assembler_blacklist")
    )
}