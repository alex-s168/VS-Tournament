package org.valkyrienskies.tournament

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.tournament.services.TournamentPlatformHelper

object TournamentModels {

    private fun getModel(rl: ModelResourceLocation): BakedModel {
        val model = TournamentPlatformHelper
            .get()
            .loadBakedModel(rl)

        if (model == null) {
            println("[Tournament] Failed to load model $rl")
            return Minecraft.getInstance().modelManager.missingModel
        }

        return model
    }

    val MODELS = mutableSetOf<ResourceLocation>()

    interface Renderer {
        fun render(
            matrixStack: PoseStack,
            blockEntity: BlockEntity,
            bufferSource: MultiBufferSource,
            packedLight: Int,
            packedOverlay: Int
        )
    }

    data class Model(
        val resourceLocation: ModelResourceLocation
    ) {
        val bakedModel: BakedModel by lazy {
            getModel(resourceLocation)
        }

        val renderer = object : Renderer {
            override fun render(
                matrixStack: PoseStack,
                blockEntity: BlockEntity,
                bufferSource: MultiBufferSource,
                packedLight: Int,
                packedOverlay: Int
            ) {
                val level = blockEntity.level ?: return

                Minecraft.getInstance().blockRenderer.modelRenderer.tesselateWithoutAO(
                    level,
                    bakedModel,
                    blockEntity.blockState,
                    blockEntity.blockPos,
                    matrixStack,
                    bufferSource.getBuffer(RenderType.cutout()),
                    true,
                    level.random,
                    42L, // Used in ModelBlockRenderer.class in renderModel, not sure what the right number is but this seems to work
                    packedOverlay
                )
            }
        }
    }

    private fun model(name: String): Model {
        val rl = ModelResourceLocation(TournamentMod.MOD_ID, name, "")

        MODELS += rl

        return Model(rl)
    }

    val PROP_BIG = model("block/prop_big_prop")
    val PROP_SMALL = model("block/prop_small_prop")

}