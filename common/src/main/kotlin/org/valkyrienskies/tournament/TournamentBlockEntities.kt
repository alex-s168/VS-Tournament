package org.valkyrienskies.tournament

import net.minecraft.Util
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.tournament.blockentity.*
import org.valkyrienskies.tournament.blockentity.explosive.ExplosiveBlockEntity
import org.valkyrienskies.tournament.blockentity.render.PropellerBlockEntityRender
import org.valkyrienskies.tournament.blockentity.render.RotatorBlockEntityRender
import org.valkyrienskies.tournament.blockentity.render.SensorBlockEntityRender
import org.valkyrienskies.tournament.blockentity.render.TransparentFuelTankBlockEntityRender
import org.valkyrienskies.tournament.registry.DeferredRegister
import org.valkyrienskies.tournament.registry.RegistrySupplier
import org.valkyrienskies.tournament.util.extension.once

@Suppress("unused")
object TournamentBlockEntities {
    private val BLOCKENTITIES = DeferredRegister.create(TournamentMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    private val renderers = mutableListOf<RendererEntry<*>>()

    lateinit var CONNECTOR: RegistrySupplier<BlockEntityType<ConnectorBlockEntity>>
    lateinit var SENSOR: RegistrySupplier<BlockEntityType<SensorBlockEntity>>
    lateinit var ROPE_HOOK: RegistrySupplier<BlockEntityType<RopeHookBlockEntity>>
    lateinit var PROP_BIG: RegistrySupplier<BlockEntityType<BigPropellerBlockEntity>>
    lateinit var PROP_SMALL: RegistrySupplier<BlockEntityType<SmallPropellerBlockEntity>>
    lateinit var CHUNK_LOADER: RegistrySupplier<BlockEntityType<ChunkLoaderBlockEntity>>
    lateinit var EXPLOSIVE: RegistrySupplier<BlockEntityType<ExplosiveBlockEntity>>
    lateinit var FUEL_TANK_FULL_SOLID: RegistrySupplier<BlockEntityType<FuelTankBlockEntity>>
    lateinit var FUEL_TANK_FULL_TRANSPARENT: RegistrySupplier<BlockEntityType<FuelTankBlockEntity>>
    lateinit var FUEL_TANK_HALF_SOLID: RegistrySupplier<BlockEntityType<FuelTankBlockEntity>>
    lateinit var ROTATOR: RegistrySupplier<BlockEntityType<RotatorBlockEntity>>

    init {
        /* ================================================================== */
        CONNECTOR = TournamentBlocks.CONNECTOR
            .withBE(::ConnectorBlockEntity)
            .byName("connector")
        /* ================================================================== */
        SENSOR = TournamentBlocks.SENSOR
            .withBE(::SensorBlockEntity)
            .byName("sensor")
            .withRenderer {
                SensorBlockEntityRender()
            }
        /* ================================================================== */
        ROPE_HOOK = TournamentBlocks.ROPE_HOOK
            .withBE(::RopeHookBlockEntity)
            .byName("rope_hook")
        /* ================================================================== */
        PROP_BIG = TournamentBlocks.PROP_BIG
            .withBE(::BigPropellerBlockEntity)
            .byName("prop_big")
            .withRenderer {
                PropellerBlockEntityRender<BigPropellerBlockEntity>(
                    TournamentModels.PROP_BIG
                )
            }
        /* ================================================================== */
        PROP_SMALL = TournamentBlocks.PROP_SMALL
            .withBE(::SmallPropellerBlockEntity)
            .byName("prop_small")
            .withRenderer {
                PropellerBlockEntityRender<SmallPropellerBlockEntity>(
                    TournamentModels.PROP_SMALL
                )
            }
        /* ================================================================== */
        CHUNK_LOADER = TournamentBlocks.CHUNK_LOADER
            .withBE(::ChunkLoaderBlockEntity)
            .byName("chunk_loader")
        /* ================================================================== */
        EXPLOSIVE = TournamentBlocks.EXPLOSIVE_INSTANT_SMALL
            .withBE(::ExplosiveBlockEntity)
            .byName("explosive_instant_small")
        /* ================================================================== */
        FUEL_TANK_FULL_SOLID = TournamentBlocks.FUEL_TANK_FULL_SOLID
            .withBE { p, s -> FuelTankBlockEntity(p, s, capf = 1.0f, FUEL_TANK_FULL_SOLID::get) }
            .byName("fuel_tank_full_solid")
        /* ================================================================== */
        FUEL_TANK_FULL_TRANSPARENT = TournamentBlocks.FUEL_TANK_FULL_TRANSPARENT
            .withBE { p, s -> FuelTankBlockEntity(p, s, capf = 1.0f, FUEL_TANK_FULL_TRANSPARENT::get) }
            .byName("fuel_tank_full_transparent")
            .withRenderer(::TransparentFuelTankBlockEntityRender)
        /* ================================================================== */
        FUEL_TANK_HALF_SOLID = TournamentBlocks.FUEL_TANK_HALF_SOLID
            .withBE { p, s -> FuelTankBlockEntity(p, s, capf = 0.5f, FUEL_TANK_HALF_SOLID::get) }
            .byName("fuel_tank_half_solid")
        /* ================================================================== */
        ROTATOR              = TournamentBlocks.ROTATOR
            .withBE(::RotatorBlockEntity)
            .byName("rotator")
            .withRenderer(::RotatorBlockEntityRender)
        /* ================================================================== */

    }

    val register by once {
        BLOCKENTITIES.applyAll()
    }

    private infix fun <T : BlockEntity> Set<RegistrySupplier<out Block>>.withBE(blockEntity: (BlockPos, BlockState) -> T) =
        Pair(this, blockEntity)

    private infix fun <T : BlockEntity> RegistrySupplier<out Block>.withBE(blockEntity: (BlockPos, BlockState) -> T) =
        Pair(setOf(this), blockEntity)

    private infix fun <T : BlockEntity> Block.withBE(blockEntity: (BlockPos, BlockState) -> T) = Pair(this, blockEntity)

    private data class RendererEntry<T: BlockEntity>(
        val type: RegistrySupplier<BlockEntityType<T>>,
        val renderer: () -> Any
    )

    @Suppress("UNCHECKED_CAST")
    fun initClientRenderers(clientRenderers: TournamentMod.ClientRenderers) {
        renderers.forEach { x ->
            val rp = BlockEntityRendererProvider {
                x.renderer() as BlockEntityRenderer<BlockEntity>
            }
            clientRenderers.registerBlockEntityRenderer(
                x.type.get() as BlockEntityType<BlockEntity>,
                rp
            )
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private infix fun <T : BlockEntity> Pair<Set<RegistrySupplier<out Block>>, (BlockPos, BlockState) -> T>.byName(name: String): RegistrySupplier<BlockEntityType<T>> =
        BLOCKENTITIES.register(name) {
            val type = Util.fetchChoiceType(References.BLOCK_ENTITY, name)

            BlockEntityType.Builder.of(
                this.second,
                *this.first.map { it.get() }.toTypedArray()
            ).build(type)
        }

    private infix fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.withRenderer(renderer: () -> Any) =
        this.also {
            renderers += RendererEntry(it, renderer)
        }
}
