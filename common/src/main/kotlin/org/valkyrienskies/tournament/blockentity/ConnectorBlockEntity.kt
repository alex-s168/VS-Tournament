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
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJointId
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
    var constraint: VSJointId? = null
    var constraintData: VSFixedJoint? = null
    var otherbesec: BlockPos? = null
    var redstoneLevel = 0
    var recreate = false

    /*fun tick() {
        val level = level as? ServerLevel ?: return

        if (recreate) {
            if (redstoneLevel == 0) {
                println("restoring constraint")
                constraint = level.shipObjectWorld.createNewConstraint(constraintData!!)
                val other = Vector3d(constraintData!!.localPos1).toBlock()
                val otherBe = level.getBlockEntity(other) as ConnectorBlockEntity
                assert(otherBe.constraintData == null)
                otherBe.constraint = constraint
                otherBe.setChanged()
                this.setChanged()
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
            val currentShip = level.getShipObjectManagingPos(blockPos)
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
                    .mapNotNull { (pos, _) -> level.getShipObjectManagingPos(pos)?.to(pos) }
                    .filter { (_, pos) -> pos != blockPos }
                    .map { (a, b) -> Triple(a, b, level.getBlockEntity(b) as ConnectorBlockEntity) }
                    .filter { (_, _, be) -> be.constraint == null && be.redstoneLevel == 0 }
                    .toCollection(res)
            }
            res.minByOrNull { sqrt(it.second.distToCenterSqr(transform.toMinecraft())) }?.let { (_, pos, be) ->
                connect(pos, be)
            }
        }
    }*/

    private fun transform(pos: Vector3dc): Pair<ShipId, Vector3dc> {
        val level = level as ServerLevel
        return level
            .getShipObjectManagingPos(pos)
            ?.let {
                it.id to it.transform
                    .shipToWorld
                    .transformPosition(pos.get(Vector3d()))
            }
            ?: (level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!! to pos)
    }

    /*private fun connect(other: BlockPos, otherBe: ConnectorBlockEntity): Boolean {
        val level = level as ServerLevel

        val centerA = Vec3.atCenterOf(blockPos).toJOML()
        val centerB = Vec3.atCenterOf(other).toJOML()

        val (idA, posA) = transform(centerA)
        val (idB, posB) = transform(centerB)

        val cfg = VSAttachmentConstraint(
            idA,
            idB,
            compliance,
            centerA,
            centerB,
            maxForce,
            min(posA.distance(posB), 1.4),
        )
        constraintData = cfg
        constraint = level.shipObjectWorld.createNewConstraint(cfg)
        otherbesec = null
        otherBe.constraint = constraint
        otherBe.constraintData = null
        otherBe.otherbesec = blockPos
        otherBe.setChanged()
        this.setChanged()
        return constraint != null
    }

    fun disconnect(recursed: Boolean = false) {
        val level = level as? ServerLevel ?: return
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
            level.shipObjectWorld.removeConstraint(constraint!!)
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
    }*/

    override fun getUpdateTag(): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    /*override fun saveAdditional(tag: CompoundTag) {
        constraint?.let {
            tag.putInt("constraint", it)
            constraintData?.let {
                tag.putLong("id0", it.shipId0)
                tag.putLong("id1", it.shipId1)

                tag.putDouble("lp0x", it.localPos0.x())
                tag.putDouble("lp0y", it.localPos0.y())
                tag.putDouble("lp0z", it.localPos0.z())

                tag.putDouble("lp1x", it.localPos1.x())
                tag.putDouble("lp1y", it.localPos1.y())
                tag.putDouble("lp1z", it.localPos1.z())

                tag.putDouble("dist", it.fixedDistance)
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
                constraintData = VSAttachmentConstraint(
                    tag.getLong("id0"),
                    tag.getLong("id1"),
                    compliance,
                    Vector3d(
                        tag.getDouble("lp0x"),
                        tag.getDouble("lp0y"),
                        tag.getDouble("lp0z"),
                    ),
                    Vector3d(
                        tag.getDouble("lp1x"),
                        tag.getDouble("lp1y"),
                        tag.getDouble("lp1z"),
                    ),
                    maxForce,
                    tag.getDouble("dist"),
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
    }*/

    /*companion object {
        private const val compliance = 1e-20
        private const val maxForce = 1e10

        val ticker = BlockEntityTicker<ConnectorBlockEntity> { level, _, _, be ->
            if(level !is ServerLevel)
                return@BlockEntityTicker

            assert(level == be.level)
            be.tick()
        }
    }*/
}