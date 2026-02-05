package org.valkyrienskies.tournament.ship

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Vector3d
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.tournament.util.extension.void
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
class PulseShipControl: ShipPhysicsListener {

    // for compat only!!
    private val Pulses = CopyOnWriteArrayList<Pair<Vector3d, Vector3d>>()

    private val pulses = CopyOnWriteArrayList<Pair<Vector3d, Vector3d>>()

    fun addToNew(ship: TournamentShips) {
        ship.addPulses(pulses)
        ship.addPulses(Pulses)
    }

    @OptIn(PhysTickOnly::class, VsBeta::class)
    override fun physTick(
        physShip: PhysShip,
        physLevel: PhysLevel
    ) {}

}