package org.valkyrienskies.tournament.util

import blitz.Provider
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KProperty

data class LazyWithLateParam<T: Any, P>(
    val compute: (P) -> T,
) {
    var value: T? = null

    inline fun get(param: () -> P): T =
        value ?: compute(param()).also { value = it }
}

class rec<T>(fn: (Provider<T>) -> T) {
    val value: T = fn(::value)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}

@JvmInline
value class TypedResourceLocation<T>(val location: ResourceLocation)