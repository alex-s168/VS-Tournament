package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.doc.Doc
import org.valkyrienskies.tournament.doc.Documented
import org.valkyrienskies.tournament.doc.documentation
import org.valkyrienskies.tournament.ship.TournamentShips
import org.valkyrienskies.tournament.util.DirectionalShape
import org.valkyrienskies.tournament.util.RotShapes

class SpinnerBlock : DirectionalBlock(
    Properties.of()
        .mapColor(MapColor.STONE)
        .sound(SoundType.STONE)
        .strength(1.0f, 2.0f)
) {

    val SHAPE = RotShapes.cube()

    val SPINNER_SHAPE = DirectionalShape.north(SHAPE)

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.POWER, 0))
    }

    override fun getRenderShape(blockState: BlockState): RenderShape = RenderShape.MODEL

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SPINNER_SHAPE[state.getValue(BlockStateProperties.FACING)]

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        builder.add(BlockStateProperties.POWER)
        super.createBlockStateDefinition(builder)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level.isClientSide) return
        level as ServerLevel

        val signal = level.getBestNeighborSignal(pos)
        level.setBlock(pos, state.setValue(BlockStateProperties.POWER, signal), 2)

        TournamentShips.getOrCreate(level, pos)?.addSpinner(
            pos.toJOML(),
            state.getValue(FACING)
                .opposite
                .normal
                .toJOMLD()
                .mul(state.getValue(BlockStateProperties.POWER).toDouble())
        )
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        super.onRemove(state, level, pos, newState, isMoving)

        if (level.isClientSide) return
        level as ServerLevel

        TournamentShips.getOrCreate(level, pos)?.removeSpinner(
            pos.toJOML()
        )
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
        level.setBlock(pos, state.setValue(BlockStateProperties.POWER, signal), 2)
    }

    override fun getStateForPlacement(
        ctx: BlockPlaceContext
    ): BlockState = defaultBlockState()
            .setValue(FACING, ctx.nearestLookingDirection.opposite)

    override fun getBlockSupportShape(
        state: BlockState,
        reader: BlockGetter,
        pos: BlockPos
    ): VoxelShape = RotShapes.cube().makeMcShape()

    class DocImpl: Documented {
        override fun getDoc() = documentation {
            page("Spinner")
                .kind(Doc.Kind.BLOCK)
                .summary("Applies rotational force around its Y-axis.")
        }
    }
}