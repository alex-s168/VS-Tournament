package org.valkyrienskies.tournament

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.client.resources.model.UnbakedModel
import net.minecraft.core.RegistryAccess
import net.minecraft.data.worldgen.BootstapContext
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.entity.player.Player
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
import org.valkyrienskies.tournament.TournamentEvents.ResourceListenerRegistrar
import java.util.function.Consumer
import kotlin.jvm.Throws

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
    @JvmField val registerResourceListeners = EventWrapper(EventEmitterImpl<ResourceListenerRegistrar>())
    @JvmField val postPlayerJoin = EventWrapper(EventEmitterImpl<PlayerJoin>())
    @JvmField val postCreateDimensions = EventWrapper(EventEmitterImpl<MinecraftServer>())
    @JvmField val registriesCompleted = postCreateDimensions
    @JvmField val collectModelsToBake = EventWrapper(EventEmitterImpl<ModelToBakeCollector>())
    @JvmField val postModelReload = EventWrapper(EventEmitterImpl<ModelManager>())

    interface ModelToBakeCollector {
        @Throws(Exception::class)
        fun loadSimpleModel(location: ResourceLocation)

        fun putModel(location: ResourceLocation, model: UnbakedModel)
    }

    interface ResourceListenerRegistrar {
        val registryAccess: RegistryAccess
        fun registerListener(id: ResourceLocation, listener: PreparableReloadListener)
    }

    data class PlayerJoin(
        val connection: Connection,
        val player: Player,
    )

    data class ItemHoverText(
        val stack: ItemStack,
        val level: Level?,
        val tooltipComponents: MutableList<Component>,
        val isAdvanced: TooltipFlag
    )
}

fun interface MinimalJsonResourceListener {
    fun apply(
        objects: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller?
    )
}

fun ResourceListenerRegistrar.registerListener(
    id: ResourceLocation,
    directory: String,
    gson: Gson = Gson(),
    listener: MinimalJsonResourceListener
) {
    registerListener(id, object : SimpleJsonResourceReloadListener(gson, directory) {
        override fun apply(
            map: Map<ResourceLocation, JsonElement>,
            resourceManager: ResourceManager,
            profiler: ProfilerFiller?
        ) {
            listener.apply(map, resourceManager, profiler)
        }
    })
}