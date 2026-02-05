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

        @ConfigEntry(description = "The speed at which the thruster will stop applying force. (-1 means that it always applies force)")
        var thrusterShutoffSpeed = 80.0

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

        // TODO: add stuff idk
        // TODO: make tag
        @ConfigEntry(description = "The list of blocks that don't get assembled by the ship assembler")
        var blockBlacklist = setOf(
            "minecraft:dirt",
            "minecraft:grass_block",
            "minecraft:grass_path",
            "minecraft:stone",
            "minecraft:bedrock",
            "minecraft:sand",
            "minecraft:gravel",
            "minecraft:water",
            "minecraft:flowing_water",
            "minecraft:lava",
            "minecraft:flowing_lava",
            "minecraft:lily_pad",
            "minecraft:coarse_dirt",
            "minecraft:podzol",
            "minecraft:granite",
            "minecraft:diorite",
            "minecraft:andesite",
            "minecraft:crimson_nylium",
            "minecraft:warped_nylium",
            "minecraft:red_sand",
            "minecraft:sandstone",
            "minecraft:end_stone",
            "minecraft:red_sandstone",
            "minecraft:blackstone",
            "minecraft:netherrack",
            "minecraft:soul_sand",
            "minecraft:soul_soil",
            "minecraft:grass",
            "minecraft:fern",
            "minecraft:dead_bush",
            "minecraft:seagrass",
            "minecraft:tall_seagrass",
            "minecraft:sea_pickle",
            "minecraft:kelp",
            "minecraft:bamboo",
            "minecraft:dandelion",
            "minecraft:poppy",
            "minecraft:blue_orchid",
            "minecraft:allium",
            "minecraft:azure_bluet",
            "minecraft:red_tulip",
            "minecraft:orange_tulip",
            "minecraft:white_tulip",
            "minecraft:pink_tulip",
            "minecraft:oxeye_daisy",
            "minecraft:cornflower",
            "minecraft:lily_of_the_valley",
            "minecraft:brown_mushroom",
            "minecraft:red_mushroom",
            "minecraft:crimson_fungus",
            "minecraft:warped_fungus",
            "minecraft:crimson_roots",
            "minecraft:warped_roots",
            "minecraft:nether_sprouts",
            "minecraft:weeping_vines",
            "minecraft:twisting_vines",
            "minecraft:chorus_plant",
            "minecraft:chorus_flower",
            "minecraft:snow",
            "minecraft:cactus",
            "minecraft:vine",
            "minecraft:sunflower",
            "minecraft:lilac",
            "minecraft:rose_bush",
            "minecraft:peony",
            "minecraft:tall_grass",
            "minecraft:large_fern",
            "minecraft:air",
            "minecraft:ice",
            "minecraft:packed_ice",
            "minecraft:blue_ice",
            "minecraft:portal",
            "minecraft:bedrock",
            "minecraft:end_portal_frame",
            "minecraft:end_portal",
            "minecraft:end_gateway",
            "minecraft:portal",
            "minecraft:oak_sapling",
            "minecraft:spruce_sapling",
            "minecraft:birch_sapling",
            "minecraft:jungle_sapling",
            "minecraft:acacia_sapling",
            "minecraft:dark_oak_sapling",
            "minecraft:oak_leaves",
            "minecraft:spruce_leaves",
            "minecraft:birch_leaves",
            "minecraft:jungle_leaves",
            "minecraft:acacia_leaves",
            "minecraft:dark_oak_leaves"
        )

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