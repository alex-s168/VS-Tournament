package org.valkyrienskies.tournament.ship

import blitz.Either
import blitz.collections.remove
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.util.concurrent.AtomicDouble
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.*
import org.valkyrienskies.tournament.blockentity.PropellerBlockEntity
import org.valkyrienskies.tournament.util.BlockMap
import org.valkyrienskies.tournament.util.SyncBlockMap
import org.valkyrienskies.tournament.util.extension.toBlock
import org.valkyrienskies.tournament.util.extension.toDimensionKey
import org.valkyrienskies.tournament.util.extension.toDouble
import org.valkyrienskies.tournament.util.extension.toResourceLocation
import org.valkyrienskies.tournament.util.helper.Helper3d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
class TournamentShips: ShipPhysicsListener {

    var level: DimensionId = "minecraft:overworld"
        private set

    data class ThrusterData(
        val pos: Vector3i,
        val force: Vector3d,
        val mult: Double,
        @Volatile
        var submerged: Boolean
    )

    data class ThrusterDataV2(
        val dir: Vector3d,
        // for normal thruster between 0 and 15 * max tier
        @Volatile
        var throttle: Float,
        @Volatile
        var submerged: Boolean = false,
        @Volatile
        var lastPower: Float = 0.0f,
    )

    @JsonIgnore
    private val toUpdateV2 = ConcurrentLinkedQueue<Long>()

    fun updateThrusterV2(pos: BlockPos) {
        toUpdateV2.add(pos.asLong())
    }

    fun allThrusters() =
        mutableListOf<Pair<BlockPos, Either<ThrusterData, ThrusterDataV2>>>().also { res ->
            res += thrusters.map { it.pos.toBlockPos() to Either.ofA(it) }
            res += thrustersV2.map { (pos, data) -> pos.toBlock() to Either.ofB(data) }
            res += thrustersV2_2.contents().map { (pos, data) -> pos to Either.ofB(data) }
        }

    private val thrusters =
        CopyOnWriteArrayList<ThrusterData>()

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
            fuelTypeKey?.toResourceLocation()?.let(TournamentFuels.REGISTRY::get)

    @Volatile
    var fuelCount = 0.0f

    @Volatile
    var fuelCap = 0.0f

    fun useFuel(count: Float) {
        fuelCount -= count
        if (fuelCount < 0f) {
            fuelType = null
            fuelCount = 0f
        }
    }

    fun useFuelThrottlePreview(throttle: Float): Float =
        fuelType?.fuel?.getPower(throttle)
            ?: 0.0f

    fun useFuelThrottle(throttle: Float, mult: Int = 1): Float =
        fuelType?.let {
            useFuel(it.fuel.getBurnRate(throttle) * mult)
            it.fuel.getPower(throttle)
        } ?: 0.0f

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

    @Deprecated("kept for save file compat")
    var wasLastShutOff = false

    fun dryForce(thruster: ThrusterDataV2): Vector3d {
        // actual fuel is used at game tick
        val force = useFuelThrottlePreview(thruster.throttle)

        var fact = 1.0
        if (!force.isFinite() || thruster.submerged) {
            fact = 0.0
        }

        return thruster.dir.mul(force.toDouble()).mul(fact)
    }

    fun dryForce(thruster: ThrusterData): Vector3d {
        var fact = 1.0
        if (thruster.submerged) {
            fact = 0.0
        }

        return thruster.force.mul(thruster.mult * TournamentConfig.SERVER.thrusterSpeed * fact)
    }

    @JsonIgnore
    private var lastFuelType = fuelType
    @JsonIgnore
    private val filteredUpdates = mutableSetOf<Long>() // TODO: replace with faster long set

    @OptIn(PhysTickOnly::class, VsBeta::class)
    override fun physTick(
        physShip: PhysShip,
        physLevel: PhysLevel
    ) {
        toUpdateV2.pollUntilEmpty {
            filteredUpdates.add(it)
        }

        filteredUpdates.forEach { packed ->
            val pos = BlockPos.of(packed)

            val throttle = thrusterV2(pos)
                ?.throttle
                ?: -1.0f // remove

            TournamentNetworking.ShipThrusterChange(
                physShip.id,
                packed,
                throttle
            ).send()
        }

        if (fuelCount > fuelCap)
            fuelCount = fuelCap

        if (fuelCount <= 0) {
            fuelCount = 0.0f
            fuelType = null
        }

        if (fuelType != lastFuelType) {
            TournamentNetworking.ShipFuelTypeChange(
                physShip.id,
                fuelType?.id
            ).send()
            lastFuelType = fuelType
        }

        if (ticker == null) {
            ticker = TickScheduler.everyServerTick(::tickfn)
            TournamentNetworking.ShipFuelTypeChange(
                physShip.id,
                fuelType?.id
            ).send()
        }

        thrustersV2.forEach { (pos, t) ->
            thrustersV2_2[thrustersV2_2.index(pos.toBlock())] = t
        }
        thrustersV2.clear()

        thrustersV2_2.contents().forEach { (pos, t) ->
            if (t.submerged) {
                t.lastPower = 0.0f
                return@forEach
            }

            // actual fuel is used at game tick
            val force = useFuelThrottlePreview(t.throttle)

            if (force == 0.0f || !force.isFinite()) {
                t.lastPower = 0.0f
                return@forEach
            }

            t.lastPower = force

            val tForce = physShip.transform.shipToWorld.transformDirection(t.dir, Vector3d())
            tForce.mul(force.toDouble())
            val tPos = pos.toJOMLD()
                .add(0.5, 0.5, 0.5, Vector3d())
                .sub(physShip.transform.positionInShip)

            physShip.applyInvariantForceToPos(tForce, tPos)
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

    private fun tickfn(server: MinecraftServer) {
        val lvl = server.getLevel(level.toDimensionKey()) ?: return

        thrusters.forEach { t ->
            val water = lvl.isWaterAt(
                Helper3d
                    .convertShipToWorldSpace(lvl, t.pos.toDouble())
                    .toBlock()
            )
            t.submerged = water
        }

        thrustersV2_2.contents().forEach { (pos, t) ->
            val water = lvl.isWaterAt(
                Helper3d
                    .convertShipToWorldSpace(lvl, pos.toJOMLD())
                    .toBlock()
            )
            t.submerged = water

            if (!water) {
                useFuelThrottle(t.throttle)
            }
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
        fun get(level: Level, pos: BlockPos)  =
            ((level.getLoadedShipManagingPos(pos)
                ?: level.getShipManagingPos(pos))
                    as? LoadedServerShip)?.let { getOrCreate(it) }
    }

    @Environment(EnvType.CLIENT)
    object Client {
        private val ships = ConcurrentHashMap<ShipId, Data>()

        operator fun get(ship: ShipId) =
            ships.computeIfAbsent(ship) {
                Data(
                    AtomicReference(null),
                    SyncBlockMap(BlockMap())
                )
            }

        operator fun get(ship: Ship) =
            get(ship.id)

        data class Data(
            val fuelType: AtomicReference<RegisteredFuelType?>,
            val thrusters: SyncBlockMap<Thruster>,
        ) {
            data class Thruster(
                val throttle: Float,
            )
        }
    }
}
