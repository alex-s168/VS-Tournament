package org.valkyrienskies.tournament.chunk

import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import org.joml.Vector2i
import org.joml.primitives.Rectanglei
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.tournament.TickScheduler
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.storage.readStorage
import org.valkyrienskies.tournament.util.extension.fix
import org.valkyrienskies.tournament.util.extension.itTake
import org.valkyrienskies.tournament.util.extension.scaleFrom
import org.valkyrienskies.tournament.util.extension.values
import java.util.function.Consumer

// TODO: make multiple blocks of tickets that cycle trough every tick so that not too many chunks are tried to be loaded at once
class ChunkLoaderManager private constructor(
    val level: ServerLevel
) {
    private val tickets = mutableListOf<ChunkLoadingTicket>()

    private var sorted = true

    private val storage = level.readStorage(ChunkLoaderManagerStorage())

    fun tick(
        amountOfTickets: Int? = null,
        amountOfChunksPerTicket: Int,
        amountOfActualChunksPerTicket: Int,
        loader: Consumer<ChunkPos>
    ) {
        if (!sorted) {
            tickets.sortBy {
                it.priority
            }
            sorted = true
        }

        val am = amountOfTickets ?: tickets.size

        if (tickets.size > am) {
            throw Exception("[Tournament] Too many chunk-loading tickets in dimension ${level.dimension()}: ${tickets.size} > $am!")
        }

        val tl = storage.toLoad
        tl.clear()
        tickets.forEach {
            tl += it.loader.getCurrentChunk()
        }
        storage.toLoad = tl

        tickets.itTake(am).forEach { ticket ->
            val mid = ticket.loader.getCurrentChunk().toJOML()
            val top = ticket.loader.getFutureChunk().toJOML()
            val bottom = top.sub(mid, Vector2i()).negate().add(mid)

            val box = Rectanglei(top, bottom).fix()

            val t = box.area().toFloat() / amountOfChunksPerTicket

            box .scaleFrom(factor = t, center = mid)
                .values()
                .sortedBy { it.distanceSquared(mid) }
                .take(amountOfActualChunksPerTicket)
                .map { ChunkPos(it.x, it.y) }
                .plus(tl)
                .forEach {
                    lastTickChunks += it
                    loader.accept(it)
                }
        }
    }

    private var unloading = false

    fun shouldCancelUnload(chunk: ChunkPos): Boolean =
        !unloading && chunk in lastTickChunks

    fun allocate(loader: ChunkLoader, priority: Int): ChunkLoadingTicket {
        val i = tickets.indexOfFirst { it.loader == loader }
        if (i != -1) {
            val ticket = tickets[i]
            ticket.priority = priority
            return ticket
        }
        return ChunkLoadingTicket(this, loader, priority).also {
            tickets += it
            if (tickets.size > 1) {
                sorted = false
            }
        }
    }

    fun dispose(ticket: ChunkLoadingTicket) {
        tickets -= ticket
    }

    private val lastTickChunks = mutableListOf<ChunkPos>()

    companion object {
        private val map =
            HashMap<ResourceKey<Level>, ChunkLoaderManager>()

        fun getForOrNull(level: ServerLevel): ChunkLoaderManager? =
            map[level.dimension()]

        fun getFor(level: ServerLevel): ChunkLoaderManager {
            val dim = level.dimension()
            if (dim in map)
                return map[dim]!!

            val lm = ChunkLoaderManager(level)
            map[dim] = lm

            if (!tickTaskSet) {
                tickTaskSet = true
                TickScheduler.everyServerTick(tickTask)
            }

            return lm
        }

        private var tickTaskSet = false

        private val tickTask: (MinecraftServer) -> Unit = { server ->
            val cpt = TournamentConfig.SERVER.chunkTicketsPerTick.let {
                if (it == -1) null else it
            }
            server.allLevels.forEach { level ->
                val manager = getFor(level)
                manager.lastTickChunks.forEach { pos ->
                    manager.unloading = true
                    level.setChunkForced(pos.x, pos.z, false)
                    manager.unloading = false
                }
                manager.tick(
                    amountOfTickets = cpt,
                    amountOfChunksPerTicket = TournamentConfig.SERVER.chunksPerTicket * 10,
                    amountOfActualChunksPerTicket = TournamentConfig.SERVER.chunksPerTicket,
                    loader = { pos ->
                        // force chunk
                        level.setChunkForced(pos.x, pos.z, true)
                        // add ticket
                        level.chunkSource.chunkMap.distanceManager.addTicket(
                            TicketType.UNKNOWN,
                            pos,
                            33, // magic number used in minecraft
                            pos
                        )
                    }
                )
            }
        }
    }
}