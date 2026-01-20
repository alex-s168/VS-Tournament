package org.valkyrienskies.tournament.items

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.blocks.RopeHookBlock
import org.valkyrienskies.tournament.TournamentBlocks
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.blockentity.RopeHookBlockEntity

class RopeItem : Item(
        Properties().stacksTo(1)
) {

    private var clickedPosition: BlockPos? = null
    private var clickedShipId: ShipId? = null
    private var ropeVSJointId: VSJointId? = null

    override fun useOn(context: UseOnContext): InteractionResult {

        val level = context.level
        val blockPos = context.clickedPos.immutable()

        val shipID: ShipId? = context.level.getShipObjectManagingPos(blockPos)?.id

        if (level is ServerLevel) {
            // if its a hook block
            if (level.getBlockState(blockPos).block == TournamentBlocks.ROPE_HOOK.get()) {
                //hook it up
                connectRope(level.getBlockState(blockPos).block as RopeHookBlock, blockPos, shipID, level)
                if (clickedPosition == null)
                    context.player!!.sendSystemMessage(Component.translatable("chat.vs_tournament.rope.connected"))
                else
                    context.player!!.sendSystemMessage(Component.translatable("chat.vs_tournament.rope.first"))

                println("  ROPE --> " + TournamentBlocks.ROPE_HOOK.get() + " < == > " + level.getBlockState(blockPos).block)

                return InteractionResult.CONSUME
            } else {
                println(" !ROPE --> " + TournamentBlocks.ROPE_HOOK.get() + " < != > " + level.getBlockState(blockPos).block)
            }
        }
        return super.useOn(context)
    }

    private fun connectRope(hookBlock: RopeHookBlock, blockPos: BlockPos, shipId: ShipId?, level: ServerLevel) {
        if(clickedPosition != null) {

            // CONNECT FULL ROPE
            var otherShipId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
            var thisShipId = otherShipId

            if (shipId != null)
                otherShipId = shipId
            if (clickedShipId != null)
                thisShipId = clickedShipId as ShipId

            println("other $otherShipId")
            println("this $thisShipId")

            if (clickedPosition == null)
                clickedPosition = blockPos

            val posA = clickedPosition!!.toJOMLD().add(0.5, 0.5, 0.5)
            val posB = blockPos.toJOMLD().add(0.5, 0.5, 0.5)

            var posC = clickedPosition!!.toJOMLD().add(0.5, 0.5, 0.5)
            var posD = blockPos.toJOMLD().add(0.5, 0.5, 0.5)

            if(level.getShipObjectManagingPos(clickedPosition!!) != null)
                posC = level
                    .getShipObjectManagingPos(clickedPosition!!)!!
                    .transform
                    .shipToWorld
                    .transformPosition(clickedPosition!!.toJOMLD())

            if(level.getShipObjectManagingPos(blockPos) != null)
                posD = level
                    .getShipObjectManagingPos(blockPos)!!
                    .transform
                    .shipToWorld
                    .transformPosition(blockPos.toJOMLD())

            println("A1 $posA")
            println("B1 $posB")
            println("C1 $posC")
            println("D1 $posD")

            val ropeCompliance = 1e-5 / (level.getLoadedShipManagingPos(blockPos)?.inertiaData?.mass ?: 1).toDouble()
            val ropeMaxForce = TournamentConfig.SERVER.ropeMaxForce

            val jointPosA = VSJointPose(posA, Quaterniond())
            val jointPosB = VSJointPose(posB, Quaterniond())

            val ropeConstraint = VSDistanceJoint(
                thisShipId, jointPosA,
                otherShipId, jointPosB,
                VSJointMaxForceTorque(ropeMaxForce.toFloat(), ropeMaxForce.toFloat()),
                ropeCompliance,
                null,
                (posC.sub(posD).length() + 1.0).toFloat()
            )

            println("Length: "+ posC.sub(posD).length())
            println(ropeConstraint)

            val gtpa = ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId)

            gtpa.addJoint(ropeConstraint) {
                (level.getBlockEntity(blockPos) as RopeHookBlockEntity)
                    .setRopeID(it, posA, posB, level)
                (level.getBlockEntity(clickedPosition!!) as RopeHookBlockEntity)
                    .setSecondary(blockPos)
            }

            clickedPosition = null
            clickedShipId = null
            this.ropeVSJointId = null

            println("Done\n")

        } else {

            // CONNECT FIRST POINT
            clickedShipId = shipId
            clickedPosition = blockPos
            ropeVSJointId = null

        }
    }

}