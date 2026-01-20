package org.valkyrienskies.tournament

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Math
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.BlockStateInfoProvider

object TournamentWeights  {
    fun register() {
        Registry.register(BlockStateInfo.REGISTRY, ResourceLocation(TournamentMod.MOD_ID, "ballast"), Ballast)
    }

    object Ballast: BlockStateInfoProvider {
        override val priority: Int
            get() = 200

        override fun getBlockStateMass(blockState: BlockState): Double? =
            if (blockState.block == TournamentBlocks.BALLAST.get())
                Math.lerp(
                    TournamentConfig.SERVER.ballastNoWeight,
                    TournamentConfig.SERVER.ballastWeight,
                    (kotlin.runCatching { blockState.getValue(BlockStateProperties.POWER) }.getOrNull() ?: 0) / 15.0
                )
            else
                null

        override fun getBlockStateType(blockState: BlockState): VsiBlockType? {
            return null
        }

    }
}
