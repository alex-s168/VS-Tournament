package org.valkyrienskies.tournament

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.tournament.blockentity.BigPropellerBlockEntity
import org.valkyrienskies.tournament.blockentity.SmallPropellerBlockEntity
import org.valkyrienskies.tournament.util.extension.explodeShip
import org.valkyrienskies.tournament.blocks.*
import org.valkyrienskies.tournament.blocks.explosive.AbstractExplosiveBlock
import org.valkyrienskies.tournament.blocks.explosive.AbstractReactiveExplosiveBlock
import org.valkyrienskies.tournament.blocks.explosive.SimpleExplosiveStagedBlock
import org.valkyrienskies.tournament.registry.DeferredRegister
import org.valkyrienskies.tournament.registry.RegistrySupplier
import org.valkyrienskies.tournament.util.extension.once

@Suppress("unused")
object TournamentBlocks {
    private val BLOCKS = DeferredRegister.create(TournamentMod.MOD_ID, Registries.BLOCK)
    private val ITEMS = ArrayList<Pair<String, ()->Item>>()


    lateinit var SHIP_ASSEMBLER           : RegistrySupplier<ShipAssemblerBlock>
    lateinit var BALLAST                  : RegistrySupplier<BallastBlock>
    lateinit var POWERED_BALLOON          : RegistrySupplier<BalloonBlock>
    lateinit var BALLOON                  : RegistrySupplier<BalloonBlock>
    lateinit var FLOATER                  : RegistrySupplier<Block>
    lateinit var THRUSTER                 : RegistrySupplier<ThrusterBlock>
    lateinit var THRUSTER_TINY            : RegistrySupplier<ThrusterBlock>
    lateinit var SPINNER                  : RegistrySupplier<SpinnerBlock>
    lateinit var SEAT                     : RegistrySupplier<SeatBlock>
    lateinit var ROPE_HOOK                : RegistrySupplier<RopeHookBlock>
    lateinit var SENSOR                   : RegistrySupplier<SensorBlock>
    lateinit var PROP_BIG                 : RegistrySupplier<PropellerBlock>
    lateinit var PROP_SMALL               : RegistrySupplier<PropellerBlock>
    lateinit var CHUNK_LOADER             : RegistrySupplier<ChunkLoaderBlock>
    lateinit var CONNECTOR                : RegistrySupplier<ConnectorBlock>
    lateinit var ORE_PHYNITE              : RegistrySupplier<Block>
    lateinit var ORE_PHYNITE_DEEPSLATE    : RegistrySupplier<Block>

    lateinit var EXPLOSIVE_INSTANT_SMALL  : RegistrySupplier<AbstractExplosiveBlock>
    lateinit var EXPLOSIVE_INSTANT_MEDIUM : RegistrySupplier<AbstractExplosiveBlock>
    lateinit var EXPLOSIVE_INSTANT_LARGE  : RegistrySupplier<AbstractExplosiveBlock>
    lateinit var EXPLOSIVE_XUS            : RegistrySupplier<AbstractExplosiveBlock>
    lateinit var EXPLOSIVE_STAGED_SMALL   : RegistrySupplier<AbstractExplosiveBlock>

    // lateinit var EXPLOSIVE_TEST           : RegistrySupplier<TestExplosiveBlock>

    lateinit var FUEL_TANK_FULL_SOLID       : RegistrySupplier<FuelTankBlockFull>
    lateinit var FUEL_TANK_FULL_TRANSPARENT : RegistrySupplier<FuelTankBlockFull>
    lateinit var FUEL_TANK_HALF_SOLID       : RegistrySupplier<FuelTankBlockHalf>

    lateinit var ROTATOR                  : RegistrySupplier<RotatorBlock>
    lateinit var MAGIC_CHAMBER            : RegistrySupplier<MagicChamberBlock>

