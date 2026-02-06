package org.valkyrienskies.tournament.util.debug

import org.joml.Vector3d
import java.awt.Color

typealias DebugObjectID = Long

abstract class DebugObject(open val always : Boolean)

class DebugLine(val a : Vector3d, val b : Vector3d, val color : Color, override val always : Boolean) : DebugObject(always)

class DebugRect(val a : Vector3d, val b : Vector3d, val color : Color, override val always : Boolean) : DebugObject(always)

class Debug4CR(val a : Vector3d, val b : Vector3d, val c : Vector3d, val d : Vector3d, val color : Color, override val always : Boolean) : DebugObject(always)