package org.valkyrienskies.tournament.ship

import blitz.collections.remove
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.google.common.util.concurrent.AtomicDouble
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.RegisteredFuelType
import org.valkyrienskies.tournament.TickScheduler
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.TournamentFuels
import org.valkyrienskies.tournament.blockentity.PropellerBlockEntity
import org.valkyrienskies.tournament.util.BlockMap
import org.valkyrienskies.tournament.util.SyncBlockMap
import org.valkyrienskies.tournament.util.extension.*
import org.valkyrienskies.tournament.util.helper.convertShipToWorldSpace
import org.valkyrienskies.tournament.util.typed
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
class TournamentShips: ShipPhysicsListener
{
    var level: DimensionId = "minecraft:overworld"
        private set

    @Deprecated("kept for save file compat")
    data class ThrusterData(
        val pos: Vector3i,
        val force: Vector3d,
        val mult: Double,
        @Volatile
        var submerged: Boolean
    )

    data class ThrusterDataV2(
        /** facing normal */
        val dir: Vector3d,
        /** for normal thruster between 0 and 1 * tier */
        @Volatile
        var throttle: Float
    )

    @Deprecated("kept for save file compat")
    private val thrusters =
        CopyOnWriteArrayList<ThrusterData>()

    @Deprecated("kept for save file compat")
    private val thrustersV2 =
        CopyOnWriteArrayList<Pair<Vector3d, ThrusterDataV2>>()

    // todo: use longs as keys?
    private val thrustersV2_2 =
        SyncBlockMap(BlockMap<ThrusterDataV2>())

    fun thrusterV2(pos: BlockPos): ThrusterDataV2? =
        thrustersV2_2[thrustersV2_2.index(pos)]

    private val balloons =
        CopyOnWriteArrayList<Pair<Vector3i, Double>>()

    private val spinners =
        CopyOnWriteArrayList<Pair<Vector3i, Vector3d>>()

    private val pulses =
        CopyOnWriteArrayList<Pair<Vector3d, Vector3d>>()

    @Volatile
    var fuelTypeKey: String? = null

    var fuelType: RegisteredFuelType?
        set (it) {
            fuelTypeKey = it?.id?.toString()
        }
        get() =
            fuelTypeKey?.toResourceLocation()
                ?.typed<RegisteredFuelType>()
                ?.let(TournamentFuels::get)

    @Volatile
    var fuelCount = 0.0f

    @Volatile
    var fuelCap = 0.0f

    /** returns 0-1 depending on how much of the wanted fuel was provider */
    fun useFuel(wanted: Float): Float {
        if (wanted == 0f) return 0f
        val throttle = min(1f, fuelCount / wanted)
        fuelCount -= wanted
        if (fuelCount < 0f) {
            fuelType = null
            fuelCount = 0f
        }
        return throttle
    }

    data class PropellerData(
        val pos: Vector3i,
        val force: Vector3d,
        var speed: AtomicDouble,
        var touchingWater: Boolean
    )

    private val propellers =
        CopyOnWriteArrayList<PropellerData>()

    val ticker by lazy { TickScheduler.everyServerTick(::tickfn) }

    @Deprecated("kept for save file compat")
    var wasLastShutOff = false

