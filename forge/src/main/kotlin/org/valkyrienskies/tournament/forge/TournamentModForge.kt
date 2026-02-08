package org.valkyrienskies.tournament.forge

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.valkyrienskies.tournament.TickScheduler
import org.valkyrienskies.tournament.TournamentConfigUpdater
import org.valkyrienskies.tournament.TournamentItems.TAB
import org.valkyrienskies.tournament.TournamentMod
import org.valkyrienskies.tournament.TournamentMod.init
import org.valkyrienskies.tournament.TournamentMod.initClient
import org.valkyrienskies.tournament.TournamentMod.initClientRenderers
import org.valkyrienskies.tournament.registry.CreativeTabs.create

@Mod(TournamentMod.MOD_ID)
class TournamentModForge {
    companion object {
        @JvmField
        val modBus: IEventBus = Bus.MOD.bus().get()
        @JvmField
        val forgeBus: IEventBus = Bus.FORGE.bus().get()
    }

    init {
        TournamentConfigUpdater.ALL_CONFIGS.forEach {
            ModLoadingContext.get().registerConfig(it.forgeType(), it.spec)
        }

        forgeBus.addListener { event: ServerTickEvent ->
            TickScheduler.tickServer(event.server)
        }

        // TODO: do this loader independently, and make sure ordering tab correctly
        modBus.addListener { _: FMLCommonSetupEvent ->
            Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                TAB,
                create()
            )
        }

        modBus.addListener { _: FMLClientSetupEvent ->
            setupClient
        }

        modBus.addListener { event: RegisterRenderers ->
            entityRenderers(
                event
            )
        }

        init()
    }

    private val setupClient by lazy {
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
}