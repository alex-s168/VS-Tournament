package org.valkyrienskies.tournament.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.tournament.TournamentBlockEntities
import org.valkyrienskies.tournament.TournamentBlocks
import org.valkyrienskies.tournament.util.extension.toBlock
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.streams.asSequence

class ConnectorBlockEntity(pos: BlockPos, state: BlockState):
    BlockEntity(TournamentBlockEntities.CONNECTOR.get(), pos, state)
{
    data class FixedDistanceJointData(
        val ship0: ShipId,
        val localPos0: Vector3dc,
        val ship1: ShipId,
        val localPos1: Vector3dc,
        val dist: Float,
    ) {
        fun toJoint() = VSDistanceJoint(
            shipId0 = ship0,
            pose0 = VSJointPose(localPos0, Quaterniond()),
            shipId1 = ship1,
            pose1 = VSJointPose(localPos1, Quaterniond()),
            minDistance = dist,
            maxDistance = dist,
        )
    }

    var constraint: VSJointId? = null
    var constraintData: FixedDistanceJointData? = null
    var otherbesec: BlockPos? = null
    var redstoneLevel = 0
    var recreate = false

    @OptIn(GameTickOnly::class)
    fun tick() {
        val level = level as? ServerLevel ?: return
        val gtpa = ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId)

        if (recreate) {
            if (redstoneLevel == 0) {
                println("[Tournament] restoring connector constraint")
                val other = Vector3d(constraintData!!.localPos1).toBlock()
                val otherBe = level.getBlockEntity(other) as ConnectorBlockEntity
                assert(otherBe.constraintData == null)
                otherBe.constraint = constraint
                gtpa.addJoint(constraintData!!.toJoint()) {
                    // this happens delayed!

                    constraint = it
                    otherBe.setChanged()
                    this.setChanged()
                }
            }
            recreate = false
        }

        constraint?.let {
            if (redstoneLevel > 0) {
                disconnect()
            }
            return
        }

        if (redstoneLevel == 0) {
            val currentShip = level.getLoadedShipManagingPos(blockPos)
            val blockPosCentered = Vec3.atCenterOf(blockPos).toJOML()
            val transform = currentShip
                ?.transform
                ?.shipToWorld
                ?.transformPosition(blockPosCentered)
                ?: blockPosCentered

            val off = Vector3d(2.0)
            val aabb = AABB(transform.sub(off).toMinecraft(), transform.add(off).toMinecraft())
            val res = mutableListOf<Triple<ServerShip, BlockPos, ConnectorBlockEntity>>()
            level.transformFromWorldToNearbyShipsAndWorld(aabb) { newbb ->
                val ranged = BlockPos.betweenClosedStream(newbb).asSequence()
                ranged.map { it.immutable() to level.getBlockState(it) }
                    .filter { (_, state) -> state.block == TournamentBlocks.CONNECTOR.get() }
                    .mapNotNull { (pos, _) -> level.getLoadedShipManagingPos(pos)?.to(pos) }
                    .filter { (_, pos) -> pos != blockPos }
                    .map { (a, b) -> Triple(a, b, level.getBlockEntity(b) as ConnectorBlockEntity) }
                    .filter { (_, _, be) -> be.constraint == null && be.redstoneLevel == 0 }
                    .toCollection(res)
            }
            res.minByOrNull { sqrt(it.second.distToCenterSqr(transform.toMinecraft())) }?.let { (_, pos, be) ->
                connect(pos, be)
            }
        }
    }

    @OptIn(GameTickOnly::class)
    private fun transform(pos: Vector3dc): Pair<ShipId, Vector3dc> {
        val level = level as ServerLevel
        return level
            .getLoadedShipManagingPos(pos)
            ?.let {
                it.id to it.transform
                    .shipToWorld
                    .transformPosition(pos.get(Vector3d()))
            }
            ?: (level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!! to pos)
    }

    private fun connect(other: BlockPos, otherBe: ConnectorBlockEntity): Boolean {
        val level = level as ServerLevel
        val gtpa = ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId)

        val centerA = Vec3.atCenterOf(blockPos).toJOML()
        val centerB = Vec3.atCenterOf(other).toJOML()

        val (idA, posA) = transform(centerA)
        val (idB, posB) = transform(centerB)

        val cfg = FixedDistanceJointData(
            ship0 = idA,
            localPos0 = centerA,
            ship1 = idB,
            localPos1 = centerB,
            dist = (min(posA.distance(posB), 1.4)).toFloat(),
        )
        constraintData = cfg
        otherbesec = null
        otherBe.constraint = constraint
        otherBe.constraintData = null
        otherBe.otherbesec = blockPos
        gtpa.addJoint(cfg.toJoint()) {
            // this happens delayed!!

            constraint = it
            otherBe.setChanged()
            this.setChanged()
        }
        return constraint != null
    }

    fun disconnect(recursed: Boolean = false) {
        val level = level as? ServerLevel ?: return
        val gtpa = ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId)

        if (!recursed) {
            constraintData?.let {
                fun doo(pos: Vector3dc) {
                    val other = pos.sub(0.5, 0.5, 0.5, Vector3d()).toBlock()
                    val otherBe = level.getBlockEntity(other) as? ConnectorBlockEntity?
                    if (otherBe != this)
                        otherBe?.disconnect(true)
                }
                doo(constraintData!!.localPos0)
                doo(constraintData!!.localPos1)
            }
        }
        constraint?.let {
            gtpa.removeJoint(it)
            constraint = null
            constraintData = null
            this.setChanged()
        }
        if (!recursed) {
            otherbesec?.let {
                val otherBe = level.getBlockEntity(it) as? ConnectorBlockEntity?
                if (otherBe != this)
                    otherBe?.disconnect(true)
            }
        }
    }

    override fun getUpdateTag(): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun saveAdditional(tag: CompoundTag) {
        constraint?.let {
            tag.putInt("constraint", it)
            constraintData?.let {
                tag.putLong("id0", it.ship0)
                tag.putLong("id1", it.ship1)

                tag.putDouble("lp0x", it.localPos0.x())
                tag.putDouble("lp0y", it.localPos0.y())
                tag.putDouble("lp0z", it.localPos0.z())

                tag.putDouble("lp1x", it.localPos1.x())
                tag.putDouble("lp1y", it.localPos1.y())
                tag.putDouble("lp1z", it.localPos1.z())

                tag.putFloat("dist", it.dist)
            }
            otherbesec?.let {
                tag.putInt("obx", it.x)
                tag.putInt("oby", it.y)
                tag.putInt("obz", it.z)
            }
        }
    }

    override fun load(tag: CompoundTag) {
        constraint = null
        if (tag.contains("constraint")) {
            constraint = tag.getInt("constraint")

            if (tag.contains("id0")) {
                recreate = constraintData == null
                constraintData = FixedDistanceJointData(
                    ship0 = tag.getLong("id0"),
                    localPos0 = Vector3d(
                        tag.getDouble("lp0x"),
                        tag.getDouble("lp0y"),
                        tag.getDouble("lp0z"),
                    ),
                    ship1 = tag.getLong("id1"),
                    localPos1 = Vector3d(
                        tag.getDouble("lp1x"),
                        tag.getDouble("lp1y"),
                        tag.getDouble("lp1z"),
                    ),
                    tag.getFloat("dist") // this performs a cast if the tag is still stored as double (from old tournament version)
                )
            }

            if (tag.contains("obx")) {
                otherbesec = BlockPos(
                    tag.getInt("obx"),
                    tag.getInt("oby"),
                    tag.getInt("obz"),
                )
            }
        }
    }

    companion object {
        private const val compliance = 1e-20
        private const val maxForce = 1e10

        val ticker = BlockEntityTicker<ConnectorBlockEntity> { level, _, _, be ->
            if (level !is ServerLevel)
                return@BlockEntityTicker

            assert(level == be.level)
            be.tick()
        }
    }
}