    @OptIn(PhysTickOnly::class, VsBeta::class)
    override fun physTick(
        physShip: PhysShip,
        physLevel: PhysLevel
    ) {
        if (fuelCount > fuelCap)
            fuelCount = fuelCap

        if (fuelCount <= 0) {
            fuelCount = 0.0f
            fuelType = null
        }

        ticker.void()

        thrustersV2.forEach { (pos, t) ->
            thrustersV2_2[thrustersV2_2.index(pos.toBlock())] = t
        }
        thrustersV2.clear()

        thrustersV2_2.contents().forEach { (pos, t) ->
            if (t.throttle == 0f) {
                return@forEach
            }

            // actual fuel is used at game tick
            val force = fuelType?.fuel?.getPower(t.throttle) ?: 0.0f

            if (force == 0.0f || !force.isFinite()) {
                return@forEach
            }

            val dirForce = Vector3d(t.dir)
                .mul(force.toDouble())
            val tPos = Vec3.atCenterOf(pos).toJOML()
            physShip.applyModelForce(dirForce, tPos)
        }

        thrusters.forEach { data ->
            val (pos, force, tier, submerged) = data

            if (submerged) {
                return@forEach
            }

            val tForce = physShip.transform.shipToWorld.transformDirection(force, Vector3d())
            val tPos = pos.toDouble().add(0.5, 0.5, 0.5).sub(physShip.transform.positionInShip)

            if (force.isFinite) {
                physShip.applyInvariantForceToPos(tForce.mul(TournamentConfig.SERVER.thrusterSpeed * tier), tPos)
            }
        }

        balloons.forEach {
            val (pos, pow) = it

            val tPos = Vector3d(pos).add(0.5, 0.5, 0.5).sub(physShip.transform.positionInShip)
            val tHeight = physShip.transform.positionInWorld.y()
            var tPValue = TournamentConfig.SERVER.balloonBaseHeight - ((tHeight * tHeight) / 1000.0)

            if (physShip.velocity.y() > 10.0)    {
                tPValue = (-physShip.velocity.y() * 0.25)
                tPValue -= (physShip.velocity.y() * 0.25)
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

    // TODO: move this into the block entities
    private fun tickfn(server: MinecraftServer) {
        val level = server.getLevel(this@TournamentShips.level.toDimensionKey()) ?: return

        thrusters.forEach { t ->
            val water = level.isWaterAt(
                level.convertShipToWorldSpace(t.pos.toDouble()).toBlock())
            t.submerged = water
        }

        propellers.forEach { p ->
            // TODO: check if water is on the outside if big propeller
            val water = level.isWaterAt(
                level.convertShipToWorldSpace(p.pos.toDouble()).toBlock())
            p.touchingWater = water

            val be = level.getBlockEntity(
                p.pos.toBlockPos()
            ) as PropellerBlockEntity<*>?

            if (be != null) {
                p.speed.set(be.speed)
            }
        }
    }

    fun addThrusterV2(
        pos: BlockPos,
        data: ThrusterDataV2
    ) {
        thrustersV2_2[thrustersV2_2.index(pos)] = data
    }

    fun removeThrusterV2(
        pos: BlockPos
    ) {
        thrustersV2_2.remove(thrustersV2_2.index(pos))
    }

    fun addThrustersV1(
        list: Iterable<Triple<Vector3i, Vector3d, Double>>
    ) {
        list.forEach { (pos, force, tier) ->
            thrusters += ThrusterData(pos, force, tier, false)
        }
    }

    fun addBalloon(pos: BlockPos, pow: Double) {
        balloons.add(pos.toJOML() to pow)
    }

    fun addBalloons(list: Iterable<Pair<Vector3i, Double>>) {
        balloons.addAll(list)
    }

    fun removeBalloon(pos: BlockPos) {
        val joml = pos.toJOMLD()
        balloons.removeAll { it.first == joml }
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
        @OptIn(GameTickOnly::class, VsBeta::class)
        fun getOrCreate(ship: LoadedServerShip, level: DimensionId) =
            ship.getAttachment<TournamentShips>()
                ?: TournamentShips().also {
                    it.level = level
                    ship.setAttachment(it)
                }

        @OptIn(GameTickOnly::class)
        fun getOrCreate(ship: LoadedServerShip): TournamentShips =
            getOrCreate(ship, ship.chunkClaimDimension)

        @OptIn(GameTickOnly::class)
        fun getOrCreate(level: Level, pos: BlockPos)  =
            ((level.getLoadedShipManagingPos(pos)
                ?: level.getShipManagingPos(pos))
                    as? LoadedServerShip)?.let { getOrCreate(it) }
    }
}
