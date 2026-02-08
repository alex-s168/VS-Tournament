package org.valkyrienskies.tournament.util

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.tournament.neighborBlocks

// TODO: rewrite

fun Level.blockGroup(pos: BlockPos, dest: DenseBlockBoolSet = DenseBlockBoolSet(), shouldCancel: (depth: Int) -> Boolean, match: (BlockState) -> Boolean): DenseBlockBoolSet {
    var depth = 0

    fun rec(pos: BlockPos) {
        if (dest[pos]) return
        if (!match(getBlockState(pos))) return

        dest[pos] = true

        depth ++
        if (shouldCancel(depth)) return
        pos.neighborBlocks().forEach(::rec)
    }

    rec(pos)

    return dest
}