    val register by once {
        SHIP_ASSEMBLER           = register("ship_assembler", ::ShipAssemblerBlock)
        BALLAST                  = register("ballast", ::BallastBlock)
        POWERED_BALLOON          = register("balloon", ::PoweredBalloonBlock)
        BALLOON                  = register("balloon_unpowered", ::BalloonBlock)
        FLOATER                  = register("floater") { Block(
            Properties.of().mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(1.0f, 2.0f)
        )}
        THRUSTER                 = register("thruster") {
            ThrusterBlock({ 1f }) {
                val t = TournamentConfig.SERVER.thrusterTiersNormal
                if (t !in 1..5) {
                    throw IllegalStateException("Thruster tiers (tournament config) must be in range 1..5")
                }
                t
            }
        }
        THRUSTER_TINY            = register("tiny_thruster") {
            ThrusterBlock({ TournamentConfig.SERVER.thrusterTinyForceMultiplier.toFloat() }) {
                val t = TournamentConfig.SERVER.thrusterTiersTiny
                if (t !in 1..5) {
                    throw IllegalStateException("Thruster tiers (tournament config) must be in range 1..5")
                }
                t
            }
        }
        SPINNER                  = register("spinner", ::SpinnerBlock)
        SEAT                     = register("seat", ::SeatBlock)
        SENSOR                   = register("sensor", ::SensorBlock)
        ROPE_HOOK                = register("rope_hook", ::RopeHookBlock)
        PROP_BIG                 = register("prop_big") {
            PropellerBlock(
                TournamentConfig.SERVER.propellerBigForce,
                ::BigPropellerBlockEntity
            )
        }
        PROP_SMALL               = register("prop_small") {
            PropellerBlock(
                TournamentConfig.SERVER.propellerSmallForce,
                ::SmallPropellerBlockEntity
            )
        }
        CHUNK_LOADER             = register("chunk_loader", ::ChunkLoaderBlock)
        CONNECTOR                = register("connector", ::ConnectorBlock)

        EXPLOSIVE_INSTANT_SMALL  = register("explosive_instant_small") { object : AbstractExplosiveBlock() {
            override fun explode(level: ServerLevel, pos: BlockPos) {
                level.explodeShip(pos.x+0.5, pos.y+0.5, pos.z+0.5, 3.0f, Level.ExplosionInteraction.TNT)
            }}
        }
        EXPLOSIVE_INSTANT_MEDIUM = register("explosive_instant_medium") { object : AbstractExplosiveBlock() {
            override fun explode(level: ServerLevel, pos: BlockPos) {
                level.explodeShip(pos.x+0.5, pos.y+0.5, pos.z+0.5, 6.0f, Level.ExplosionInteraction.TNT)
            }}
        }
        EXPLOSIVE_INSTANT_LARGE  = register("explosive_instant_large") { object : AbstractExplosiveBlock() {
            override fun explode(level: ServerLevel, pos: BlockPos) {
                level.explodeShip(pos.x+0.5, pos.y+0.5, pos.z+0.5, 12.0f, Level.ExplosionInteraction.TNT)
            }}
        }

        EXPLOSIVE_XUS            = register("explosive_experimental_unstable_shrapnel") { object : AbstractReactiveExplosiveBlock() {
            override fun explode(level: ServerLevel, pos: BlockPos) {
                val center = Vec3.atCenterOf(pos)
                val dmgRadius = 7.0
                level.getEntities(null, AABB.ofSize(center, dmgRadius, dmgRadius, dmgRadius)).forEach {
                    it.hurt(level.damageSources().generic(), 13.0f)
                }
                level.explodeShip(center.x, center.y, center.z, 1.5f, Level.ExplosionInteraction.TNT)
            }

            override fun shouldExplode(level: ServerLevel, pos: BlockPos): Boolean {
                val rand = level.random.nextFloat()

                return when (rand) {
                    in 0.0f..0.3f -> {
                        (pos.neighborBlocks() + pos).forEach {
                            level.setBlockAndUpdate(it, Blocks.FIRE.defaultBlockState())
                        }

                        false
                    }

                    else -> true
                }
            }
        }}

        EXPLOSIVE_STAGED_SMALL   = register("explosive_staged_small") {
            object : SimpleExplosiveStagedBlock(
                (3..7),
                (4..7),
                (-10..10),
                (-2..2),
                (-10..10),
                Level.ExplosionInteraction.TNT
            ) {}
        }
        // EXPLOSIVE_TEST           = register("explosive_test", ::TestExplosiveBlock)
        FUEL_TANK_FULL_SOLID       = register("fuel_tank_full_solid") { FuelTankBlockFull(false) }
        FUEL_TANK_FULL_TRANSPARENT = register("fuel_tank_full_transparent") { FuelTankBlockFull(true) }
        FUEL_TANK_HALF_SOLID       = register("fuel_tank_half_solid") { FuelTankBlockHalf() }

        ROTATOR                    = register("rotator") { RotatorBlock() }

        ORE_PHYNITE                = register("ore_phynite") {
            DropExperienceBlock(Properties.of().mapColor(MapColor.METAL)
                .strength(3.0f, 3.0f)
            )
        }

        ORE_PHYNITE_DEEPSLATE      = register("ore_phynite_deepslate") {
            DropExperienceBlock(Properties.of().mapColor(MapColor.METAL)
                .strength(4.5f, 4.0f)
            )
        }

        register("steel_block") {
            Block(Properties.of().mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(7.0F, 8.0F)
                .sound(SoundType.METAL))
        }

        MAGIC_CHAMBER = register("magic_chamber") {
            MagicChamberBlock()
        }

        // old:
        register("shipifier") { OldBlock(SHIP_ASSEMBLER.get()) }
        register("instantexplosive") { OldBlock(EXPLOSIVE_INSTANT_MEDIUM.get()) }
        register("instantexplosive_big") { OldBlock(EXPLOSIVE_INSTANT_LARGE.get()) }
        register("stagedexplosive") { OldBlock(EXPLOSIVE_STAGED_SMALL.get()) }
        register("stagedexplosive_big") { OldBlock(Blocks.AIR) }

        BLOCKS.applyAll()
        VSGameEvents.registriesCompleted.on { _, _ ->
            makeFlammables()
        }
    }

    private fun <T: Block> register(name: String, block: () -> T): RegistrySupplier<T> {
        val supplier = BLOCKS.register(name, block)
        ITEMS.add(name to { BlockItem(supplier.get(), Item.Properties()) })
        return supplier
    }

    private fun <T: Block> register(name: String, block: () -> T, item: () -> Item): RegistrySupplier<T> {
        val supplier = BLOCKS.register(name, block)
        ITEMS.add(name to item)
        return supplier
    }

    fun flammableBlock(block: Block, flameOdds: Int, burnOdds: Int) {
        val fire = Blocks.FIRE as FireBlock
        fire.setFlammable(block, flameOdds, burnOdds)
    }

    fun makeFlammables() {
        flammableBlock(SEAT.get(), 15, 25)
        flammableBlock(POWERED_BALLOON.get(), 30, 60)
        flammableBlock(BALLOON.get(), 30, 60)
        flammableBlock(FLOATER.get(), 30, 60)

        flammableBlock(FUEL_TANK_FULL_SOLID.get(), 30, 60)
        flammableBlock(FUEL_TANK_FULL_TRANSPARENT.get(), 30, 60)
        flammableBlock(FUEL_TANK_HALF_SOLID.get(), 30, 60)
    }

    fun registerItems(items: DeferredRegister<Item>) {
        ITEMS.forEach { items.register(it.first, it.second) }
    }

}
