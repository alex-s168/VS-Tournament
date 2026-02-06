package org.valkyrienskies.tournament.blockentity.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3f
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.blockentity.SensorBlockEntity
import org.valkyrienskies.tournament.util.helper.getShipRenderPosition
import java.awt.Color

// TODO: does this actually work?

class SensorBlockEntityRender: BlockEntityRenderer<SensorBlockEntity>
{
    override fun render(
        be: SensorBlockEntity,
        partial: Float,
        pose: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val mc = Minecraft.getInstance()
        val level = mc.level!!

        if (mc.entityRenderDispatcher.shouldRenderHitBoxes()) {
            val normal = be
                .blockState
                .getValue(BlockStateProperties.FACING)
                .normal

            val start = level.getShipRenderPosition(be.blockPos.toJOMLD())
            val startBlaze3D = Vector3f(
                start.x.toFloat(),
                start.y.toFloat(),
                start.z.toFloat()
            )
            pose.last().pose().translate(startBlaze3D)

            val end = level.getShipRenderPosition(
                be.blockPos.toJOMLD().add(normal.toJOMLD().mul(TournamentConfig.SERVER.sensorDistance)))
            val endBlaze3D = Vector3f(
                end.x.toFloat(),
                end.y.toFloat(),
                end.z.toFloat()
            )
            pose.last().pose().translate(endBlaze3D)

            val lineNormal = start.sub(end).normalize()
            val lineNormalX = lineNormal.x.toFloat()
            val lineNormalY = lineNormal.y.toFloat()
            val lineNormalZ = lineNormal.z.toFloat()

            val consumer = bufferSource.getBuffer(RenderType.lines())

            val c = Color.BLUE

            val r = c.red
            val g = c.green
            val b = c.blue
            val a = c.alpha

            consumer.vertex(
                startBlaze3D.x().toDouble(),
                startBlaze3D.y().toDouble(),
                startBlaze3D.z().toDouble()
            ).color(r, g, b, a).normal(lineNormalX, lineNormalY, lineNormalZ).endVertex()
            consumer.vertex(
                endBlaze3D.x().toDouble(),
                endBlaze3D.y().toDouble(),
                endBlaze3D.z().toDouble()
            ).color(r, g, b, a).normal(lineNormalX, lineNormalY, lineNormalZ).endVertex()
        }
    }

}