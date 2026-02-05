package org.valkyrienskies.tournament

import net.minecraft.core.HolderGetter
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.features.FeatureUtils
import net.minecraft.data.worldgen.placement.PlacementUtils
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.levelgen.GenerationStep.Decoration
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.placement.*
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest
import org.valkyrienskies.tournament.util.extension.once

object TournamentWorldGen {
    @JvmStatic
    val ORE_PHYNITE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
        ResourceLocation(TournamentMod.MOD_ID, "ore_phynite"))
    @JvmStatic
    val ORE_PHYNITE_MIDDLE = ResourceKey.create(Registries.PLACED_FEATURE,
        ResourceLocation(TournamentMod.MOD_ID, "ore_phynite_middle"))
    @JvmStatic
    val ORE_PHYNITE_UPPER = ResourceKey.create(Registries.PLACED_FEATURE,
        ResourceLocation(TournamentMod.MOD_ID, "ore_phynite_middle"))
    @JvmStatic
    val ORE_PHYNITE_SMALL = ResourceKey.create(Registries.PLACED_FEATURE,
        ResourceLocation(TournamentMod.MOD_ID, "ore_phynite_small"))
    // TODO: make above pattern nicer

    val register by once {

        TournamentEvents.bootstrapOreFeatures.on { ctx ->
            FeatureUtils.register(
                ctx,
                ORE_PHYNITE,
                Feature.ORE,
                OreConfiguration(listOf(
                    OreConfiguration.target(
                        TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                        TournamentBlocks.ORE_PHYNITE.get().defaultBlockState()
                    ),
                    OreConfiguration.target(
                        TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                        TournamentBlocks.ORE_PHYNITE_DEEPSLATE.get().defaultBlockState()
                    )
                ), 9)
            )
        }

        lateinit var placedFeatures: HolderGetter<PlacedFeature>

        TournamentEvents.bootstrapPlacedFeatures.on { ctx ->
            val configuredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE)
            val phynite = configuredFeatures.getOrThrow(ORE_PHYNITE)

            placedFeatures = ctx.lookup(Registries.PLACED_FEATURE)

            PlacementUtils.register(
                ctx,
                ORE_PHYNITE_UPPER, phynite, commonOrePlacement(
                    90, HeightRangePlacement.triangle(
                        VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)
                    )
                )
            )
            PlacementUtils.register(
                ctx,
                ORE_PHYNITE_MIDDLE, phynite, commonOrePlacement(
                    10, HeightRangePlacement.triangle(
                        VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)
                    )
                )
            )
            PlacementUtils.register(
                ctx,
                ORE_PHYNITE_SMALL, phynite, commonOrePlacement(
                    10, HeightRangePlacement.uniform(
                        VerticalAnchor.bottom(), VerticalAnchor.absolute(72)
                    )
                )
            )
        }

        // TODO: do this with datapacks instead
        TournamentEvents.worldGenFeatures.on { builder ->
            builder.addFeature(Decoration.UNDERGROUND_ORES, placedFeatures.getOrThrow(ORE_PHYNITE_UPPER))
            builder.addFeature(Decoration.UNDERGROUND_ORES, placedFeatures.getOrThrow(ORE_PHYNITE_MIDDLE))
            builder.addFeature(Decoration.UNDERGROUND_ORES, placedFeatures.getOrThrow(ORE_PHYNITE_SMALL))
        }
    }

    private fun orePlacement(
        placementModifier: PlacementModifier,
        placementModifier2: PlacementModifier
    ): List<PlacementModifier> {
        return listOf(placementModifier, InSquarePlacement.spread(), placementModifier2, BiomeFilter.biome())
    }

    private fun commonOrePlacement(count: Int, heightRange: PlacementModifier): List<PlacementModifier> {
        return orePlacement(CountPlacement.of(count), heightRange)
    }

    private fun rareOrePlacement(chance: Int, heightRange: PlacementModifier): List<PlacementModifier> {
        return orePlacement(RarityFilter.onAverageOnceEvery(chance), heightRange)
    }

}