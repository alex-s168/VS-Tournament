package org.valkyrienskies.tournament.forge

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.valkyrienskies.tournament.TickScheduler
import org.valkyrienskies.tournament.TournamentConfigUpdater
import org.valkyrienskies.tournament.TournamentItems.TAB
import org.valkyrienskies.tournament.TournamentMod
import org.valkyrienskies.tournament.TournamentMod.init
import org.valkyrienskies.tournament.TournamentMod.initClient
import org.valkyrienskies.tournament.TournamentMod.initClientRenderers
import org.valkyrienskies.tournament.TournamentModels
import org.valkyrienskies.tournament.registry.CreativeTabs.create
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(TournamentMod.MOD_ID)
class TournamentModForge {

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(TournamentMod.MOD_ID)

    init {
        TournamentConfigUpdater.ALL_CONFIGS.forEach {
            ModLoadingContext.get().registerConfig(it.forgeType(), it.spec)
        }

        FORGE_BUS.addListener { event: ServerTickEvent ->
            TickScheduler.tickServer(event.server)
        }

        MOD_BUS.addListener { _: FMLCommonSetupEvent ->
            Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                TAB,
                create()
            )
        }

        MOD_BUS.addListener { event: FMLClientSetupEvent? ->
            setupClient
        }

        MOD_BUS.addListener { event: ModelEvent.RegisterAdditional ->
            TournamentModels.MODELS.forEach { rl ->
                LOGGER.info("Registering model $rl")
                event.register(rl)
            }
        }

        MOD_BUS.addListener { event: RegisterRenderers ->
            entityRenderers(
                event
            )
        }

        init()
    }

    val setupClient by lazy {
        initClient()
    }

    private fun entityRenderers(event: RegisterRenderers) {
        initClientRenderers(
            object : TournamentMod.ClientRenderers {
                override fun <T : BlockEntity> registerBlockEntityRenderer(
                    t: BlockEntityType<T>,
                    r: BlockEntityRendererProvider<T>
                ) = event.registerBlockEntityRenderer(t, r)
            }
        )
    }

    companion object {
        fun getModBus(): IEventBus = MOD_BUS
    }
}