package org.valkyrienskies.tournament

import dev.architectury.platform.Platform
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.attachment.removeAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.tournament.ship.*
import org.valkyrienskies.tournament.util.extension.void
import org.valkyrienskies.tournament.util.extension.with
import java.io.File

// TODO: remove chunkloader if can't fix
// TODO: replace all "vs_tournament" with Tournament.MOD_ID

@OptIn(GameTickOnly::class, VsBeta::class)
private fun migrateShipController(ship: LoadedServerShip) {
    val thrusterShipCtrl = ship.getAttachment<ThrusterShipControl>()
    if (thrusterShipCtrl != null) {
        TournamentShips.getOrCreate(ship).addThrustersV1(thrusterShipCtrl.Thrusters.with(thrusterShipCtrl.thrusters))
        ship.removeAttachment<ThrusterShipControl>()
    }

    val balloonShipCtrl = ship.getAttachment<BalloonShipControl>()
    if (balloonShipCtrl != null) {
        TournamentShips.getOrCreate(ship).addBalloons(balloonShipCtrl.balloons)
        ship.removeAttachment<BalloonShipControl>()
    }

    val spinnerShipCtrl = ship.getAttachment<SpinnerShipControl>()
    if (spinnerShipCtrl != null) {
        TournamentShips.getOrCreate(ship).addSpinners(spinnerShipCtrl.spinners.with(spinnerShipCtrl.Spinners))
        ship.removeAttachment<SpinnerShipControl>()
    }

    val pulsesShipCtrl = ship.getAttachment<PulseShipControl>()
    if (pulsesShipCtrl != null) {
        pulsesShipCtrl.addToNew(TournamentShips.getOrCreate(ship))
        ship.removeAttachment<PulseShipControl>()
    }
}

object TournamentMod {
    const val MOD_ID = "vs_tournament"

    @JvmField
    var configFolder: File? = null

    @OptIn(VsBeta::class)
    @JvmStatic
    fun init() {
        configFolder = Platform.getConfigFolder().toFile()

        TournamentFuelManager.void()
        TournamentNetworking.register
        TournamentBlocks.register
        TournamentBlockEntities.register
        TournamentItems.register
        TournamentWeights.register
        TournamentTriggers.register
        TournamentWorldGen.register

        vsApi.registerAttachment(BalloonShipControl::class.java)
        vsApi.registerAttachment(PulseShipControl::class.java)
        vsApi.registerAttachment(SpinnerShipControl::class.java)
        vsApi.registerAttachment(ThrusterShipControl::class.java)
        vsApi.registerAttachment(tournamentShipControl::class.java)
        vsApi.registerAttachment(TournamentShips::class.java)

        VSEvents.shipLoadEvent.on { e ->
            migrateShipController(e.ship)
        }
    }

    @JvmStatic
    fun initClient() {

    }

    interface ClientRenderers {
        fun <T: BlockEntity> registerBlockEntityRenderer(t: BlockEntityType<T>, r: BlockEntityRendererProvider<T>)
    }

    @JvmStatic
    fun initClientRenderers(clientRenderers: ClientRenderers) {
        TournamentBlockEntities.initClientRenderers(clientRenderers)
    }
}
