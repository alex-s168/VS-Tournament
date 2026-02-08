package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.ship.TournamentShips

class PoweredBalloonBlock: BalloonBlock() {

    init {
        registerDefaultState(defaultBlockState()
            .setValue(BlockStateProperties.POWER, 0)
            .setValue(BlockStateProperties.POWERED, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(BlockStateProperties.POWER)
        builder.add(BlockStateProperties.POWERED)
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

        val signal = level.getBestNeighborSignal(pos)

        if (signal != state.getValue(BlockStateProperties.POWER)) {
            level.setBlock(
                pos,
                state
                    .setValue(BlockStateProperties.POWER, signal)
                    .setValue(BlockStateProperties.POWERED, signal > 0),
                2
            )
        }
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (level.isClientSide) return
        level as ServerLevel

        val signal = level.getBestNeighborSignal(pos)

        TournamentShips.getOrCreate(level, pos)?.addBalloon(
            pos,
            signal.toDouble() * TournamentConfig.SERVER.balloonAnalogStrength
        )

        if (signal == state.getValue(BlockStateProperties.POWER))
            return

        level.setBlock(
            pos,
            state
                .setValue(BlockStateProperties.POWER, signal)
                .setValue(BlockStateProperties.POWERED, signal > 0),
            2
        )
    }
}