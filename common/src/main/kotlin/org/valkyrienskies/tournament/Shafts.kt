package org.valkyrienskies.tournament

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ShaftStats(
    var force: Float = 0.0f,
    var speed: Float = 0.0f, // 0 to 1
) {
    fun reset() {
        force = 0.0f
        speed = 0.0f
    }
}

interface ShaftConsumer {
    fun wantSpeeds(level: ServerLevel, pos: BlockPos, state: BlockState): ClosedFloatingPointRange<Float>
    fun consume(level: ServerLevel, pos: BlockPos, state: BlockState, stats: ShaftStats)
    fun dobreak(level: ServerLevel, pos: BlockPos, state: BlockState)
}

interface CommonShaft<L: BlockGetter> {
    val level: L
    val dir: Direction.Axis

    val shaftChunks: MutableSet<ChunkPos>
    val shaftBlocks: MutableSet<Pair<ChunkPos,BlockPos>>

    fun addShaftBlock(pos: BlockPos) {
        val cp = ChunkPos(pos)
        shaftBlocks += cp to pos
        shaftChunks += cp
    }

    fun removeShaftBlock(pos: BlockPos) {
        val cp = ChunkPos(pos)
        shaftBlocks -= cp to pos
        val remaining = shaftBlocks.count { it.first == cp }
        if (remaining == 0) {
            shaftChunks -= cp
        }
    }
}

class ClientShaft(
    override val level: ClientLevel,
    override val dir: Direction.Axis
): CommonShaft<ClientLevel> {
    override val shaftChunks = mutableSetOf<ChunkPos>()
    override val shaftBlocks = mutableSetOf<Pair<ChunkPos,BlockPos>>()

    var speed = 0.0f

    var visualRotation = 0.0f
        private set

    internal fun tick() {
        visualRotation -= speed
        visualRotation %= 360.0f
    }
}

class ServerShaft(
    override val level: ServerLevel,
    override val dir: Direction.Axis
): CommonShaft<ServerLevel> {
    // should only be 2
    val consumer = mutableListOf<Pair<BlockPos, ShaftConsumer>>()
    val statsThisTick = ShaftStats().also { it.reset() }
    override val shaftChunks = mutableSetOf<ChunkPos>()
    override val shaftBlocks = mutableSetOf<Pair<ChunkPos,BlockPos>>()

    fun wantSpeeds(): ClosedFloatingPointRange<Float>? {
        val all = consumer.map { it.second.wantSpeeds(level, it.first, level.getBlockState(it.first)) }
        val rang = all.reduceOrNull { acc, x ->
            val start = max(acc.start, x.start)
            val end = min(acc.endInclusive, x.endInclusive)
            object : ClosedFloatingPointRange<Float> {
                override fun lessThanOrEquals(a: Float, b: Float): Boolean = error("")
                override val endInclusive: Float = end
                override val start: Float = start
            }
        }
        return rang?.let {
            if (it.start > it.endInclusive) null
            else it.start..it.endInclusive
        }
    }

    fun mergeFrom(other: ServerShaft) {
        consumer += other.consumer
        shaftChunks += other.shaftChunks
        shaftBlocks += other.shaftBlocks
    }

    fun apply(force: Float, speed: Float) {
        val old = statsThisTick.force
        statsThisTick.force += force
        val diff = statsThisTick.force - old
        val effect = diff / statsThisTick.force

        statsThisTick.speed *= speed * effect
    }

    private var lastSpeed = 0.0f
    internal fun tick() {
        consumer.forEach { (pos, cons) ->
            val state = level.getBlockState(pos)
            if (statsThisTick.speed !in cons.wantSpeeds(level, pos, state))
                cons.dobreak(level, pos, state)
            else cons.consume(level, pos, state, statsThisTick)
        }

        val diffSpeed = abs(lastSpeed - statsThisTick.speed)
        if (diffSpeed > 0.5f) {
            val first = shaftBlocks.first().second
            TournamentNetworking.ShaftSpeedChange(
                first.asLong(),
                TournamentNetworking.ShaftSpeedChange.getAxis(dir),
                statsThisTick.speed
            )
        }
        lastSpeed = statsThisTick.speed

        statsThisTick.reset()
    }

    private fun alreadyAddedToParent() =
        this in ServerShaftMan.get(level).shafts

    override fun addShaftBlock(pos: BlockPos) {
        super.addShaftBlock(pos)
        if (alreadyAddedToParent() && shaftBlocks.isNotEmpty()) {
            val first = shaftBlocks.first().second
            if (pos == first) return
            TournamentNetworking.ShaftBlockChange(
                first.asLong(),
                listOf(pos.asLong()),
                false
            ).send()
        }
    }

    override fun removeShaftBlock(pos: BlockPos) {
        super.removeShaftBlock(pos)
        if (alreadyAddedToParent() && shaftBlocks.isNotEmpty()) {
            val first = shaftBlocks.first().second
            if (pos == first) return
            TournamentNetworking.ShaftBlockChange(
                first.asLong(),
                listOf(pos.asLong()),
                true
            ).send()
        }
    }
}

fun Direction.Axis.directions() =
    when (this) {
        Direction.Axis.X -> listOf(Direction.EAST, Direction.WEST)
        Direction.Axis.Y -> listOf(Direction.UP, Direction.DOWN)
        Direction.Axis.Z -> listOf(Direction.SOUTH, Direction.NORTH)
    }

fun BlockPos.posOnAxis(axis: Direction.Axis) =
    axis.choose(x, y, z)

