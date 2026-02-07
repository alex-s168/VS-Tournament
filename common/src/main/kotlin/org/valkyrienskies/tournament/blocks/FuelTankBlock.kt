package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.WorldlyContainerHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.tournament.TournamentBlockEntities
import org.valkyrienskies.tournament.blockentity.FuelTankBlockEntity
import org.valkyrienskies.tournament.neighborBlocks
import org.valkyrienskies.tournament.util.LazyWithLateParam
import org.valkyrienskies.tournament.util.TitleType
import org.valkyrienskies.tournament.util.block.SlabBaseEntityBlock
import org.valkyrienskies.tournament.util.block.WithExRenderInfo
import org.valkyrienskies.tournament.util.blockGroup
import org.valkyrienskies.tournament.util.extension.toComponent
import org.valkyrienskies.tournament.util.sendTitle
import java.util.BitSet

// TODO: accessors for data from lua

private fun useCommon(
    state: BlockState,
    level: Level,
    pos: BlockPos,
    player: Player,
    hand: InteractionHand,
    hit: BlockHitResult
): InteractionResult {
    if (level !is ServerLevel)
        return InteractionResult.FAIL

    val be = level.getBlockEntity(pos)
            as? FuelTankBlockEntity
        ?: return InteractionResult.FAIL

    val stack = player.getItemInHand(hand)

    var result = InteractionResult.FAIL

    val canStore = be.canStoreCount(stack)
    if (canStore > 0) {
        be.forceStore(stack, canStore)
        stack.shrink(canStore)
        result = InteractionResult.SUCCESS
    }

    be.ship { controller ->
        level.sendTitle(
            player,
            TitleType.ACTION_BAR_TEXT,
            TranslatableContents(
                "misc.vs_tournament.fuel.level",
                "Total (ship) fuel level: %s / %s",
                arrayOf(
                    controller.fuelCount,
                    controller.fuelCap,
                )
            ).toComponent()
        )
    }

    return result
}

private fun onCatchFire(level: Level, pos: BlockPos) {
    if (level !is ServerLevel) return

    val connectedTanks = level.blockGroup(pos, shouldCancel = { it > 30 }) {
        it.block is FuelTankBlockFull || it.block is FuelTankBlockHalf
    }

    val fillLevel = LazyWithLateParam { bp: BlockPos ->
        val be = level.getBlockEntity(bp) as FuelTankBlockEntity
        be.wholeShipFillLevelSynced
    }

    connectedTanks.forEachSet { _, x, y, z ->
        val bp = BlockPos(x, y, z)
        val fill = fillLevel.get { bp }
        if (fill > 0.1) {
            val radius = fill * 4 // at max fill radius
            level.explode(null, x + 0.5, y + 0.5, z + 0.5, radius, true, Level.ExplosionInteraction.TNT)
        }
        level.setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState())
    }
}

private fun checkFire(level: Level, pos: BlockPos, fromPos: BlockPos) {
    if (level.getBlockState(fromPos).block is FireBlock) {
        onCatchFire(level, pos)
    }
}

class FuelTankBlockFull(
    val transparent: Boolean
): BaseEntityBlock(
    Properties
        .of()
        .mapColor(MapColor.METAL)
        .noOcclusion()
        .isViewBlocking { _,_,_ -> !transparent }
), WorldlyContainerHolder, WithExRenderInfo {

    override fun onProjectileHit(level: Level, state: BlockState, hit: BlockHitResult, projectile: Projectile) {
        if (level is ServerLevel && projectile.isOnFire) {
            onCatchFire(level, hit.blockPos)
        }
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? FuelTankBlockEntity? ?: return

        be.onAdded()

        updateNeighborTransparent(level, pos)
        pos.neighborBlocks().forEach {
            checkFire(level, pos, it)
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? FuelTankBlockEntity? ?: return

        be.onRemoved()

        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult)
        = useCommon(state, level, pos, player, hand, hit)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        FuelTankBlockEntity(pos, state, 1.0f) {
            if (transparent) { TournamentBlockEntities.FUEL_TANK_FULL_TRANSPARENT.get() }
            else { TournamentBlockEntities.FUEL_TANK_FULL_SOLID.get() }
        }

    override fun getContainer(state: BlockState, level: LevelAccessor, pos: BlockPos): WorldlyContainer =
        (level.getBlockEntity(pos) as FuelTankBlockEntity).getContainer()

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> = FuelTankBlockEntity.ticker as BlockEntityTicker<T>

    override fun getRenderShape(blockState: BlockState) =
        if (transparent) RenderShape.INVISIBLE else RenderShape.MODEL

    override fun skipRendering(state: BlockState, adjacentBlockState: BlockState, direction: Direction): Boolean {
        if (transparent) return false
        return super.skipRendering(state, adjacentBlockState, direction)
    }

    override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos): Float {
        if (transparent) return 1.0f
        return super.getShadeBrightness(state, level, pos)
    }

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean {
        if (transparent) return true
        return super.propagatesSkylightDown(state, level, pos)
    }

    fun updateNeighborTransparent(level: Level, pos: BlockPos) {
        val be = level.getBlockEntity(pos) as? FuelTankBlockEntity ?: return

        if (transparent) {
            val bitSet = BitSet(6)

            Direction.entries.forEachIndexed { index, direction ->
                val p = pos.relative(direction)
                val bs = level.getBlockState(p).block
                if (bs is FuelTankBlockFull && bs.transparent) {
                    bitSet[index] = true
                }
            }

            be.neighborsTransparent = bitSet
            be.update()
        }
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

        updateNeighborTransparent(level, pos)
        checkFire(level, pos, fromPos)
    }

    override fun getFaceRenderType(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        face: Direction
    ): WithExRenderInfo.FaceRenderType {
        if (!transparent) {
            return WithExRenderInfo.FaceRenderType.NORMAL
        }

        val be = level.getBlockEntity(pos) as? FuelTankBlockEntity
            ?: return WithExRenderInfo.FaceRenderType.NORMAL

        if (be.isNeighborTransparent(face.opposite)) {
            return WithExRenderInfo.FaceRenderType.FORCE_NOT_RENDER
        }

        return WithExRenderInfo.FaceRenderType.FORCE_RENDER
    }
}

class FuelTankBlockHalf: SlabBaseEntityBlock(
    Properties.of().mapColor(MapColor.METAL)
), WorldlyContainerHolder {

    override fun onProjectileHit(level: Level, state: BlockState, hit: BlockHitResult, projectile: Projectile) {
        if (level is ServerLevel && projectile.isOnFire) {
            onCatchFire(level, hit.blockPos)
        }
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? FuelTankBlockEntity? ?: return

        be.onAdded()
        pos.neighborBlocks().forEach {
            checkFire(level, pos, it)
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? FuelTankBlockEntity? ?: return

        be.onRemoved()

        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult)
            = useCommon(state, level, pos, player, hand, hit)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        val isDouble = state.getValue(TYPE) == SlabType.DOUBLE
        val capF = if (isDouble) 1.0f else 0.5f
        return FuelTankBlockEntity(pos, state, capF) {
            TournamentBlockEntities.FUEL_TANK_HALF_SOLID.get()
        }
    }

    override fun getContainer(state: BlockState, level: LevelAccessor, pos: BlockPos): WorldlyContainer =
        (level.getBlockEntity(pos) as FuelTankBlockEntity).getContainer()

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> = FuelTankBlockEntity.ticker as BlockEntityTicker<T>

    override fun getRenderShape(blockState: BlockState) =
        RenderShape.MODEL

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        checkFire(level, pos, fromPos)
    }

}