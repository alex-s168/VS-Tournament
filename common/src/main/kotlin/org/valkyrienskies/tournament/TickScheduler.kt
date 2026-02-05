package org.valkyrienskies.tournament

import net.minecraft.server.MinecraftServer
import org.valkyrienskies.tournament.util.extension.with
import java.util.concurrent.ConcurrentHashMap

/**
 * Allows for scheduling of tasks to be run on the server thread.
 */
object TickScheduler {
    private val serverTickTemp = ConcurrentHashMap.newKeySet<Ticking>()
    private val serverTickPerm = ConcurrentHashMap.newKeySet<Ticking>()

    /**
     * Adds a temporary task to be run ONCE on the server thread.
     */
    fun serverTickOnce(f: (MinecraftServer) -> Unit): Ticking {
        val t = Ticking(f, false)
        serverTickTemp += t
        return t
    }

    /**
     * Adds a permanent task to be run every tick on the server thread.
     */
    fun everyServerTick(f: (MinecraftServer) -> Unit): Ticking {
        val t = Ticking(f, true)
        serverTickPerm += t
        return t
    }

    /**
     * Should be called by a mixin in the server tick loop.
     */
    fun tickServer(server: MinecraftServer) {
        val toRemove = mutableSetOf<Ticking>()
        serverTickTemp.with(serverTickPerm).forEach { t ->
            if (!t.active) {
                toRemove += t
                return@forEach
            }
            t.f(server)
        }
        serverTickTemp.clear()
        serverTickPerm.removeAll(toRemove)
    }

    /**
     * A task to be run on the server thread.
     */
    data class Ticking(
        /**
         * The function to be run.
         */
        val f: (MinecraftServer) -> Unit,

        /**
         * If false, will be removed after the next tick.
         */
        val active: Boolean
    )
}