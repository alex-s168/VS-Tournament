package org.valkyrienskies.tournament.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.TournamentBlockEntities
import org.valkyrienskies.tournament.chunk.ChunkLoader
import org.valkyrienskies.tournament.chunk.ChunkLoaderManager
import org.valkyrienskies.tournament.chunk.ChunkLoadingTicket
import org.valkyrienskies.tournament.util.extension.toChunkPos

class ChunkLoaderBlockEntity(pos: BlockPos, state: BlockState):
    BlockEntity(TournamentBlockEntities.CHUNK_LOADER.get(), pos, state),
    ChunkLoader
{

    internal var ticket: ChunkLoadingTicket? = null

    private fun tick(level: ServerLevel) {
        if (ticket == null) {
            val manager = ChunkLoaderManager.getFor(level)
            ticket = manager.allocate(this, 200)
        }
    }

    private fun getCurrPos() =
        level
            ?.getShipObjectManagingPos(blockPos)
            ?.shipToWorld
            ?.transformPosition(blockPos.toJOMLD())

    override fun getCurrentChunk(): ChunkPos =
        getCurrPos()
            ?.toChunkPos()
            ?: blockPos.toChunkPos()

    override fun getFutureChunk(): ChunkPos =
        level
            ?.getShipObjectManagingPos(blockPos)
            ?.velocity
            ?.mul(3.0, Vector3d())
            ?.add(getCurrPos())
            ?.toChunkPos()
            ?: blockPos.toChunkPos()

    companion object {
        val ticker = BlockEntityTicker<ChunkLoaderBlockEntity> { level, _, _, be ->
            if(level !is ServerLevel)
                return@BlockEntityTicker

            be.tick(level)
        }
    }
}