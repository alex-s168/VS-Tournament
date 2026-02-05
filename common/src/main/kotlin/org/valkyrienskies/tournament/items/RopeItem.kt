package org.valkyrienskies.tournament.items

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.TournamentBlocks
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.blockentity.RopeHookBlockEntity
import org.valkyrienskies.tournament.blocks.RopeHookBlock

class RopeItem : Item(
        Properties().stacksTo(1)
) {

    private var firstClickedPosition: BlockPos? = null
    private var firstClickedShipId: ShipId? = null

    override fun useOn(context: UseOnContext): InteractionResult {

        val level = context.level
        val blockPos = context.clickedPos.immutable()

        val shipID: ShipId? = context.level.getLoadedShipManagingPos(blockPos)?.id

        if (level is ServerLevel && level.getBlockState(blockPos).block == TournamentBlocks.ROPE_HOOK.get()) {
            connectRope(level.getBlockState(blockPos).block as RopeHookBlock, blockPos, shipID, level)
            if (firstClickedPosition == null)
                context.player!!.sendSystemMessage(Component.translatable("chat.vs_tournament.rope.connected"))
            else
                context.player!!.sendSystemMessage(Component.translatable("chat.vs_tournament.rope.first"))

            println("  ROPE --> " + TournamentBlocks.ROPE_HOOK.get() + " < == > " + level.getBlockState(blockPos).block)

            return InteractionResult.CONSUME
        }
        return super.useOn(context)
    }

    @OptIn(GameTickOnly::class)
    private fun connectRope(hookBlock: RopeHookBlock, blockPos: BlockPos, shipId: ShipId?, level: ServerLevel) {
        if (firstClickedPosition == null) {
            // connect first point
            firstClickedShipId = shipId
            firstClickedPosition = blockPos
            return
        }

        // connect full rope
        var otherShipId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        var thisShipId = otherShipId

        if (shipId != null)
            otherShipId = shipId
        if (firstClickedShipId != null)
            thisShipId = firstClickedShipId as ShipId

        println("other $otherShipId")
        println("this $thisShipId")

        if (firstClickedPosition == null)
            firstClickedPosition = blockPos

        val posA = firstClickedPosition!!.toJOMLD().add(0.5, 0.5, 0.5)
        val posB = blockPos.toJOMLD().add(0.5, 0.5, 0.5)

        var posC = firstClickedPosition!!.toJOMLD().add(0.5, 0.5, 0.5)
        var posD = blockPos.toJOMLD().add(0.5, 0.5, 0.5)

        level.getLoadedShipManagingPos(firstClickedPosition!!)?.let { ship ->
            posC = ship.transform
                .shipToWorld
                .transformPosition(firstClickedPosition!!.toJOMLD())
        }

        level.getLoadedShipManagingPos(blockPos)?.let { ship ->
            posD = ship
                .transform
                .shipToWorld
                .transformPosition(blockPos.toJOMLD())
        }

        println("A1 $posA")
        println("B1 $posB")
        println("C1 $posC")
        println("D1 $posD")

        val ropeCompliance = 1e-5 / (level.getShipObjectManagingPos(blockPos)?.inertiaData?.mass ?: 1).toDouble()
        val ropeMaxForce = TournamentConfig.SERVER.ropeMaxForce
        val ropeConstraint = VSDistanceJoint(
            thisShipId, VSJointPose(posA, Quaterniond()),
            otherShipId, VSJointPose(posB, Quaterniond()),
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
            (level.getBlockEntity(firstClickedPosition!!) as RopeHookBlockEntity)
                .setSecondary(blockPos)
        }

        firstClickedPosition = null
        firstClickedShipId = null

        println("Rope created\n")
    }

}