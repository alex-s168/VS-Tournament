package org.valkyrienskies.tournament

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity

object TournamentModels {
    @JvmField val MODELS = mutableMapOf<ResourceLocation, Model>()

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
        @JvmField val resourceLocation: ResourceLocation,
        @JvmField val checkSides: Boolean = true,
        @JvmField val useAO: Boolean = false,
    ) {
        @JvmField var bakedModel: BakedModel? = null

        val renderer = object : Renderer {
            override fun render(
                matrixStack: PoseStack,
                blockEntity: BlockEntity,
                bufferSource: MultiBufferSource,
                packedLight: Int,
                packedOverlay: Int
            ) {
                val level = blockEntity.level ?: return

                val modelBlockRenderer = Minecraft.getInstance().blockRenderer.modelRenderer
                val fn = if (useAO) modelBlockRenderer::tesselateWithAO else modelBlockRenderer::tesselateWithoutAO
                fn(
                    level,
                    bakedModel!!,
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
        if (rl in MODELS)
            error("Model $name already registered !")
        val model = Model(rl, checkSides, useAO)
        MODELS[rl] = model
        return model
    }

    @JvmField val PROP_BIG = model("block/prop_big_prop")
    @JvmField val PROP_SMALL = model("block/prop_small_prop")
    @JvmField val SOLID_FUEL = model("block/solid_fuel")
    @JvmField val ROTATOR_ROTARY = model("block/rotator_rotary")
    @JvmField val FUEL_TANK_FULL_TRANSPARENT = model(
        "block/fuel_tank_full_transparent",
        useAO = true
    )

    val register by lazy {
        TournamentEvents.collectModelsToBake.on { modelBaker ->
            MODELS.keys.forEach {
                try {
                    modelBaker.loadSimpleModel(it)
                } catch (_: Exception) {
                    println("[Tournament] Failed to load model $it!")
                }
            }
        }

        TournamentEvents.postModelReload.on { modelManager ->
            MODELS.forEach { (key, model) ->
                model.bakedModel = modelManager.`vs_tournament$getModelOrNull`(key) ?: let {
                    // TODO: proper logging
                    println("[Tournament] Failed to get model $key")
                    modelManager.missingModel
                }
            }
        }
    }
}