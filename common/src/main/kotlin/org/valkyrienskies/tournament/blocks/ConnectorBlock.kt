package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import org.valkyrienskies.tournament.blockentity.ConnectorBlockEntity

class ConnectorBlock: BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.METAL)
) {

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level as? ServerLevel == null) return


        val be = level.getBlockEntity(pos) as? ConnectorBlockEntity
            ?: return

        val signal = level.getBestNeighborSignal(pos)
        be.redstoneLevel = signal
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)

        if (level as? ServerLevel == null) return

        val be = level.getBlockEntity(pos) as? ConnectorBlockEntity
            ?: return

        val signal = level.getBestNeighborSignal(pos)
        be.redstoneLevel = signal
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ConnectorBlockEntity(pos, state)

    /*@Suppress("UNCHECKED_CAST")
    override fun <T: BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> =
        ConnectorBlockEntity.ticker as BlockEntityTicker<T>

    @Deprecated("Deprecated in Java")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level is ServerLevel) {
            val be = level.getBlockEntity(pos) as? ConnectorBlockEntity
                ?: return

            be.disconnect()
        }

        super.onRemove(state, level, pos, newState, isMoving)
    }*/
}