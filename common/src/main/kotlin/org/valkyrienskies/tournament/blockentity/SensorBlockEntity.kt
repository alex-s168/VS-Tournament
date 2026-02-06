package org.valkyrienskies.tournament.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.tournament.TournamentBlockEntities
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.util.helper.convertShipToWorldSpace
import org.valkyrienskies.tournament.util.math.lerp
import kotlin.math.ceil

class SensorBlockEntity(pos: BlockPos, state: BlockState):
    BlockEntity(TournamentBlockEntities.SENSOR.get(), pos, state)
{

    var lastVal = 0

    fun getResult(level: ServerLevel): Int {
        val facing = blockState
            .getValue(BlockStateProperties.FACING)
            .normal

        val start = level.convertShipToWorldSpace(
            Vec3.atCenterOf(blockPos)
                .add(Vec3.atLowerCornerOf(facing)
                    .scale(0.5))
        )

        val end = level.convertShipToWorldSpace(
            Vec3.atCenterOf(blockPos)
                .add(Vec3.atLowerCornerOf(facing)
                    .scale(TournamentConfig.SERVER.sensorDistance + 0.5))
        )

        val clipResult = level.clipIncludeShips(
            ClipContext(
                start.toMinecraft(),
                end.toMinecraft(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            ),
            true
        )

        val hit = level.convertShipToWorldSpace(
            clipResult
            .location
            .toJOML()
        )

        return if (clipResult.type != HitResult.Type.MISS) {
            ceil(lerp(15.0, 1.0, start.distance(hit) / TournamentConfig.SERVER.sensorDistance)).toInt()
        } else {
            0
        }
    }

    companion object {
        fun tick(level: Level, pos: BlockPos, state: BlockState, be: BlockEntity) {
            be as SensorBlockEntity
            if(level.isClientSide)
                return

            be.lastVal = be.getResult(level as ServerLevel)
            if (be.lastVal == state.getValue(BlockStateProperties.POWER))
                return

            level.setBlock(pos, state.setValue(BlockStateProperties.POWER, be.lastVal), 2)
        }
    }
}