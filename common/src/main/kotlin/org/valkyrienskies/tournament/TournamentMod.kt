package org.valkyrienskies.tournament

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.attachment.removeAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.tournament.ship.*
import org.valkyrienskies.tournament.util.extension.with

// TODO: remove chunkloader if can't fix
// TODO: replace all "vs_tournament" with Tournament.MOD_ID

@OptIn(GameTickOnly::class, VsBeta::class)
private fun migrateShipController(ship: LoadedServerShip) {
    if (TournamentConfig.SERVER.removeAllAttachments) {
        ship.removeAttachment<BalloonShipControl>()
        ship.removeAttachment<PulseShipControl>()
        ship.removeAttachment<SpinnerShipControl>()
        ship.removeAttachment<ThrusterShipControl>()
        ship.removeAttachment<tournamentShipControl>()
        ship.removeAttachment<TournamentShips>()
    }
    else {
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
}

object TournamentMod {
    const val MOD_ID = "vs_tournament"

    @JvmStatic
    fun init() {
        TournamentNetworking.register
        TournamentBlocks.register
        TournamentBlockEntities.register
        TournamentItems.register
        TournamentWeights.register
        TournamentTriggers.register
        TournamentWorldGen.register

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
