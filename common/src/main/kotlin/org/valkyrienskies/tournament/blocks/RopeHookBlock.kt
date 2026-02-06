package org.valkyrienskies.tournament.blocks

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.TournamentDebugHelper
import org.valkyrienskies.tournament.TournamentItems
import org.valkyrienskies.tournament.util.block.DirectionalBaseEntityBlock
import org.valkyrienskies.tournament.util.debug.DebugLine
import org.valkyrienskies.tournament.blockentity.RopeHookBlockEntity
import org.valkyrienskies.tournament.doc.Doc
import org.valkyrienskies.tournament.doc.Documented
import org.valkyrienskies.tournament.doc.documentation
import org.valkyrienskies.tournament.util.DirectionalShape
import org.valkyrienskies.tournament.util.RotShapes
import org.valkyrienskies.tournament.util.helper.drawQuadraticParticleCurve
import org.valkyrienskies.tournament.util.helper.getShipRenderPosition
import java.awt.Color
import java.lang.Exception
import java.util.*
import kotlin.math.absoluteValue

class RopeHookBlock : DirectionalBaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.STONE)
        .sound(SoundType.STONE)
        .strength(1.0f, 2.0f)
) {

    val SHAPE = RotShapes.box(0.25, 0.0, 0.25, 15.75, 16.0, 15.75)
    val ROPEATTACH_SHAPE = DirectionalShape.north(SHAPE)

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.POWER, 0))
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = RopeHookBlockEntity(pos, state)

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, randomSource: RandomSource) {
        super.animateTick(state, level, pos, randomSource)
        val be = level.getBlockEntity(pos) as RopeHookBlockEntity
        if (be.otherPos != null && !be.isSecondary && TournamentDebugHelper.exists(be.debugID)) {
            level as ClientLevel
            if (be.maxLen == 0.0) {
                be.maxLen = (level.getShipRenderPosition(be.otherPos!!)
                    .distance(level.getShipRenderPosition(be.mainPos!!)))
                    .absoluteValue
            }
            val p1 = level.getShipRenderPosition(be.mainPos!!)
            val p2 = level.getShipRenderPosition(be.otherPos!!)

            if (TournamentConfig.CLIENT.particleRopeRenderer)
                drawQuadraticParticleCurve(p1, p2, be.maxLen, be.maxLen * 2, level, ParticleTypes.CLOUD)

            TournamentDebugHelper.list()[be.debugID] = DebugLine(p1, p2, Color.RED, !TournamentConfig.CLIENT.particleRopeRenderer)
        }
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return ROPEATTACH_SHAPE[state.getValue(BlockStateProperties.FACING)]
    }

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

        if(signal > 0)
            dropConstraints(level, pos)
    }

    fun dropConstraints(level: ServerLevel, pos: BlockPos) {
        val be = level.getBlockEntity(pos) as RopeHookBlockEntity
        val gpta = ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId)

        if (be.isSecondary) {
            val t = be.conPos?.let { level.getBlockState(it) }

            try {
                val pbe = (be.conPos?.let { level.getBlockEntity(it) } as RopeHookBlockEntity)
                if (pbe.ropeId != 0) {
                    ItemEntity(
                        level,
                        pos.x.toDouble(),
                        pos.y.toDouble(),
                        pos.z.toDouble(),
                        ItemStack(TournamentItems.ROPE.get())
                    ).also {
                        level.addFreshEntity(it)
                    }
                }
                pbe.ropeId?.let {
                    gpta.removeJoint(it)
                }
                TournamentDebugHelper.removeObject(pbe.debugID)
                pbe.otherPos = null
                pbe.ropeId = 0
                pbe.debugID = -1
                level.sendBlockUpdated(
                    pbe.blockPos,
                    pbe.blockState,
                    pbe.blockState,
                    Block.UPDATE_ALL_IMMEDIATE
                )
            } catch (_: Exception) {}

            be.conPos?.let { level.removeBlock(it, false) }
            be.conPos?.let { level.setBlock(it, t!!, 2) }
        }

        TournamentDebugHelper.removeObject(be.debugID)
        be.ropeId?.let { gpta.removeJoint(it) }
        be.otherPos = null
        be.ropeId = 0
        be.debugID = -1
        level.sendBlockUpdated(be.blockPos, be.blockState, be.blockState, Block.UPDATE_ALL_IMMEDIATE)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level.isClientSide) return
        level as ServerLevel

        dropConstraints(level, pos)

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

        if (level as? ServerLevel == null) return

        val signal = level.getBestNeighborSignal(pos)
        level.setBlock(pos, state.setValue(BlockStateProperties.POWER, signal), 2)

        if(signal > 0)
            dropConstraints(level, pos)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, ctx.nearestLookingDirection.opposite)
    }

    class DocImpl: Documented {
        override fun getDoc() = documentation {
            page("Rope Hook")
                .kind(Doc.Kind.BLOCK)
                .summary("Allows for one rope connection at a time. " +
                        "Disconnects when powered with redstone. " +
                        "Right click two rope hooks with a rope item to connect them. ")
        }
    }
}