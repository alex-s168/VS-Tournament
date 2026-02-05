package org.valkyrienskies.tournament

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.tournament.services.TournamentPlatformHelper
import org.valkyrienskies.tournament.util.rend.DefinitelyNotCopiedFromCreateSuperRenderTypeBuffer

object TournamentModels {

    private fun getModel(rl: ResourceLocation): BakedModel {
        val model = TournamentPlatformHelper
            .get()
            .loadBakedModel(rl)

        if (model == null) {
            // TODO: proper logging
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
        val resourceLocation: ResourceLocation,
        val checkSides: Boolean = true,
        val useAO: Boolean = false,
    ) {
        val bakedModel: BakedModel by lazy {
            getModel(resourceLocation)
        }

        fun renderNow(matrixStack: PoseStack, blockEntity: BlockEntity, packedOverlay: Int) {
            val level = blockEntity.level ?: return
            val modRend = Minecraft.getInstance().blockRenderer.modelRenderer

            val buf = DefinitelyNotCopiedFromCreateSuperRenderTypeBuffer.getInstance()
            modRend.tesselateWithAO(
                level,
                bakedModel,
                blockEntity.blockState,
                blockEntity.blockPos,
                matrixStack,
                buf.getBuffer(RenderType.cutout()),
                checkSides,
                level.random,
                42L, // Used in ModelBlockRenderer.class in renderModel, not sure what the right number is but this seems to work
                packedOverlay
            )
            buf.draw()
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

                val modRend = Minecraft.getInstance().blockRenderer.modelRenderer
                val fn = if (useAO) modRend::tesselateWithAO else modRend::tesselateWithoutAO
                fn(
                    level,
                    bakedModel,
                    blockEntity.blockState,
                    blockEntity.blockPos,
                    matrixStack,
                    bufferSource.getBuffer(RenderType.cutout()),
                    checkSides,
                    level.random,
                    42L, // Used in ModelBlockRenderer.class in renderModel, not sure what the right number is but this seems to work
                    packedOverlay
                )
            }
        }
    }

    private fun model(name: String, checkSides: Boolean = true, useAO: Boolean = false): Model {
        val rl = ResourceLocation(TournamentMod.MOD_ID, name)

        MODELS += rl

        return Model(rl, checkSides, useAO)
    }

    val PROP_BIG = model("block/prop_big_prop")
    val PROP_SMALL = model("block/prop_small_prop")
    val SOLID_FUEL = model("block/solid_fuel")
    val ROTATOR_ROTARY = model("block/rotator_rotary")
    val FUEL_TANK_FULL_TRANSPARENT = model(
        "block/fuel_tank_full_transparent",
        useAO = true
    )

}