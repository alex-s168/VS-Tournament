package org.valkyrienskies.tournament

import net.minecraft.data.worldgen.BootstapContext
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import org.valkyrienskies.core.api.event.EventConsumer
import org.valkyrienskies.core.api.event.RegisteredListener
import org.valkyrienskies.core.api.event.SingleEvent
import org.valkyrienskies.core.util.events.EventEmitterImpl

// kotlin <-> jvm interop moment
class EventWrapper<T>(private val impl: SingleEvent<T>) : SingleEvent<T> {
    override fun on(cb: EventConsumer<T>): RegisteredListener =
        impl.on(cb)

    override fun emit(value: T) =
        impl.emit(value)
}

object TournamentEvents {
    @JvmField val itemHoverText = EventWrapper(EventEmitterImpl<ItemHoverText>())
    @JvmField val clientTick = EventWrapper(EventEmitterImpl<Unit>())
    @JvmField val worldGenFeatures = EventWrapper(EventEmitterImpl<BiomeGenerationSettings.Builder>())
    @JvmField val bootstrapPlacedFeatures = EventWrapper(EventEmitterImpl<BootstapContext<PlacedFeature>>())
    @JvmField val bootstrapOreFeatures = EventWrapper(EventEmitterImpl<BootstapContext<ConfiguredFeature<*, *>>>())
    @JvmField val registerResourceManagers = EventWrapper(EventEmitterImpl<ResourceListenerRegistrar>())

    fun interface ResourceListenerRegistrar {
        fun registerListener(id: ResourceLocation, listener: PreparableReloadListener)
    }

    data class ItemHoverText(
        val stack: ItemStack,
        val level: Level?,
        val tooltipComponents: MutableList<Component>,
        val isAdvanced: TooltipFlag
    )
}