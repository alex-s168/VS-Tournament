package org.valkyrienskies.tournament

import blitz.Provider
import net.minecraft.Util
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
import kotlin.reflect.KProperty

object TournamentBlockEntities {
    private val BLOCKENTITIES = DeferredRegister.create(TournamentMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    private val renderers = mutableListOf<RendererEntry<*>>()

    val CONNECTOR = TournamentBlocks.CONNECTOR
        .withBE(::ConnectorBlockEntity)
        .byName("connector")

    val THRUSTER = TournamentBlocks.THRUSTER
        .withBE(::ThrusterBlockEntity)
        .byName("thruster")

    val SENSOR = TournamentBlocks.SENSOR
        .withBE(::SensorBlockEntity)
        .byName("sensor")
        .withRenderer(object : RenderProviderProvider<SensorBlockEntity> {
            override fun get() = BlockEntityRendererProvider {
                SensorBlockEntityRender()
            }
        })

    val ROPE_HOOK = TournamentBlocks.ROPE_HOOK
        .withBE(::RopeHookBlockEntity)
        .byName("rope_hook")

    val PROP_BIG = TournamentBlocks.PROP_BIG
        .withBE(::BigPropellerBlockEntity)
        .byName("prop_big")
        .withRenderer(object : RenderProviderProvider<BigPropellerBlockEntity> {
            override fun get() = BlockEntityRendererProvider {
                PropellerBlockEntityRender<BigPropellerBlockEntity>(
                    TournamentModels.PROP_BIG
                )
            }
        })

    val PROP_SMALL = TournamentBlocks.PROP_SMALL
        .withBE(::SmallPropellerBlockEntity)
        .byName("prop_small")
        .withRenderer(object : RenderProviderProvider<SmallPropellerBlockEntity> {
            override fun get() = BlockEntityRendererProvider {
                PropellerBlockEntityRender<SmallPropellerBlockEntity>(
                    TournamentModels.PROP_SMALL
                )
            }
        })

    val CHUNK_LOADER = TournamentBlocks.CHUNK_LOADER
        .withBE(::ChunkLoaderBlockEntity)
        .byName("chunk_loader")

    val EXPLOSIVE = TournamentBlocks.EXPLOSIVE_INSTANT_SMALL
        .withBE(::ExplosiveBlockEntity)
        .byName("explosive_instant_small")

    val FUEL_TANK_FULL_SOLID by rec { self ->
        TournamentBlocks.FUEL_TANK_FULL_SOLID
            .withBE { p, s -> FuelTankBlockEntity(p, s, capf = 1.0f, self) }
            .byName("fuel_tank_full_solid")
    }

    val FUEL_TANK_FULL_TRANSPARENT by rec { self ->
        TournamentBlocks.FUEL_TANK_FULL_TRANSPARENT
            .withBE { p, s -> FuelTankBlockEntity(p, s, capf = 1.0f, self) }
            .byName("fuel_tank_full_transparent")
            .withRenderer(object : RenderProviderProvider<FuelTankBlockEntity> {
                override fun get() = BlockEntityRendererProvider { TransparentFuelTankBlockEntityRender() }
            })
    }

    val FUEL_TANK_HALF_SOLID by rec { self ->
        TournamentBlocks.FUEL_TANK_HALF_SOLID
            .withBE { p, s -> FuelTankBlockEntity(p, s, capf = 0.5f, self) }
            .byName("fuel_tank_half_solid")
    }

    val ROTATOR = TournamentBlocks.ROTATOR
        .withBE(::RotatorBlockEntity)
        .byName("rotator")
        .withRenderer(object : RenderProviderProvider<RotatorBlockEntity> {
            override fun get() = BlockEntityRendererProvider { RotatorBlockEntityRender() }
        })

    val register by once {
        BLOCKENTITIES.applyAll()
    }

    private infix fun <T : BlockEntity> Set<RegistrySupplier<out Block>>.withBE(blockEntity: (BlockPos, BlockState) -> T) =
        Pair(this, blockEntity)

    private infix fun <T : BlockEntity> RegistrySupplier<out Block>.withBE(blockEntity: (BlockPos, BlockState) -> T) =
        setOf(this).withBE(blockEntity)

    private data class RendererEntry<T: BlockEntity>(
        val type: RegistrySupplier<BlockEntityType<T>>,
        val renderer: RenderProviderProvider<T>
    ) {
        class ClientOnly<T: BlockEntity>(val entry: RendererEntry<T>) {
            fun register(clientRenderers: TournamentMod.ClientRenderers) {
                clientRenderers.registerBlockEntityRenderer(
                    entry.type.get(),
                    entry.renderer.get()
                )
            }
        }

        fun register(clientRenderers: TournamentMod.ClientRenderers) {
            ClientOnly(this).register(clientRenderers)
        }
    }

    fun initClientRenderers(clientRenderers: TournamentMod.ClientRenderers) {
        renderers.forEach {
            it.register(clientRenderers)
        }
    }

    private infix fun <T : BlockEntity> Pair<Set<RegistrySupplier<out Block>>, (BlockPos, BlockState) -> T>.byName(name: String): RegistrySupplier<BlockEntityType<T>> =
        BLOCKENTITIES.register(name) {
            val type = Util.fetchChoiceType(References.BLOCK_ENTITY, name)

            BlockEntityType.Builder.of(
                this.second,
                *this.first.map { it.get() }.toTypedArray()
            ).build(type)
        }

    private infix fun <T : BlockEntity> RegistrySupplier<BlockEntityType<T>>.withRenderer(renderer: RenderProviderProvider<T>) =
        this.also {
            renderers += RendererEntry(it, renderer)
        }

    class rec<T>(fn: (Provider<T>) -> RegistrySupplier<T>) {
        val value: RegistrySupplier<T> = fn { value.get() }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): RegistrySupplier<T> {
            return value
        }
    }

    // because class loading!!!
    interface RenderProviderProvider<T: BlockEntity> {
        val a: Int get() = 1 // keep!!!
        fun get(): BlockEntityRendererProvider<T>
    }
}
