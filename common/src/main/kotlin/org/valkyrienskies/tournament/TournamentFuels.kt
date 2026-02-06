package org.valkyrienskies.tournament

import blitz.caching
import blitz.collections.RefVec
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.Lifecycle
import dev.architectury.utils.GameInstance
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.LiteralContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.tags.TagKey
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.tournament.util.extension.applyStyle
import org.valkyrienskies.tournament.util.extension.toComponent
import org.valkyrienskies.tournament.util.extension.toResourceLocation
import org.valkyrienskies.tournament.util.parseParticle
import java.lang.ref.WeakReference
import kotlin.jvm.optionals.getOrNull
import kotlin.math.round

// TODO: STORE IN DATAPACK

class Cached<K, V>(private val compute: (K) -> V) {
    private var key: WeakReference<K>? = null
    private var v: V? = null
    operator fun get(key: K): V {
        if (key == this.key?.get()) {
            return v!!
        }
        this.key = WeakReference(key)
        v = compute(key)
        return v!!
    }
}

data class FuelType(
    // when off
    val standbyBurnRate: Float,

    // when on
    val baseBurnRate: Float,
    val burnRatePerThrottle: Float,

    // when on
    val basePower: Float,
    val powerPerThrottle: Float,

    val particles: ((RegistryAccess) -> ParticleOptions)?,
    val particleVelocity: Float,
    val particleSpread: Float,
    val particleCount: Int,
) {
    // higher score is "better"
    val score = basePower + powerPerThrottle * 15 - baseBurnRate * 1000 - burnRatePerThrottle * 1000 * 15 - standbyBurnRate * 10000

    fun getBurnRate(throttle: Float) =
        if (throttle == 0.0f)
            standbyBurnRate
        else
            baseBurnRate + burnRatePerThrottle * throttle

    fun getPower(throttle: Float) =
        basePower + powerPerThrottle * throttle

    companion object {
        fun decode(json: JsonObject): FuelType =
            FuelType(
                standbyBurnRate = json.get("standbyBurnRate")?.asFloat ?: 0f,
                baseBurnRate = json.get("baseBurnRate")?.asFloat ?: 0f,
                burnRatePerThrottle = json.get("burnRatePerThrottle")?.asFloat ?: 0f,
                basePower = json.get("basePower")?.asFloat ?: 0f,
                powerPerThrottle = json.get("powerPerThrottle")?.asFloat ?: 0f,
                particles = json.get("particleType")?.asString?.let {
                    if (it.isEmpty()) null
                    else (Cached { registryAccess: RegistryAccess -> parseParticle(registryAccess, it) })::get
                },
                particleVelocity = json.get("particleVelocity")?.asFloat ?: 0.4f,
                particleSpread = json.get("particleSpread")?.asFloat ?: 0f,
                particleCount = json.get("particleCount")?.asInt ?: 2,
            )
    }

}

class RegisteredFuelType(
    val id: ResourceLocation,
    val fuel: FuelType,
    val targetItems: RefVec<ResourceLocation>,
    val targetTags: RefVec<ResourceLocation>,
)

object TournamentFuels {
    @JvmField
    val REGISTRY = MappedRegistry<RegisteredFuelType>(
        ResourceKey.createRegistryKey(ResourceLocation(TournamentMod.MOD_ID, "fuels")),
        Lifecycle.experimental()
    )

    private val byItem = mutableMapOf<ResourceLocation, RegisteredFuelType>()
    private val byTag = mutableMapOf<ResourceLocation, RegisteredFuelType>()

    @JvmStatic
    fun byItem(item: ResourceKey<Item>): RegisteredFuelType? =
        byItem[item.location()]

    @JvmStatic
    fun byItemId(item: ResourceLocation): RegisteredFuelType? =
        byItem[item]

    @JvmStatic
    fun byTag(tag: TagKey<Item>): RegisteredFuelType? =
        byTag[tag.location]

    @JvmStatic
    fun byTagId(tag: ResourceLocation): RegisteredFuelType? =
        byTag[tag]

