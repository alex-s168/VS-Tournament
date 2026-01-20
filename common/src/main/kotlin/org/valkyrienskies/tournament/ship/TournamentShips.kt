package org.valkyrienskies.tournament.ship

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.util.concurrent.AtomicDouble
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.mod.common.util.settings
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.tournament.TickScheduler
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.blockentity.PropellerBlockEntity
import org.valkyrienskies.tournament.util.extension.toBlock
import org.valkyrienskies.tournament.util.extension.toDimensionKey
import org.valkyrienskies.tournament.util.extension.toDouble
import org.valkyrienskies.tournament.util.helper.Helper3d
import java.util.concurrent.CopyOnWriteArrayList

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
class TournamentShips: ShipPhysicsListener {

    var level: DimensionId = "minecraft:overworld"

    data class ThrusterData(
        val pos: Vector3i,
        val force: Vector3d,
        val mult: Double,
        var submerged: Boolean
    )

    val thrusters =
        CopyOnWriteArrayList<ThrusterData>()

    private val balloons =
        CopyOnWriteArrayList<Pair<Vector3i, Double>>()

    private val spinners =
        CopyOnWriteArrayList<Pair<Vector3i, Vector3d>>()

    private val pulses =
        CopyOnWriteArrayList<Pair<Vector3d, Vector3d>>()

    data class PropellerData(
        val pos: Vector3i,
        val force: Vector3d,
        var speed: AtomicDouble,
        var touchingWater: Boolean
    )

    private val propellers =
        CopyOnWriteArrayList<PropellerData>()

    @JsonIgnore
    private var ticker: TickScheduler.Ticking? = null

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        physShip as PhysShipImpl

        if (ticker == null) {
            ticker = TickScheduler.serverTickPerm { server ->
                val lvl = server.getLevel(level.toDimensionKey())
                    ?: return@serverTickPerm

                thrusters.forEach { t ->
                    val water = lvl.isWaterAt(
                        Helper3d
                            .convertShipToWorldSpace(lvl, t.pos.toDouble())
                            .toBlock()
                    )
                    t.submerged = water
                }

                propellers.forEach { p ->
                    // TODO: check if water is on the outside if big propeller
                    val water = lvl.isWaterAt(
                        Helper3d
                            .convertShipToWorldSpace(lvl, p.pos.toDouble())
                            .toBlock()
                    )
                    p.touchingWater = water

                    val be = lvl.getBlockEntity(
                        p.pos.toBlockPos()
                    ) as PropellerBlockEntity<*>?

                    if (be != null) {
                        p.speed.set(be.speed)
                    }
                }
            }
        }

        val vel = physShip.velocity

        thrusters.forEach { data ->
            val (pos, force, tier, submerged) = data

            if (submerged) {
                return@forEach
            }

            val tForce = physShip.transform.shipToWorld.transformDirection(force, Vector3d())
            val tPos = pos.toDouble().add(0.5, 0.5, 0.5).sub(physShip.transform.positionInShip)

            if (force.isFinite && (
                TournamentConfig.SERVER.thrusterShutoffSpeed == -1.0
                    || physShip.velocity.length() < TournamentConfig.SERVER.thrusterShutoffSpeed
                )
            ) {
                physShip.applyInvariantForceToPos(tForce.mul(TournamentConfig.SERVER.thrusterSpeed * tier), tPos)
            }
        }

        balloons.forEach {
            val (pos, pow) = it

            val tPos = Vector3d(pos).add(0.5, 0.5, 0.5).sub(physShip.transform.positionInShip)
            val tHeight = physShip.transform.positionInWorld.y()
            var tPValue = TournamentConfig.SERVER.balloonBaseHeight - ((tHeight * tHeight) / 1000.0)

            if (vel.y() > 10.0)    {
                tPValue = (-vel.y() * 0.25)
                tPValue -= (vel.y() * 0.25)
            }
            if(tPValue <= 0){
                tPValue = 0.0
            }
            physShip.applyInvariantForceToPos(
                Vector3d(
                    0.0,
                    (pow + 1.0) * TournamentConfig.SERVER.balloonPower * tPValue,
                    0.0
                ),
                tPos
            )
        }

        spinners.forEach {
            val (_, torque) = it    // TODO: WATF

            val torqueGlobal = physShip.transform.shipToWorldRotation.transform(torque, Vector3d())

            physShip.applyInvariantTorque(torqueGlobal.mul(TournamentConfig.SERVER.spinnerSpeed))
        }

        pulses.forEach {
            val (pos, force) = it
            val tPos = pos.add(0.5, 0.5, 0.5).sub(physShip.transform.positionInShip)
            val tForce = physShip.transform.worldToShip.transformDirection(force)

            physShip.applyRotDependentForceToPos(tForce, tPos)
        }
        pulses.clear()

        propellers.forEach {
            val (pos, force, speed, touchingWater) = it

            if (!touchingWater) {
                return@forEach
            }

            val tPos = pos.toDouble().add(0.5, 0.5, 0.5).sub(physShip.transform.positionInShip)
            val tForce = physShip.transform.shipToWorld.transformDirection(force, Vector3d())

            physShip.applyInvariantForceToPos(tForce.mul(speed.get()), tPos)
        }
    }

    fun addThruster(
        pos: BlockPos,
        tier: Double,
        force: Vector3d
    ) {
        thrusters += ThrusterData(pos.toJOML(), force, tier, false)
    }

    fun addThrusters(
        list: Iterable<Triple<Vector3i, Vector3d, Double>>
    ) {
        list.forEach { (pos, force, tier) ->
            thrusters += ThrusterData(pos, force, tier, false)
        }
    }

    fun stopThruster(
        pos: BlockPos
    ) {
        thrusters.removeIf { pos.toJOML() == it.pos }
    }

    fun addBalloon(pos: BlockPos, pow: Double) {
        balloons.add(pos.toJOML() to pow)
    }

    fun addBalloons(list: Iterable<Pair<Vector3i, Double>>) {
        balloons.addAll(list)
    }

    fun removeBalloon(pos: BlockPos) {
        balloons.removeAll { it.first == pos.toJOML() }
    }

    fun addSpinner(pos: Vector3i, torque: Vector3d) {
        spinners.add(pos to torque)
    }

    fun addSpinners(list: Iterable<Pair<Vector3i, Vector3d>>) {
        spinners.addAll(list)
    }

    fun removeSpinner(pos: Vector3i) {
        spinners.removeAll { it.first == pos }
    }

    fun addPulse(pos: Vector3d, force: Vector3d) {
        pulses.add(pos to force)
    }

    fun addPulses(list: Iterable<Pair<Vector3d, Vector3d>>) {
        pulses.addAll(list)
    }

    fun addPropeller(pos: Vector3i, force: Vector3d) {
        propellers += PropellerData(pos, force, AtomicDouble(), false)
    }

    fun removePropeller(pos: Vector3i) {
        propellers.removeIf { it.pos == pos }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip, level: DimensionId) =
            ship.getAttachment(TournamentShips::class.java)
                ?: TournamentShips().also {
                    it.level = level
                    ship.setAttachment(it)
                }

        fun getOrCreate(ship: LoadedServerShip): TournamentShips =
            getOrCreate(ship, ship.chunkClaimDimension)
    }
}