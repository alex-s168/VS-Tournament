package org.valkyrienskies.tournament.util.extension

fun Int.makeRange(b: Int): IntRange =
    if (this > b) (b..this) else (this..b)