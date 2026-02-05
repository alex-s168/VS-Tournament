package org.valkyrienskies.tournament

import blitz.collections.remove
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld
import org.valkyrienskies.core.impl.networking.simple.SimplePacket
import org.valkyrienskies.core.impl.networking.simple.register
import org.valkyrienskies.core.impl.networking.simple.registerClientHandler
import org.valkyrienskies.core.impl.networking.simple.sendToAllClients
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.tournament.ship.TournamentShips
import org.valkyrienskies.tournament.util.extension.once
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object TournamentNetworking {
    @OptIn(ExperimentalContracts::class)
    private fun <R: Any> runIfServer(fn: () -> R): R? {
        contract {
            callsInPlace(fn, InvocationKind.AT_MOST_ONCE)
        }

        return if (vsCore.dummyShipWorldServer is ShipObjectServerWorld) {
            fn()
        } else null
    }

    data class ShipFuelTypeChange(
        val ship: ShipId,
        val fuel: String?,
    ) : SimplePacket {
        constructor(
            ship: ShipId,
            fuel: ResourceLocation?
        ): this(ship, fuel?.toString())

        fun fuelKey() =
            fuel?.let(::ResourceLocation)

        fun fuelFuel() =
            fuelKey()?.let(TournamentFuelManager.fuels::get)

        fun send() {
            runIfServer {
                // TODO after vs update
                // with(vsCore.simplePacketNetworking) {
                this.sendToAllClients()
                // }
            }
        }

        fun clientHandler() {
            val client = TournamentShips.Client[ship]
            client.fuelType.set(fuelFuel())
        }
    }

    data class ShipThrusterChange(
        val ship: ShipId,
        val pos: Long,
        val throttle: Float,
    ): SimplePacket {
        fun unpackPos() =
            BlockPos.of(pos)

        fun removed() =
            throttle < 0.0f

        fun send() {
            runIfServer {
                // TODO after vs update
                // with(vsCore.simplePacketNetworking) {
                this.sendToAllClients()
                // }
            }
        }

        fun clientHandler() {
            val client = TournamentShips.Client[ship]
            val idx = client.thrusters.index(unpackPos())
            if (removed()) {
                client.thrusters.remove(idx)
            } else {
                client.thrusters[idx] = TournamentShips.Client.Data.Thruster(throttle)
            }
        }
    }

    // add or delete blocks inside shaft or delete shaft (if blocks contains shaftpos)
    data class ShaftBlockChange(
        val shaftPos: Long,
        val newBlocks: List<Long>,
        val remove: Boolean
    ): SimplePacket {
        fun send() {
            runIfServer {
                // TODO after vs update
                // with(vsCore.simplePacketNetworking) {
                this.sendToAllClients()
                // }
            }
        }

        fun clientHandler() {
            val level = Minecraft.getInstance().level!!
            val man = ClientShaftMan.get(level)
            if (shaftPos in newBlocks) {
                if (!remove) error("no.")
                man.eraseShaft(BlockPos.of(shaftPos))
            } else {
                val shaft = man.shaftAt(BlockPos.of(shaftPos))!!
                if (remove) {
                    newBlocks.forEach { shaft.removeShaftBlock(BlockPos.of(it)) }
                } else {
                    newBlocks.forEach { shaft.addShaftBlock(BlockPos.of(it)) }
                }
            }
        }
    }

    // change shaft speed or add shaft
    data class ShaftSpeedChange(
        val shaftPos: Long, // blockPos
        val axis: Byte,
        val speed: Float,
    ): SimplePacket {
        fun unpackPos() =
            BlockPos.of(shaftPos)

        fun axis() =
            Direction.Axis.entries[axis.toInt()]

        companion object {
            fun getAxis(axis: Direction.Axis) =
                axis.ordinal.toByte()
        }

        fun send() {
            runIfServer {
                // TODO after vs update
                // with(vsCore.simplePacketNetworking) {
                this.sendToAllClients()
                // }
            }
        }

        fun clientHandler() {
            val level = Minecraft.getInstance().level!!
            val man = ClientShaftMan.get(level)
            val pos = unpackPos()
            man.getOrCreateShaft(pos, axis()).speed = speed
        }
    }

    val register by once {
        // TODO after vs update
        // with(vsCore.simplePacketNetworking) {
        ShipFuelTypeChange::class.register()
        ShipThrusterChange::class.register()
        ShaftSpeedChange::class.register()
        ShaftBlockChange::class.register()
        // }

        // TODO after vs update
        // with(vsCore.simplePacketNetworking) {
        ShipFuelTypeChange::class.registerClientHandler(ShipFuelTypeChange::clientHandler)
        ShipThrusterChange::class.registerClientHandler(ShipThrusterChange::clientHandler)
        ShaftSpeedChange::class.registerClientHandler(ShaftSpeedChange::clientHandler)
        ShaftBlockChange::class.registerClientHandler(ShaftBlockChange::clientHandler)
        // }
    }
}