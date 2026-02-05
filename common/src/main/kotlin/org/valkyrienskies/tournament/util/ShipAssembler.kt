package org.valkyrienskies.tournament.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.tournament.TournamentTags

object ShipAssembler {

    fun findStructure(level: ServerLevel, pos: BlockPos) : DenseBlockPosSet {
        val set = level.blockGroup(pos, shouldCancel = { it > 2000 }) {
            !it.isAir && !it.`is`(TournamentTags.ASSEMBLER_BLACKLIST)
        }

        return set.toVsSlow()
    }

}
