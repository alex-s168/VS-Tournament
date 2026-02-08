package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.blockentity.PropellerBlockEntity
import org.valkyrienskies.tournament.doc.Doc
import org.valkyrienskies.tournament.doc.Documented
import org.valkyrienskies.tournament.doc.documentation
import org.valkyrienskies.tournament.ship.TournamentShips
import org.valkyrienskies.tournament.util.DirectionalShape
import org.valkyrienskies.tournament.util.RotShapes
import org.valkyrienskies.tournament.util.block.DirectionalBaseEntityBlock

class PropellerBlock(
    val mult: Double,
    val beConstr: (BlockPos, BlockState) -> PropellerBlockEntity<*>,
): DirectionalBaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.STONE)
        .sound(SoundType.STONE)
        .strength(1.0f, 2.0f)
), RedstoneConnectingBlock {
    companion object {
        private val SHAPE = RotShapes.box(0.1, 0.1, 8.1, 15.9, 15.9, 15.9)

        private val DIRECTIONAL_SHAPE = DirectionalShape.south(SHAPE)

        fun getPropSignal(state: BlockState, level: Level, pos: BlockPos): Int {
            val sides = Direction.entries - state.getValue(FACING).opposite
            val best = sides.maxOf {
                level.getSignal(pos.relative(it), it)
            }
            return best
        }
    }

    init {
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
        )
    }

    override fun getRenderShape(blockState: BlockState) =
        RenderShape.MODEL

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        DIRECTIONAL_SHAPE[state.getValue(FACING)]

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) =
        super.createBlockStateDefinition(
            builder
                .add(FACING)
        )

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level !is ServerLevel) return

        val signal = getPropSignal(state, level, pos)

        val be = level.getBlockEntity(pos) as? PropellerBlockEntity<*>
            ?: return

        be.signal = signal

        be.update()

        TournamentShips.getOrCreate(level, pos)?.addPropeller(
            pos.toJOML(),
            state.getValue(FACING)
                .normal
                .toJOMLD()
                .mul(mult)
        )
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level !is ServerLevel) return

        TournamentShips.getOrCreate(level, pos)?.removePropeller(pos.toJOML())

        super.onRemove(state, level, pos, newState, isMoving)
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

        if (level !is ServerLevel) return

        val signal = getPropSignal(state, level, pos)

        val be = level.getBlockEntity(pos) as? PropellerBlockEntity<*>
            ?: return

        be.signal = signal

        be.update()
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        var dir = ctx.nearestLookingDirection

        if(ctx.player != null && ctx.player!!.isShiftKeyDown)
            dir = dir.opposite

        return defaultBlockState()
            .setValue(FACING, dir)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        beConstr(pos, state)

    @Suppress("UNCHECKED_CAST")
    override fun <T: BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> =
        PropellerBlockEntity.ticker as BlockEntityTicker<T>

    override fun canConnectTo(state: BlockState, direction: Direction): Boolean =
        direction in Direction.entries - state.getValue(FACING)

    class DocImpl: Documented {
        override fun getDoc() = documentation {
            page("Propeller")
                .kind(Doc.Kind.BLOCK)
                .summary("Redstone-powered ship propeller.")
                .summary("There is a small propeller (designed to look like a torpedo propeller) and a large (3x3) propeller.")
                .summary("Popellers take a bit of time to spin up (reach their maximum force), which makes them unsuitable for steering and similar.")
        }
    }
}