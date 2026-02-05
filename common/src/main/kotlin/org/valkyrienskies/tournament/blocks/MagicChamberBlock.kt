package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.tournament.TournamentItems

class MagicChamberBlock: Block(
    Properties.of().mapColor(MapColor.METAL)
    .sound(SoundType.STONE).strength(5.0f, 5.0f)
) {

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (hand == InteractionHand.OFF_HAND) {
            if (player.mainHandItem.item == Items.LAPIS_LAZULI) {
                player.mainHandItem.count --

                when (level.random.nextFloat()) {
                    in 0f..0.5f -> {}

                    in 0.5f..0.7f -> {
                        val c = Vec3.atCenterOf(pos)
                        level.explode(null, c.x, c.y, c.z, 1f, Level.ExplosionInteraction.TNT)
                    }

                    else -> {
                        popResource(level, pos,
                            ItemStack(TournamentItems.UNSTABLE_LAPIS.get(), 1))
                    }
                }

                return InteractionResult.CONSUME_PARTIAL
            }
        }

        return InteractionResult.PASS
    }

}