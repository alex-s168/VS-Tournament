package org.valkyrienskies.tournament.ship

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.attachment.removeAttachment
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.ships.saveAttachment
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.tournament.TickScheduler
import java.util.concurrent.CopyOnWriteArrayList

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
/**
 * for compat only!!
 * @see TournamentShips
  */
@Deprecated("Use TournamentShips instead")
class tournamentShipControl : ShipPhysicsListener {

    private var extraForce = 0.0
    private var physConsumption = 0f
    var power = 0.0
    var consumed = 0f
        private set

    private val Balloons = mutableListOf<Pair<Vector3i, Double>>()
    private val Spinners = mutableListOf<Pair<Vector3i, Vector3d>>()
    private val Thrusters = mutableListOf<Triple<Vector3i, Vector3d, Double>>()
    private val Pulses = CopyOnWriteArrayList<Pair<Vector3d, Vector3d>>()

    companion object {
        @OptIn(GameTickOnly::class, VsBeta::class)
        private fun migrate(id: ShipId) {
            vsApi.getServerShipWorld()?.loadedShips?.getById(id)?.let { ship ->
                ship.getAttachment<tournamentShipControl>()?.apply {
                    println("Converting old ship controller (\"tournamentShipControl\") of ship $id to new")

                    val tournamentShips = TournamentShips.getOrCreate(ship)

                    tournamentShips.addBalloons(Balloons)
                    Balloons.clear()

                    tournamentShips.addThrustersV1(Thrusters)
                    Thrusters.clear()

                    tournamentShips.addSpinners(Spinners)
                    Spinners.clear()

                    tournamentShips.addPulses(Pulses)
                    Pulses.clear()

                    ship.removeAttachment<tournamentShipControl>()
                }
            }
        }
    }

    @OptIn(PhysTickOnly::class, VsBeta::class)
    override fun physTick(
        ship: PhysShip,
        physLevel: PhysLevel
    ) {
        val id = ship.id
        TickScheduler.serverTickOnce {
            migrate(id)
        }
    }

}