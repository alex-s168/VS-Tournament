package org.valkyrienskies.tournament.util.extension

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import org.valkyrienskies.core.api.world.properties.DimensionId

fun DimensionId.toDimensionKey(): ResourceKey<Level> =
    this.split(":").let {
        ResourceLocation(it[it.size - 2], it[it.size - 1]).toDimensionKey()
    }

fun String.toResourceLocation() =
    ResourceLocation(this)

fun ResourceLocation.toDimensionKey(): ResourceKey<Level> =
    ResourceKey.create(Registries.DIMENSION, this)