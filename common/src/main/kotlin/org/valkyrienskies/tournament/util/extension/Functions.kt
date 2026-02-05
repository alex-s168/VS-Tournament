package org.valkyrienskies.tournament.util.extension

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import java.io.File

fun void() =
    Unit

fun Any?.void() =
    Unit

inline fun once(crossinline code: () -> Unit) = lazy { code() }

fun String.toBoolean() =
    when (this.lowercase()) {
        "1",
        "on",
        "true",
        "yes",
        "y" -> true

        "0",
        "off",
        "false",
        "no",
        "n" -> false

        else -> error("Invalid boolean value")
    }

fun String.resLoc() =
    ResourceLocation(this)

fun File.contentsRecOnlyFiles(): Sequence<File> =
    sequence {
        listFiles()?.forEach {
            if (it.isDirectory)
                yieldAll(it.contentsRecOnlyFiles())
            else
                yield(it)
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun ComponentContents.toComponent() =
    MutableComponent.create(this)

inline fun MutableComponent.applyStyle(fn: (Style) -> Style) = apply {
    style = fn(style)
}

class StyledComponent(
    @JvmField val original: Component,
    @JvmField val newStyle: Style
): Component {
    override fun getStyle(): Style = newStyle
    override fun getContents(): ComponentContents = original.contents
    override fun getSiblings(): List<Component?> = original.siblings
    override fun getVisualOrderText(): FormattedCharSequence = original.visualOrderText
}

inline fun Component.withStyle(fn: (Style) -> Style): Component =
    StyledComponent(this, fn(this.style))