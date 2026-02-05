package org.valkyrienskies.tournament

import net.minecraft.data.worldgen.BootstapContext
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import org.valkyrienskies.core.util.events.EventEmitterImpl

object TournamentEvents {
    val itemHoverText = EventEmitterImpl<ItemHoverText>()
    val clientTick = EventEmitterImpl<Unit>()
    val worldGenFeatures = EventEmitterImpl<BiomeGenerationSettings.Builder>()
    val bootstrapPlacedFeatures = EventEmitterImpl<BootstapContext<PlacedFeature>>()
    val bootstrapOreFeatures = EventEmitterImpl<BootstapContext<ConfiguredFeature<*, *>>>()

    data class ItemHoverText(
        val stack: ItemStack,
        val level: Level?,
        val tooltipComponents: MutableList<Component>,
        val isAdvanced: TooltipFlag
    )
}