fun BlockPos.neighborBlocks() =
    listOf(this.above(), this.below(), this.north(), this.south(), this.east(), this.west())

abstract class CommonShaftMan<S: CommonShaft<L>, L: BlockGetter>(val level: L) {
    abstract val shafts: List<S>

    fun shaftsInChunk(chunk: ChunkPos) =
        shafts.filter { chunk in it.shaftChunks }

    fun shaftsAdjacentTo(pos: BlockPos): List<S> {
        val toSearch = pos.neighborBlocks() + pos
        val interestingChunks = toSearch.map(::ChunkPos).toSet()
        val a = interestingChunks
            .flatMap(::shaftsInChunk)
            .filter { s -> toSearch.any { pos -> s.shaftBlocks.any { it.first == pos } } }
        return a
    }

    fun shaftAdjacentTo(pos: BlockPos, axis: Direction.Axis): S? =
        shaftsAdjacentTo(pos).find { it.dir == axis }

    fun shaftAt(pos: BlockPos): S? {
        val cp = ChunkPos(pos)
        return shafts.find {
            cp in it.shaftChunks &&
                    it.shaftBlocks.any { it.first == pos }
        }
    }
}

class ClientShaftMan private constructor(level: ClientLevel): CommonShaftMan<ClientShaft, ClientLevel>(level) {
    override val shafts = CopyOnWriteArrayList<ClientShaft>()

    init {
        TournamentEvents.clientTick.on { _ ->
            shafts.forEach(ClientShaft::tick)
        }
    }

    fun eraseShaft(pos: BlockPos) {
        shaftAt(pos)?.let(shafts::remove)
    }

    fun getOrCreateShaft(pos: BlockPos, dir: Direction.Axis) =
        shaftAt(pos) ?: ClientShaft(level, dir).also {
            it.addShaftBlock(pos)
            shafts.add(it)
        }

    companion object {
        private val all = mutableMapOf<ClientLevel, ClientShaftMan>()

        fun get(level: ClientLevel) =
            all.computeIfAbsent(level) { ClientShaftMan(it) }
    }
}

class ServerShaftMan private constructor(level: ServerLevel): CommonShaftMan<ServerShaft, ServerLevel>(level) {
    override val shafts = mutableListOf<ServerShaft>()

    private fun shaftAdd(shaft: ServerShaft) {
        shafts += shaft
        val shaftPos = shaft.shaftBlocks.first().second.asLong()
        TournamentNetworking.ShaftSpeedChange(
            shaftPos,
            TournamentNetworking.ShaftSpeedChange.getAxis(shaft.dir),
            0.0f
        ).send()
        if (shaft.shaftBlocks.isNotEmpty()) {
            val packed = shaft.shaftBlocks.mapNotNull { (_, pos) ->
                val l = pos.asLong()

                if (l == shaftPos) null
                else l
            }

            TournamentNetworking.ShaftBlockChange(
                shaftPos,
                packed,
                false
            ).send()
        }
    }

    private fun shaftRemove(shaft: ServerShaft) {
        shafts -= shaft
        shaft.shaftBlocks.firstOrNull()?.second?.asLong()?.let { shaftPos ->
            TournamentNetworking.ShaftBlockChange(
                shaftPos,
                listOf(shaftPos),
                true
            ).send()
        }
    }

    init {
        TickScheduler.everyServerTick {
            shafts.forEach(ServerShaft::tick)
        }
    }

    fun addConsumer(pos: BlockPos, block: ShaftConsumer, axis: Direction.Axis) {
        shaftAdjacentTo(pos, axis)?.let { it.consumer += pos to block }
    }

    fun removeConsumer(pos: BlockPos, axis: Direction.Axis) {
        shaftAdjacentTo(pos, axis)?.let {
            it.consumer.removeIf { it.first == pos }
        }
    }

    // remove / split
    fun removeShaft(pos: BlockPos) {
        val shaft = shaftAt(pos) ?: return
        shaft.removeShaftBlock(pos)
        val groups = shaft.shaftBlocks.groupBy { (it.second.posOnAxis(shaft.dir) - pos.posOnAxis(shaft.dir)) > 0 }.values
        val consumers = shaft.consumer.groupBy { (it.first.posOnAxis(shaft.dir) - pos.posOnAxis(shaft.dir)) > 0 }.values
        shaftRemove(shaft)
        groups.zip(consumers).mapNotNull { (it, consumer) ->
            if (it.isNotEmpty()) {
                val res = ServerShaft(level, shaft.dir)
                res.consumer += consumer

                it.forEach { (cp, bp) ->
                    res.shaftChunks += cp
                    res.shaftBlocks += cp to bp
                }

                res
            } else null
        }.forEach(::shaftAdd)
    }

    // extend neighbor shaft or create new shaft
    fun placeOrGetShaft(pos: BlockPos, dir: Direction.Axis): ServerShaft {
        val a = shaftsAdjacentTo(pos)
            .filter { it.dir == dir }
        a.firstOrNull { it.shaftBlocks.any { it.first == pos } }?.let { return it }
        val merge = a + ServerShaft(level, dir).also { it.addShaftBlock(pos) }
        val acc = merge.first()
        merge.forEach(::shaftRemove)
        merge.drop(1).forEach {
            acc.mergeFrom(it)
        }
        shaftAdd(acc)
        return acc
    }

    companion object {
        private val all = mutableMapOf<ServerLevel, ServerShaftMan>()

        fun get(level: ServerLevel) =
            all.computeIfAbsent(level) { ServerShaftMan(it) }
    }
}
