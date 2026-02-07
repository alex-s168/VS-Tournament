package org.valkyrienskies.tournament

import blitz.collections.RefVec
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.google.gson.JsonObject
import com.mojang.serialization.Lifecycle
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.LiteralContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.core.util.events.EventEmitterImpl
import org.valkyrienskies.mod.common.playerWrapper
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.tournament.util.TournamentParticleUtils
import org.valkyrienskies.tournament.util.TypedResourceLocation
import org.valkyrienskies.tournament.util.extension.applyStyle
import org.valkyrienskies.tournament.util.extension.toComponent
import org.valkyrienskies.tournament.util.extension.toResourceLocation
import org.valkyrienskies.tournament.util.parseParticle
import kotlin.jvm.optionals.getOrNull
import kotlin.math.round

data class FuelParticleOptions(
    // TODO: somehow jackson serialize this
    @field:JsonSerialize(using = ParticleOptionSerializer::class)
    @field:JsonDeserialize(using = ParticleOptionDeserializer::class)
    @JvmField var particles: ParticleOptions?,
    @field:JsonIgnore
    @JvmField val unparsedParticles: String?,
    @JvmField val particleVelocity: Float,
    @JvmField val particleSpread: Float,
    @JvmField val particleCount: Int,
)

class ParticleOptionSerializer: JsonSerializer<ParticleOptions>() {
    override fun serialize(
        value: ParticleOptions,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeId(BuiltInRegistries.PARTICLE_TYPE, value.type)
        value.writeToNetwork(buf)
        gen.writeBinary(buf.array())
    }
}

class ParticleOptionDeserializer: JsonDeserializer<ParticleOptions>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): ParticleOptions {
        val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(p.binaryValue))
        val particleType = buf.readById(BuiltInRegistries.PARTICLE_TYPE)!!
        val opts = TournamentParticleUtils.readParticle(buf, particleType)
        return opts
    }
}

data class FuelType(
    // when off
    @JvmField val standbyBurnRate: Float,

    // when on
    @JvmField val baseBurnRate: Float,
    @JvmField val maxBurnRate: Float,

    // when on
    @JvmField val basePower: Float,
    @JvmField val maxPower: Float,

    @JvmField val particles: FuelParticleOptions,
) {
    // higher score is "better"
    @JsonIgnore
    @JvmField val score = maxPower - baseBurnRate * 1000 - maxBurnRate * 1000 - standbyBurnRate * 10000

    fun getBurnRate(throttle: Float) =
        if (throttle == 0.0f) standbyBurnRate
        else baseBurnRate + maxBurnRate * throttle

    fun getPower(throttle: Float) =
        if (throttle == 0.0f) 0f
        else basePower + maxPower * throttle

    companion object {
        fun decode(json: JsonObject): FuelType =
            FuelType(
                standbyBurnRate = json.get("standbyBurnRate")?.asFloat ?: 0f,
                baseBurnRate = json.get("baseBurnRate")?.asFloat ?: 0f,
                maxBurnRate = json.get("maxBurnRate")?.asFloat ?: 0f,
                basePower = json.get("basePower")?.asFloat ?: 0f,
                maxPower = json.get("maxPower")?.asFloat ?: 0f,
                particles = FuelParticleOptions(
                    particles = null,
                    unparsedParticles = json.get("particleType")?.asString,
                    particleVelocity = json.get("particleVelocity")?.asFloat ?: 0.4f,
                    particleSpread = json.get("particleSpread")?.asFloat ?: 0f,
                    particleCount = json.get("particleCount")?.asInt ?: 2,
                )
            )
    }

}

class RegisteredFuelType(
    @JvmField val id: ResourceLocation,
    @JvmField val fuel: FuelType,
    @JvmField val targetItems: RefVec<ResourceLocation>,
    @JvmField val targetTags: RefVec<ResourceLocation>,
    @JvmField var compactId: Int?,
) {
    fun toClient() = ClientFuelType(
        particles = fuel.particles,
    )
}

class ClientFuelType(
    @JvmField val particles: FuelParticleOptions,
)

@Environment(EnvType.CLIENT)
object TournamentClientFuels {
    @JvmField var types = emptyArray<ClientFuelType>()
}

// TODO: use real registry (also for compact fuel IDs)
object TournamentFuels {
    @JvmField val preFuelsReloaded = EventWrapper(EventEmitterImpl<Unit>())
    @JvmField val postFuelsReloaded = EventWrapper(EventEmitterImpl<Unit>())

    private val registry = mutableMapOf<TypedResourceLocation<RegisteredFuelType>, RegisteredFuelType>()
    private val byItem = mutableMapOf<TypedResourceLocation<Item>, RegisteredFuelType>()
    private val byTag = mutableMapOf<ResourceLocation, RegisteredFuelType>()
    private var clientFuels = emptyList<ClientFuelType>()