    @JvmStatic
    fun get(item: ItemStack): RegisteredFuelType? {
        var best: RegisteredFuelType? = null
        fun found(ft: RegisteredFuelType) {
            if (best == null || ft.fuel.score > best!!.fuel.score)
                best = ft
        }
        item.itemHolder.unwrapKey().getOrNull()?.location()?.let(::byItemId)?.let(::found)
        item.tags.forEach {
            byTag(it)?.let(::found)
        }
        return best
    }

    private val loader = object : SimpleJsonResourceReloadListener(Gson(), "vs_tournament_fuel") {
        override fun apply(
            objects: Map<ResourceLocation, JsonElement>,
            resourceManager: ResourceManager,
            profiler: ProfilerFiller?
        ) {
            objects.forEach { (key, element) ->
                try {
                    val json = element.asJsonObject
                    val fuel = RegisteredFuelType(
                        id = key,
                        fuel = FuelType.decode(json),
                        targetItems = ((json.get("items")?.asJsonArray?.asSequence() ?: emptySequence()) + sequenceOf(json.get("item")))
                            .filterNotNull()
                            .map { it.asString.toResourceLocation() }
                            .asIterable()
                            .let(RefVec.Companion::from),
                        targetTags = ((json.get("tags")?.asJsonArray?.asSequence() ?: emptySequence()) + sequenceOf(json.get("tag")))
                            .filterNotNull()
                            .map { it.asString.toResourceLocation() }
                            .asIterable()
                            .let(RefVec.Companion::from),
                    )
                    Registry.register(REGISTRY, key, fuel)
                } catch (e: Exception) {
                    println("[Tournament] ERROR while trying to load fuel '$key':")
                    e.printStackTrace()
                }
            }
        }
    }

    val register by lazy {
        TournamentEvents.registerResourceManagers.on {
            it.registerListener(ResourceLocation(TournamentMod.MOD_ID, "vs_tournament_fuel"), loader)
        }

        VSGameEvents.registriesCompleted.on {
            byItem.clear()
            byTag.clear()

            println("[Tournament] ${REGISTRY.size()} fuel types were registered")
            REGISTRY.forEach { fuel ->
                fuel.targetItems.forEach {
                    byItem.compute(it) { _, old ->
                        if (old == null || fuel.fuel.score > old.fuel.score)
                            fuel
                        else old
                    }
                }

                fuel.targetTags.forEach {
                    byTag.compute(it) { _, old ->
                        if (old == null || fuel.fuel.score > old.fuel.score)
                            fuel
                        else old
                    }
                }
            }
        }

        TournamentEvents.itemHoverText.on { (stack, _, tooltipComponents, _) ->
            val fuel = stack.tournamentFuel()?.fuel ?: return@on

            fun num(num: Float): String =
                (round(num * 1_000_000f) / 1_000_000f).toString()

            fun tc(key: String, vararg args: Any, styleMod: (Style) -> Style = { it }): MutableComponent =
                TranslatableContents("tooltip.vs_tournament.fuel.$key", "$key Fuel", args)
                    .toComponent()
                    .applyStyle(styleMod)

            fun t(key: String, vararg args: Any, styleMod: (Style) -> Style = { it }) {
                tooltipComponents += tc(key, *args, styleMod)
            }

            fun separator() {
                tooltipComponents += LiteralContents("").toComponent()
            }

            // TODO: merge with vs mass tooltips code (also has pounds cfg and conversion)

            t("title") { it.withUnderlined(true) }

            separator()

            t("standbyBurnRate", num(fuel.standbyBurnRate), num(fuel.standbyBurnRate * 20))
            t("baseBurnRate", num(fuel.baseBurnRate), num(fuel.baseBurnRate * 20))
            t("burnRatePerThrottle", num(fuel.burnRatePerThrottle), num(fuel.burnRatePerThrottle * 20)) {
                it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tc("infoThrottle")))
            }

            separator()

            t("basePower", num(fuel.basePower))
            t("powerPerThrottle", num(fuel.powerPerThrottle)) {
                it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tc("infoThrottle")))
            }
        }
    }
}

fun ItemStack.tournamentFuel(): RegisteredFuelType? =
    TournamentFuels.get(this)