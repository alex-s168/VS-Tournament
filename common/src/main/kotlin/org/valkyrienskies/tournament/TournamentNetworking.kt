package org.valkyrienskies.tournament

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.valkyrienskies.core.impl.networking.simple.SimplePacket
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.tournament.util.extension.once

object TournamentNetworking {
    data class FuelsReloaded(
        val fuels: List<ClientFuelType>,
    ): SimplePacket {
        fun send() {
            vsCore.simplePacketNetworking.sendToAllClients(this)
        }

        fun clientHandler() {
            TournamentClientFuels.types = fuels.toTypedArray()
        }
    }

    // add or delete blocks inside shaft or delete shaft (if blocks contains shaftpos)
    data class ShaftBlockChange(
        val shaftPos: Long,
        val newBlocks: List<Long>,
        val remove: Boolean
    ): SimplePacket {
        fun send() {
            vsCore.simplePacketNetworking.sendToAllClients(this)
        }

        fun clientHandler() {
            val level = Minecraft.getInstance().level!!
            val man = ClientShaftMan.get(level)
            if (shaftPos in newBlocks) {
                assert(remove)
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
            vsCore.simplePacketNetworking.sendToAllClients(this)
        }

        fun clientHandler() {
            val level = Minecraft.getInstance().level!!
            val man = ClientShaftMan.get(level)
            val pos = unpackPos()
            man.getOrCreateShaft(pos, axis()).speed = speed
        }
    }

    val register by once {
        with(vsCore.simplePacketNetworking) {
            FuelsReloaded::class.register()
            ShaftSpeedChange::class.register()
            ShaftBlockChange::class.register()
        }

        with(vsCore.simplePacketNetworking) {
            FuelsReloaded::class.registerClientHandler(FuelsReloaded::clientHandler)
            ShaftSpeedChange::class.registerClientHandler(ShaftSpeedChange::clientHandler)
            ShaftBlockChange::class.registerClientHandler(ShaftBlockChange::clientHandler)
        }
    }
}