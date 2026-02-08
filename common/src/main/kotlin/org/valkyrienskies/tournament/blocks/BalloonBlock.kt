package org.valkyrienskies.tournament.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSources
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.TournamentLootTables
import org.valkyrienskies.tournament.TournamentTriggers
import org.valkyrienskies.tournament.doc.Doc
import org.valkyrienskies.tournament.doc.Documented
import org.valkyrienskies.tournament.doc.documentation
import org.valkyrienskies.tournament.ship.TournamentShips

open class BalloonBlock : Block(
    Properties.of()
        .mapColor(MapColor.WOOL)
        .sound(SoundType.WOOL)
        .strength(1.0f, 2.0f)
) {
    override fun fallOn(level: Level, state: BlockState, blockPos: BlockPos, entity: Entity, f: Float) {
        entity.causeFallDamage(f, 0.2f, DamageSources(level.registryAccess()).fall())
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (level.isClientSide) return
        level as ServerLevel

        TournamentShips.getOrCreate(level, pos)?.addBalloon(
            pos,
            TournamentConfig.SERVER.unpoweredBalloonMul * TournamentConfig.SERVER.balloonAnalogStrength
        )
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        super.onRemove(state, level, pos, newState, isMoving)

        if (level.isClientSide) return
        level as ServerLevel

        TournamentShips.getOrCreate(level, pos)?.removeBalloon(pos)
    }

    override fun onProjectileHit(level: Level, state: BlockState, hit: BlockHitResult, projectile: Projectile) {
        if (level as? ServerLevel == null) return

        val shooter = projectile.owner as? ServerPlayer

        fun shotBalloon(pos: BlockPos) {
            val table = TournamentLootTables.BALLOON_POP
            val ctx = LootParams.Builder(level)
                .withLuck(shooter?.luck ?: 0f)
                .create(LootContextParamSets.EMPTY)
            val loot = level.server.lootData.getLootTable(table).getRandomItems(ctx)

            loot.forEach { itemStack ->
                ItemEntity(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, itemStack).also {
                    level.addFreshEntity(it)
                }
            }
        }

        shooter?.let { player ->
            TournamentTriggers.BALLOON_SHOT_TRIGGER.trigger(player)
        }

        level.destroyBlock(hit.blockPos, false)
        shotBalloon(hit.blockPos)
        Direction.entries.forEach {
            val neighbor = hit.blockPos.relative(it)
            if (level.getBlockState(neighbor).block == this &&
                level.random.nextFloat() < 0.5
            ) {
                shotBalloon(neighbor)
                level.destroyBlock(neighbor, false)
            }
        }
    }

    class DocImpl: Documented {
        override fun getDoc() = documentation {
            page("Balloon")
                .kind(Doc.Kind.BLOCK)
                .summary("Increases lift of the ship.")
                .summary("There are two variants: powered- and unpowered- balloon. The powered balloon has higher lift but requires a redstone signal.")
                .summary("The powered balloon will be 3x stronger when powered than the unpowered balloon.")
        }
    }
}
