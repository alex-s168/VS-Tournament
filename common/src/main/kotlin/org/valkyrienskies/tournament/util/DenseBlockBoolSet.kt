package org.valkyrienskies.tournament.util

import blitz.Endian
import blitz.collections.DenseIx16x16BoolMap
import blitz.toBytes
import blitz.toInt
import blitz.toLong
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet

// TODO: get rid of this and replace with VS?

class DenseBlockBoolSet: Set<BlockPos> {
    val backing = mutableMapOf<ChunkPos, DenseIx16x16BoolMap>()

    private fun get(cp: ChunkPos) =
        backing[cp]

    operator fun get(cp: ChunkPos, absBP: BlockPos): Boolean =
        get(cp)?.let {
            val relX = absBP.x - cp.minBlockX
            val relZ = absBP.z - cp.minBlockZ
            it[relX, absBP.y, relZ]
        } ?: false

    operator fun get(blockPos: BlockPos) =
        get(ChunkPos(blockPos), blockPos)

    private fun getOrCreate(cp: ChunkPos): DenseIx16x16BoolMap =
        backing.computeIfAbsent(cp) { _ -> DenseIx16x16BoolMap() }

    operator fun set(cp: ChunkPos, absBP: BlockPos, value: Boolean) {
        val chunk = getOrCreate(cp)
        val relX = absBP.x - cp.minBlockX
        val relZ = absBP.z - cp.minBlockZ
        chunk[relX, absBP.y, relZ] = value
    }

    operator fun set(blockPos: BlockPos, value: Boolean) {
        set(ChunkPos(blockPos), blockPos, value)
    }

    inline fun forEachSet(fn: (cp: ChunkPos, absX: Int, absY: Int, absZ: Int) -> Unit) =
        backing.forEach { (cp, it) ->
            it.forEachSet { relX, y, relZ ->
                val x = cp.minBlockX + relX
                val z = cp.minBlockZ + relZ
                fn(cp, x, y, z)
            }
        }

    inline fun forEachSet(fn: (BlockPos) -> Unit) {
        forEachSet { _, x, y, z ->
            fn(BlockPos(x, y, z))
        }
    }

    private inline fun serialize(unbufferedConsumer: (ByteArray) -> Unit, fn: (DenseIx16x16BoolMap) -> Unit) {
        unbufferedConsumer(backing.size.toBytes(Endian.LITTLE))
        backing.forEach { (cp, it) ->
            cp.toLong().toBytes(Endian.LITTLE).also(unbufferedConsumer)
            fn(it)
        }
    }

    /**
     * base: 4
     * per contained chunk: 12 + [per contained layer: (3 + (0 to 256))] bytes
     * only recommended if very few positions per layer
     */
    fun serializeByPositions(unbufferedConsumer: (ByteArray) -> Unit) =
        serialize(unbufferedConsumer) {
            it.serializeByPositions(true, unbufferedConsumer)
        }

    /**
     * base: 4
     * per contained chunk: 12 + [per contained layer: 34 bytes]
     * should almost always be used
     */
    fun serializeByLayers(unbufferedConsumer: (ByteArray) -> Unit) =
        serialize(unbufferedConsumer) {
            it.serializeByLayers(true, unbufferedConsumer)
        }

    override val size: Int
        get() {
            var num = 0
            backing.forEach { (_, it) ->
                it.backing.forEachSet { _, map ->
                    (0..<16).forEach {
                        num += map.countSetInRow(it)
                    }
                }
            }
            return num
        }

    override fun isEmpty(): Boolean {
        forEachSet { _, _, _, _ ->
            return false
        }
        return true
    }

    override fun contains(element: BlockPos): Boolean =
        get(element)

    override fun iterator(): Iterator<BlockPos> =
        iterator {
            forEachSet {
                yield(it)
            }
        }

    fun clear() {
        backing.clear()
    }

    override fun containsAll(elements: Collection<BlockPos>): Boolean =
        elements.all { contains(it) }

    companion object {
        private inline fun deserialize(unbufferedProvider: (Int) -> ByteArray, fn: () -> DenseIx16x16BoolMap): DenseBlockBoolSet {
            val count = unbufferedProvider(4).toInt(Endian.LITTLE)
            val res = DenseBlockBoolSet()
            repeat(count) {
                val cp = ChunkPos(unbufferedProvider(8).toLong(Endian.LITTLE))
                res.backing[cp] = fn()
            }
            return res
        }

        fun deserializeByPositions(unbufferedProvider: (Int) -> ByteArray) =
            deserialize(unbufferedProvider) {
                DenseIx16x16BoolMap.deserializeByPositions(true, unbufferedProvider = unbufferedProvider)
            }

        fun deserializeByLayers(unbufferedProvider: (Int) -> ByteArray) =
            deserialize(unbufferedProvider) {
                DenseIx16x16BoolMap.deserializeByLayers(true, unbufferedProvider = unbufferedProvider)
            }
    }

}

fun DenseBlockBoolSet.toVsSlow(): DenseBlockPosSet {
    val res = DenseBlockPosSet()
    forEachSet { _, x, y, z ->
        res.add(x, y, z)
    }
    return res
}