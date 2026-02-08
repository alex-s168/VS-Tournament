package org.valkyrienskies.tournament.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.tournament.TournamentTags

object TournamentShipAssembler {
    fun findStructure(level: ServerLevel, pos: BlockPos): DenseBlockBoolSet {
        return level.blockGroup(pos, shouldCancel = { it > 2000 }) {
            !it.isAir && !it.`is`(TournamentTags.ASSEMBLER_BLACKLIST) && !it.inAssemblyBlacklist() && !it.liquid()
        }
    }
}