package org.valkyrienskies.tournament.util

import blitz.collections.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos

fun JsonGenerator.writeBlockPos(blockPos: BlockPos) {
    writeNumber(blockPos.asLong())
}

fun JsonNode.readBlockPos(): BlockPos =
    if (isArray) BlockPos(
        get(0).asInt(),
        get(1).asInt(),
        get(2).asInt(),
    ) else
        BlockPos.of(asLong())

private class BlockMapSerializer: StdSerializer<BlockMap<*>>(BlockMap::class.java) {
    override fun serialize(value: BlockMap<*>, gen: JsonGenerator, provider: SerializerProvider?) {

        gen.writeStartArray()
        value.contents().forEach { (a, b) ->
            gen.writeStartObject()

            gen.writeFieldName("pos")
            gen.writeBlockPos(a)

            gen.writeFieldName("value")
            provider?.defaultSerializeValue(b, gen)

            gen.writeFieldName("type")
            gen.writeString(b?.let { it::class.java.name } ?: "")

            gen.writeEndObject()
        }
        gen.writeEndArray()
    }
}

private class BlockMapDeserializer: StdDeserializer<BlockMap<*>>(BlockMap::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): BlockMap<*> {
        val res = BlockMap<Any?>()

        val arr = p.codec.readTree<ArrayNode>(p)
        arr.forEach { node ->
            val pos = node.get("pos").readBlockPos()

            val ty = node.get("type").asText()
            if (ty != "") {
                val clazz = Class.forName(ty)

                val v = node.get("value")
                val value = ctxt?.readTreeAsValue(v, clazz)

                res[res.index(pos)] = value
            }
        }

        return res
    }
}

@JsonSerialize(using = BlockMapSerializer::class)
@JsonDeserialize(using = BlockMapDeserializer::class)
class BlockMap<T>: BlitzMap<BlockPos, T, BlockMap.Index<T>> {
    private val underlying = I2HashMap<BlitzHashMap<I3HashMapKey, T>>(::mutableListOf)

    sealed class Index<T>

    private class IndexImpl<T>(
        val cp: BlitzHashMap.Index<I2HashMapKey, BlitzHashMap<I3HashMapKey, T>>,
        val bp: I3HashMapKey,
    ): Index<T>()

    override fun contents(): Contents<Pair<BlockPos, T>> =
        underlying.contents().flatMap { (cp, bm) ->
            val real = ChunkPos(cp.a, cp.b)
            val baseX = real.minBlockX
            val baseZ = real.minBlockZ

            bm.contents().map { (bp, v) ->
                BlockPos(baseX + bp.a, bp.b, baseZ + bp.c) to v
            }
        }.contents

    override fun set(index: Index<T>, value: T?) {
        val idx = (index as IndexImpl)

        val blockMap = underlying[idx.cp]
            ?: I3HashMap<T>(::mutableListOf).also {
                underlying[idx.cp] = it
            }

        blockMap[blockMap.index(idx.bp)] = value

        if (value == null && blockMap.contents().count() == 0) {
            underlying.remove(idx.cp)
        }
    }

    override fun index(key: BlockPos): Index<T> {
        val cp = ChunkPos(key)

        return IndexImpl(
            underlying.index(I2HashMapKey(cp.x, cp.z)),
            I3HashMapKey(
                key.x - cp.minBlockX,
                key.y,
                key.z - cp.minBlockZ,
            )
        )
    }

    override fun get(index: Index<T>): T? {
        val idx = (index as IndexImpl)

        return underlying[idx.cp]?.let { bm ->
            bm[bm.index(idx.bp)]
        }
    }
}

open class SyncMap<K, V, I, M: BlitzMap<K,V,I>>(
    val unsafe: M
): BlitzMap<K,V,I> {
    @JsonIgnore
    private val lock = Any()

    override fun contents(): Contents<Pair<K, V>> =
        synchronized(lock) { unsafe.contents() }

    override fun set(index: I, value: V?) {
        synchronized(lock) {
            unsafe[index] = value
        }
    }

    override fun index(key: K): I =
        unsafe.index(key)

    override fun get(index: I): V? =
        synchronized(lock) {
            unsafe[index]
        }
}

typealias SyncBlockMap<T> = SyncMap<BlockPos, T, BlockMap.Index<T>, BlockMap<T>>