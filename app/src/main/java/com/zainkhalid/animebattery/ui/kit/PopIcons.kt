package com.zainkhalid.animebattery.ui.kit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons on a 24 grid, 2.2 stroke with round ends (Lucide-like), so they sit well
 * next to the chunky Nunito type. Tint them with Icon(tint = ...).
 */
object PopIcons {
    private fun icon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = if (filled) null else SolidColor(Color.Black),
                    strokeLineWidth = 2.2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Buddy = icon(
        "buddy",
        "M12 21 C6 17 3 13.5 3 9.5 C3 6.5 5.2 4.5 7.8 4.5 C9.6 4.5 11 5.5 12 7 C13 5.5 14.4 4.5 16.2 4.5 C18.8 4.5 21 6.5 21 9.5 C21 13.5 18 17 12 21 Z",
    )
    val Studio = icon(
        "studio",
        "M21 4 H14", "M10 4 H3", "M21 12 H12", "M8 12 H3", "M21 20 H16", "M12 20 H3",
        "M14 2 V6", "M8 10 V14", "M16 18 V22",
    )
    val Drops = icon(
        "drops",
        "M3 9 H21 V13 H3 Z", "M12 9 V21", "M19 13 V19 C19 20.1 18.1 21 17 21 H7 C5.9 21 5 20.1 5 19 V13",
        "M12 9 C10.5 6 9 4 7.5 4 C6 4 5.5 5.5 6 6.5 C6.6 7.7 8.5 9 12 9",
        "M12 9 C13.5 6 15 4 16.5 4 C18 4 18.5 5.5 18 6.5 C17.4 7.7 15.5 9 12 9",
    )
    val Me = icon(
        "me",
        "M16 7.5 A4 4 0 1 1 8 7.5 A4 4 0 1 1 16 7.5 Z",
        "M4.5 21 V19.5 C4.5 17 6.5 15 9 15 H15 C17.5 15 19.5 17 19.5 19.5 V21",
    )
    val Sparkle = icon("sparkle", "M12 2.5 L14 9.5 L21 12 L14 14.5 L12 21.5 L10 14.5 L3 12 L10 9.5 Z", filled = true)
    val Bolt = icon("bolt", "M13 2 L4 14 H12 L11 22 L20 10 H12 Z", filled = true)
    val Chevron = icon("chevron", "M9 6 L15 12 L9 18")
    val Back = icon("back", "M15 6 L9 12 L15 18")
    val Image = icon(
        "image",
        "M5 3 H19 C20.1 3 21 3.9 21 5 V19 C21 20.1 20.1 21 19 21 H5 C3.9 21 3 20.1 3 19 V5 C3 3.9 3.9 3 5 3 Z",
        "M11 9 A2 2 0 1 1 7 9 A2 2 0 1 1 11 9 Z",
        "M21 15 L16.5 10.5 L6 21",
    )
    val Grid = icon("grid", "M3 3 H10 V10 H3 Z", "M14 3 H21 V10 H14 Z", "M14 14 H21 V21 H14 Z", "M3 14 H10 V21 H3 Z")
    val Flask = icon(
        "flask",
        "M9 2 H15", "M10 2 V9 L4.5 18.5 C3.9 19.6 4.7 21 6 21 H18 C19.3 21 20.1 19.6 19.5 18.5 L14 9 V2", "M7 15 H17",
    )
    val Power = icon("power", "M12 2 V11", "M18.4 6.6 A9 9 0 1 1 5.6 6.6")
    val Check = icon("check", "M5 12.5 L10 17.5 L19 7")
    val Pencil = icon("pencil", "M16.5 3.5 L20.5 7.5 L8 20 H4 V16 Z", "M14 6 L18 10")
}
