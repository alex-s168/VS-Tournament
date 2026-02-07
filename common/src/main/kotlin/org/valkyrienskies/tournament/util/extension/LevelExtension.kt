package org.valkyrienskies.tournament.util.extension

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import org.joml.Vector3d
import org.valkyrienskies.tournament.util.helper.convertShipToWorldSpace

fun ServerLevel.explodeShip(x : Double, y: Double, z: Double, radius: Float, interaction: Level.ExplosionInteraction) =
    explodeShip(Vector3d(x,y,z), radius, interaction)

fun ServerLevel.explodeShip(pos: Vector3d, radius: Float, interaction: Level.ExplosionInteraction) {
    explode(pos, radius, interaction)
    explode(level.convertShipToWorldSpace(pos), radius, interaction)
}

fun ServerLevel.explode(pos: Vector3d, radius: Float, interaction: Level.ExplosionInteraction) =
    explode(PrimedTnt(EntityType.TNT, this), pos.x, pos.y, pos.z, radius, interaction).void()

inline fun <reified T: BlockEntity> Level.getBlockEntity(pos: BlockPos): T? =
    getBlockEntity(pos) as? T