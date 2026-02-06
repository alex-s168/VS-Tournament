package org.valkyrienskies.tournament.util

import com.mojang.brigadier.StringReader
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.ParticleArgument
import net.minecraft.core.RegistryAccess
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.Registries
import net.minecraft.world.flag.FeatureFlagSet

fun parseParticle(access: RegistryAccess, part: String): ParticleOptions {
    val ctx = CommandBuildContext.configurable(access, FeatureFlagSet.of())
    return ParticleArgument.readParticle(StringReader(part), ctx.holderLookup(Registries.PARTICLE_TYPE))
}