    @JvmStatic
    @JvmSynthetic @JvmName("byItemId_typed")
    fun get(item: TypedResourceLocation<Item>): RegisteredFuelType? =
        byItem[item]

    @JvmStatic
    fun byItemId(item: ResourceLocation): RegisteredFuelType? =
        byItem[TypedResourceLocation(item)]

    @JvmStatic
    @JvmName("byTagId")
    fun get(tag: TagKey<Item>): RegisteredFuelType? =
        byTag[tag.location]

    @JvmStatic
    fun byTagId(tag: ResourceLocation): RegisteredFuelType? =
        byTag[tag]

    @JvmStatic
    @JvmName("byFuelId")
    fun get(id: TypedResourceLocation<RegisteredFuelType>): RegisteredFuelType? =
        registry[id]

    @JvmStatic
    fun get(item: ItemStack): RegisteredFuelType? {
        var best: RegisteredFuelType? = null
        fun RegisteredFuelType.found() {
            if (best == null || fuel.score > best!!.fuel.score)
                best = this
        }
        item.itemHolder.unwrapKey().getOrNull()?.location()?.let(::byItemId)?.found()
        item.tags.forEach {
            get(it)?.found()
        }
        return best
    }

    val register by lazy {
        TournamentEvents.registerResourceListeners.on { registrar ->
            registrar.registerListener(ResourceLocation(TournamentMod.MOD_ID, "vs_tournament_fuel"), "vs_tournament_fuel")
            { objects, resourceManager, _ ->
                val registryAccess = registrar.registryAccess
                registry.clear()
                preFuelsReloaded.emit(Unit)
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
                            compactId = null,
                        )
                        registry[TypedResourceLocation(key)] = fuel
                    } catch (e: Exception) {
                        println("[Tournament] ERROR while trying to load fuel '$key':")
                        e.printStackTrace()
                    }
                }

                byItem.clear()
                byTag.clear()

                val allFuels = RefVec<ClientFuelType>(registry.size)
                registry.values.forEachIndexed { compactId, fuel ->
                    fuel.fuel.particles.unparsedParticles?.let {
                        fuel.fuel.particles.particles = parseParticle(registryAccess, it)
                    }

                    fuel.compactId = compactId
                    allFuels.pushBack(fuel.toClient())

                    fuel.targetItems.forEach {
                        byItem.compute(TypedResourceLocation(it)) { _, old ->
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
                clientFuels = allFuels.toCollection(ArrayList(allFuels.size))

                println("[Tournament] ${clientFuels.size} fuel types were registered")

                postFuelsReloaded.emit(Unit)
            }
        }

        // after server started, when datapack reloaded, send to all online players
        TournamentEvents.postCreateDimensions.on { _ ->
            postFuelsReloaded.on { _ ->
                vsCore.simplePacketNetworking.sendToAllClients(
                    TournamentNetworking.FuelsReloaded(clientFuels))
            }
        }

        TournamentEvents.postPlayerJoin.on { ev ->
            vsCore.simplePacketNetworking.sendToClient(
                TournamentNetworking.FuelsReloaded(clientFuels),
                ev.player.playerWrapper)
        }

        TournamentEvents.itemHoverText.on { (stack, _, tooltipComponents, _) ->
            val fuel = stack.tournamentFuel()?.fuel ?: return@on

            fun num(num: Float): String =
                (round(num * 1_000_000f) / 1_000_000f).toString()

            fun tc(key: String, vararg args: Any, styleMod: (Style) -> Style = { it }): MutableComponent =
                TranslatableContents("tooltip.vs_tournament.fuel.$key", "", args)
                    .toComponent()
                    .applyStyle(styleMod)

            fun t(key: String, vararg args: Any, styleMod: (Style) -> Style = { it }) {
                tooltipComponents += tc(key, *args, styleMod)
            }

            fun separator() {
                tooltipComponents += LiteralContents("").toComponent()
            }

            // TODO: integrate with vs mass tooltips code (also has pounds cfg and conversion)

            t("title") { it.withUnderlined(true) }
            separator()
            t("standbyBurnRate", num(fuel.standbyBurnRate), num(fuel.standbyBurnRate * 20))
            t("baseBurnRate", num(fuel.baseBurnRate), num(fuel.baseBurnRate * 20))
            t("maxBurnRate", num(fuel.maxBurnRate), num(fuel.maxBurnRate * 20))
            separator()
            t("basePower", num(fuel.basePower))
            t("maxPower", num(fuel.maxPower))
            separator()
            t("infoThrottle")
        }
    }
}

fun ItemStack.tournamentFuel(): RegisteredFuelType? =
    TournamentFuels.get(this)