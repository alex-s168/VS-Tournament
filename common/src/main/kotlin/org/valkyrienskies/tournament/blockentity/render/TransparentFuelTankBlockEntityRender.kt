package org.valkyrienskies.tournament.blockentity.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import org.lwjgl.opengl.GL11
import org.valkyrienskies.tournament.TournamentModels
import org.valkyrienskies.tournament.blockentity.FuelTankBlockEntity
import org.valkyrienskies.tournament.util.extension.pose

class TransparentFuelTankBlockEntityRender:
    BlockEntityRenderer<FuelTankBlockEntity>
{

    override fun render(
        be: FuelTankBlockEntity,
        partial: Float,
        pose: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (be.wholeShipFillLevelSynced > 0.05f) {
            val byDir = List(Direction.entries.size) { index ->
                if (be.neighborsTransparent[index]) {
                    0.0
                } else {
                    0.01
                }
            }

            val x = byDir[Direction.WEST.ordinal]
            val y = byDir[Direction.DOWN.ordinal]
            val z = byDir[Direction.NORTH.ordinal]

            val nx = byDir[Direction.EAST.ordinal]
            val ny = byDir[Direction.UP.ordinal]
            val nz = byDir[Direction.SOUTH.ordinal]

            pose.pose {
                translate(x, y, z)

                scale(
                    1.0f - (x + nx).toFloat(),
                    (1.0f - (y + ny).toFloat()) * be.wholeShipFillLevelSynced,
                    1.0f - (z + nz).toFloat()
                )

                TournamentModels.SOLID_FUEL.renderer.render(
                    pose,
                    be,
                    bufferSource,
                    packedLight,
                    packedOverlay
                )
            }
        }

        val oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        GL11.glEnable(GL11.GL_CULL_FACE)

        val old = GL11.glGetInteger(GL11.GL_FRONT_FACE)

        GL11.glFrontFace(GL11.GL_CCW)
        TournamentModels.FUEL_TANK_FULL_TRANSPARENT.renderer.render(pose, be, bufferSource, packedLight, packedOverlay)

        GL11.glFrontFace(GL11.GL_CW)
        TournamentModels.FUEL_TANK_FULL_TRANSPARENT.renderer.render(pose, be, bufferSource, packedLight, packedOverlay)

        GL11.glFrontFace(old)

        if (!oldCull) {
            GL11.glDisable(GL11.GL_CULL_FACE)
        }
    }

}