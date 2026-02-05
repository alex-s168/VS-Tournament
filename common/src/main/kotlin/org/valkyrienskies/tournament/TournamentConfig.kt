package org.valkyrienskies.tournament

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.config.ModConfig
import org.valkyrienskies.core.internal.config.ConfigEntry
import org.valkyrienskies.core.internal.config.VsiConfigModel
import org.valkyrienskies.core.internal.config.VsiConfigModelCategory
import org.valkyrienskies.mod.api.config.VSConfigApi
import org.valkyrienskies.mod.api.config.VSConfigApi.update
import org.valkyrienskies.mod.common.config.ConfigType
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.tournament.util.extension.toForge

object TournamentConfig {
    @JvmField
    val CLIENT = Client()

    @JvmField
    val SERVER = Server()

    class Client {

        @ConfigEntry(description = "Use the particle rope renderer instead of the line rope renderer")
        var particleRopeRenderer = true

    }

    class Server {
        @ConfigEntry(description = "The maximum force a rope can handle before breaking")
        var ropeMaxForce = 1e10

        @ConfigEntry(description = "The force a spinner applies to a ship")
        var spinnerSpeed = 5000.0

        @ConfigEntry(description = "The force a balloon applies to a ship")
        var balloonPower = 30.0

        @ConfigEntry(description = "How much stronger a balloon will get when powered (1.0 is 15x stronger at max power)")
        var balloonAnalogStrength = 1.0

        @ConfigEntry(description = "The force multiplier of a balloon (not for \"powered balloon\")")
        var unpoweredBalloonMul = 5.0

        @ConfigEntry(description = "Base height of a balloon")
        var balloonBaseHeight = 100.0

        @ConfigEntry(description = "only for old thrusters: the force a thruster applies to a ship * tier")
        var thrusterSpeed = 10000.0

        @ConfigEntry(description = "The maximum amount of tiers a normal thruster can have (1-5)")
        var thrusterTiersNormal = 4

        @ConfigEntry(description = "The maximum amount of tiers a tiny thruster can have (1-5)")
        var thrusterTiersTiny = 2

        @ConfigEntry(description = "for new thrusters: throttle / fuel usage mult of tiny thrusters ; for old thrusters: force multiplier of tiny thruster")
        var thrusterTinyForceMultiplier = 0.2

        @ConfigEntry(description = "Amount of fuel items a fuel container can hold ; halfed for slab fuel tanks")
        var fuelContainerCap = 100.0

        @ConfigEntry(description = "The weight of a ballast when redstone powered")
        var ballastWeight = 10000.0

        @ConfigEntry(description = "The weight of a ballast when not redstone powered")
        var ballastNoWeight = 800.0

        @ConfigEntry(description = "The force the pulse gun applies to a ship")
        var pulseGunForce = 300.0

        @ConfigEntry(description = "Maximum distance a sensor can detect a ship from", min = 1.0, max = 128.0)
        var sensorDistance = 10.0

        @ConfigEntry(description = "The force of a big propeller at max speed")
        var propellerBigForce = 10000.0

        @ConfigEntry(description = "The max speed of a big propeller at max redstone input")
        var propellerBigSpeed = 7.0f

        @ConfigEntry(description = "The acceleration of a big propeller. (deaccel = accel * 2)")
        var propellerBigAccel = 0.1f

        @ConfigEntry(description = "The force of a big propeller at max speed")
        var propellerSmallForce = 1000.0

        @ConfigEntry(description = "The max speed of a big propeller at max redstone input")
        var propellerSmallSpeed = 50.0f

        @ConfigEntry(description = "The acceleration of a big propeller. (deaccel = accel * 2)")
        var propellerSmallAccel = 1.0f

        @ConfigEntry(description = "How many chunk tickets can be processed each level tick? (-1 means unlimited)")
        var chunkTicketsPerTick = -1

        @ConfigEntry(description = "How many chunks can be loaded per chunk ticket?")
        var chunksPerTicket = 100

        @ConfigEntry(description = "After how many ticks to error when loading chunk still not finished? (throws error when double this amount of ticks has passed)")
        var chunkLoadTimeout = 40

        @ConfigEntry(description = "a")
        var rotatorSpeed = 70.0f

        @ConfigEntry(description = "a")
        var rotatorAccel = 1.8f

        @ConfigEntry(description = "DO NOT CHANGE THIS UNLESS YOU KNOW WHAT YOU ARE DOING!")
        var removeAllAttachments = false
    }
}

object TournamentConfigUpdater {
    @JvmRecord
    data class Config(
        @JvmField val type: ConfigType,
        @JvmField val spec: ForgeConfigSpec,
        @JvmField val model: VsiConfigModel,
        @JvmField val path: String,
    ) {
        fun forgeType(): ModConfig.Type =
            type.toForge()

        companion object {
            fun make(
                type: ConfigType,
                annotatedConfigObject: Any,
                path: String,
                builder: ForgeConfigSpec.Builder.() -> ForgeConfigSpec.Builder = { this },
                forgeConfigValueConsumer: (String, ForgeConfigSpec.ConfigValue<*>) -> Unit = { a, b -> }
            ): Config {
                val model = VSConfigApi.buildVSConfigModel(annotatedConfigObject)
                val b = builder(ForgeConfigSpec.Builder())
                val spec = VSConfigApi.buildForgeConfigSpec(
                    configCategory = model.root,
                    builder = b,
                    forgeConfigValueConsumer = forgeConfigValueConsumer,
                ).build()
                return Config(
                    type = type,
                    spec = spec,
                    model = model,
                    path = path,
                )
            }
        }
    }

    @JvmField
    val SERVER = Config.make(
        ConfigType.SERVER,
        TournamentConfig.SERVER,
        "valkyrienskies/tournament/server.toml",
    )

    @JvmField
    val CLIENT = Config.make(
        ConfigType.CLIENT,
        TournamentConfig.CLIENT,
        "valkyrienskies/tournament/client.toml",
    )

    @JvmField
    val ALL_CONFIGS = arrayOf(SERVER, CLIENT)

    @JvmStatic
    fun update(config: ModConfig) {
        val updatedEntries = mutableSetOf<VSGameEvents.ConfigUpdateEntry>()
        ALL_CONFIGS.forEach {
            it.model.update(config, it.type, updatedEntries)
        }
    }
}