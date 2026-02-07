package org.valkyrienskies.tournament.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class TournamentParticleUtils {
    private static <T extends ParticleOptions> @NotNull T readParticleImpl(@NotNull FriendlyByteBuf buffer, @NotNull ParticleType<T> particleType) {
        return particleType.getDeserializer().fromNetwork(particleType, buffer);
    }

    public static @NotNull ParticleOptions readParticle(@NotNull FriendlyByteBuf buffer, @NotNull ParticleType<?> particleType) {
        return readParticleImpl(buffer, particleType);
    }
}