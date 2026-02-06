package org.valkyrienskies.tournament.util.helper

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.absoluteValue

fun Level.getShipRenderPosition(pos: Vector3d): Vector3d =
    when (this) {
        is ServerLevel -> convertShipToWorldSpace(pos)
        is ClientLevel -> getShipRenderPosition(pos)
        else -> pos
    }

@OptIn(GameTickOnly::class)
fun ClientLevel.getShipRenderPosition(pos: Vector3d): Vector3d =
    getLoadedShipManagingPos(pos)?.renderTransform?.shipToWorld?.transformPosition(pos)
        ?: pos

fun Level.convertShipToWorldSpace(pos: Vector3d): Vector3d =
    getLoadedShipManagingPos(pos) ?.shipToWorld ?.transformPosition(pos)
        ?: pos

@Suppress("NOTHING_TO_INLINE")
inline fun Level.convertShipToWorldSpace(pos: BlockPos): Vector3d =
    convertShipToWorldSpace(pos.toJOMLD())

@Suppress("NOTHING_TO_INLINE")
inline fun Level.convertShipToWorldSpace(pos: Vec3): Vector3d =
    convertShipToWorldSpace(pos.toJOML())

fun drawParticleLine(a: Vector3d, b: Vector3d, level: Level, particle: ParticleOptions) {
    val le = a.distance(b) * 3
    for (i in 1..le.toInt()) {
        val pos = a.lerp(b, i / le)
        level.addParticle(particle, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
    }
}

fun drawQuadraticParticleCurve(a : Vector3d, c : Vector3d, length: Double, segments:Double, level: Level, particle: ParticleOptions) {
    val lengthAC = (a.sub(c).length() * segments).absoluteValue
    val lengthTOT = length * segments
    val b = c.sub(c.sub(a).div(2.0))

    if(lengthAC < lengthTOT) {
        b.y -= lengthTOT - lengthAC
    }

    for (i in 1..lengthAC.toInt()) {
        val t = i / lengthAC

        val d : Vector3d = a.lerp(b, t)
        val e : Vector3d = b.lerp(c, t)
        val x : Vector3d = d.lerp(e, t)

        level.addParticle(particle, x.x, x.y, x.z, 0.0, 0.0, 0.0)
    }
}