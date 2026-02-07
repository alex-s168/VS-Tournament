package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
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
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.TournamentItems
import org.valkyrienskies.tournament.TournamentProperties
import org.valkyrienskies.tournament.blockentity.ThrusterBlockEntity
import org.valkyrienskies.tournament.doc.Doc
import org.valkyrienskies.tournament.doc.Documented
import org.valkyrienskies.tournament.doc.documentation
import org.valkyrienskies.tournament.ship.TournamentShips
import org.valkyrienskies.tournament.util.DirectionalShape
import org.valkyrienskies.tournament.util.RotShapes
import org.valkyrienskies.tournament.util.block.DirectionalBaseEntityBlock

// TODO: DOCUMENT THAT HIGHER TIER -> HIGHER BURN RATE, BUT MORE THRUST
// TODO: RENAME TO SOLID FUEL THRUSTER
// TODO: spinner and rotor fuel

class ThrusterBlock(
    private val mult: () -> Double,
    private val maxTier: () -> Int
) : DirectionalBaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.STONE)
        .sound(SoundType.STONE)
        .strength(1.0f, 2.0f)
) {

    private val SHAPE = DirectionalShape.south(RotShapes.box(3.0, 5.0, 4.0, 13.0, 11.0, 16.0))

    init {
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(TournamentProperties.TIER, 1)
        )
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ThrusterBlockEntity(pos, state)

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPE[state.getValue(BlockStateProperties.FACING)]
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (level !is ServerLevel) return InteractionResult.PASS

        if (player.mainHandItem.item.asItem() == TournamentItems.UPGRADE_THRUSTER.get() && hand == InteractionHand.OFF_HAND) {
            val tier = state.getValue(TournamentProperties.TIER)
            if (tier < maxTier()) {
                level.setBlockAndUpdate(pos, state.setValue(TournamentProperties.TIER, tier + 1))

                if (!player.isCreative) {
                    player.mainHandItem.shrink(1)
                }
                return InteractionResult.CONSUME
            }
        }

        return InteractionResult.PASS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        builder.add(TournamentProperties.TIER)
        super.createBlockStateDefinition(builder)
    }

    fun getThrottle(state: BlockState, signal: Int) =
        state.getValue(TournamentProperties.TIER) *
                (signal.toFloat() / 15) *
                mult().toFloat()

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level !is ServerLevel) return

        val signal = level.getBestNeighborSignal(pos)

        val ships = TournamentShips.get(level, pos)

        ships?.addThrusterV2(
            pos,
            TournamentShips.ThrusterDataV2(
                state.getValue(FACING).normal.toJOMLD(),
                getThrottle(state, signal),
                false,
            )
        )

        ships?.updateThrusterV2(pos)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level !is ServerLevel) return

        val ships = TournamentShips.get(level, pos)

        ships?.removeThrusterV2(pos)
        ships?.updateThrusterV2(pos)

        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getDrops(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> {
        val drops = super.getDrops(state, builder)

        val tier = state.getValue(TournamentProperties.TIER)
        if (tier > 1) {
            drops += ItemStack(TournamentItems.UPGRADE_THRUSTER.get(), tier - 1)
        }

        return drops
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

        val signal = level.getBestNeighborSignal(pos)

        val ships = TournamentShips.get(level, pos)

        ships?.thrusterV2(pos)?.let {
            val new = getThrottle(state, signal)

            if (it.throttle != new) {
                it.throttle = new
                ships.updateThrusterV2(pos)
            }
        }
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        var dir = ctx.nearestLookingDirection

        if(ctx.player != null && ctx.player!!.isShiftKeyDown)
            dir = dir.opposite

        return defaultBlockState()
            .setValue(BlockStateProperties.FACING, dir)
    }
/*
    @OptIn(GameTickOnly::class)
    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        super.animateTick(state, level, pos, random)
        if (level !is ServerLevel) return

        val ship = level.getLoadedShipManagingPos(pos) ?: return
        val controller = TournamentShips.getOrCreate(ship)

        val rp = ship.transform.shipToWorld.transformPosition(pos.toJOMLD())
        val fuel = controller.fuelType?.fuel
        val thruster = controller.thrusterV2(pos)
        val throttle = thruster?.throttle ?: 0.0f

        if (fuel?.particles != null && throttle > 0.0f) {
            val dir = state.getValue(FACING)

            val vel = fuel.particleVelocity.toDouble()

            val x = rp.x + (0.5 * (dir.stepX + 1))
            val y = rp.y + (0.5 * (dir.stepY + 1))
            val z = rp.z + (0.5 * (dir.stepZ + 1))
            val speedX = dir.stepX * -vel
            val speedY = dir.stepY * -vel
            val speedZ = dir.stepZ * -vel

            fun rand() = (random.nextFloat() * 2 - 1) * fuel.particleSpread

            val particleData = fuel.particles(level.registryAccess())
            repeat(fuel.particleCount) {
                level.addParticle(
                    particleData,
                    x + rand(), y + rand(), z + rand(),
                    speedX, speedY, speedZ
                )
            }
        }
    }
*/
    class DocImpl: Documented {
        override fun getDoc() = documentation {
            page("Thruster")
                .kind(Doc.Kind.BLOCK)
                .summary("Redstone-powered, upgradable thruster.")
                .summary("There are two variants. A small thruster and a \"normal\" thruster.")
                .summary("The thruster is only active when redstone powered. " +
                         "It can be upgraded by right-clicking on it with a thruster upgrade.")
                .summary("The thruster's performance depends on the fuel type being used.")
                .section("Config") {
                    content("The maximum thruster tier is ${TournamentConfig.SERVER.thrusterTiersNormal} for the normal thruster, " +
                            "and ${TournamentConfig.SERVER.thrusterTiersTiny} for the small thruster.")
                    content("The normal thruster has a force of ${TournamentConfig.SERVER.thrusterSpeed} N, " +
                            "which gets multiplied by the tier / level of the thruster.")
                    content("The small thruster is ${TournamentConfig.SERVER.thrusterTinyForceMultiplier}x as powerful as the normal thruster " +
                            "=> ${TournamentConfig.SERVER.thrusterTinyForceMultiplier * TournamentConfig.SERVER.thrusterSpeed} N")
                }
        }
    }
}
