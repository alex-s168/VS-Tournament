package org.valkyrienskies.tournament.util

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.block.state.BlockState

// TODO: USE KELVIN
fun BlockState.getHeat(): Int =
    when (block) {
        Blocks.ICE -> -2

        Blocks.SNOW,
        Blocks.SNOW_BLOCK,
        Blocks.POWDER_SNOW,
        Blocks.POWDER_SNOW_CAULDRON -> -1
        Blocks.LAVA,
        Blocks.LAVA_CAULDRON -> 2

        is FireBlock -> 1

        is CampfireBlock -> if (hasProperty(CampfireBlock.LIT) && getValue(CampfireBlock.LIT)) 1 else 0

        else -> 0
    }