package org.valkyrienskies.tournament.blocks.explosive

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.tournament.blockentity.explosive.ExplosiveBlockEntity

abstract class AbstractExplosiveBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.SAND)
) {

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
        if(signal > 0)
            ignite(level, pos)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = ExplosiveBlockEntity(pos, state)

    /**
     * gets called after ignition if explosionTicks() is 0
     */
    open fun explode(level: ServerLevel, pos: BlockPos) {}

    open fun shouldExplode(level: ServerLevel, pos: BlockPos): Boolean {
        return true
    }

    /**
     * how many explosion ticks it has (how often it should call explodeTick() after ignition)
     * If 0 then explode() gets called once
     */
    open fun explosionTicks() : Int { return 0 }

    /**
     * if explosionTicks() > 0 then this is called explosionTicks() amount after ignition
     */
    open fun explodeTick(level: ServerLevel, pos: BlockPos) {}

    /**
     * Starts ignition from outside code
     */
    final fun ignite(level: ServerLevel, pos: BlockPos) {
        if (!shouldExplode(level, pos)) {
            level.removeBlock(pos, false)
            return
        }

        if(explosionTicks() > 0) {
            explodeTick(level, pos)
            try {
                (level.getBlockEntity(pos) as ExplosiveBlockEntity).explosionTicks = explosionTicks()
            } catch (_ : Exception) {}
        } else {
            level.removeBlock(pos, false)
            explode(level, pos)
        }
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    final override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> {

        return BlockEntityTicker { levelB: Level, posB: BlockPos, stateB: BlockState, t: T ->
            run {
                ExplosiveBlockEntity.tick(
                    levelB,
                    posB,
                    stateB,
                    t
                )
            }
        }
    }

    open fun tickExtra(level: ServerLevel, pos: BlockPos) {}

    open fun explodeUsingFlint(level: ServerLevel, pos: BlockPos): Boolean {
        return false
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (level is ServerLevel && player.mainHandItem.item == Items.FLINT_AND_STEEL) {
            if (explodeUsingFlint(level, pos)) {
                ignite(level, pos)
            }
        }

        return InteractionResult.PASS
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level !is ServerLevel) return

        val signal = level.getBestNeighborSignal(pos)

        if (signal > 0) {
            ignite(level, pos)
        }
    }

}
