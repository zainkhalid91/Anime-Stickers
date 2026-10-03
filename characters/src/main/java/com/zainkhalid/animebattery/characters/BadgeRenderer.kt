package com.zainkhalid.animebattery.characters

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.sin

/**
 * Draws a whole badge: plate, gauge fill, optional percentage, and the figure.
 * Holds its own Stroke objects so a frame allocates nothing.
 */
class BadgeRenderer {

    /** Draws the percentage text centred in the given box. Supplied by the app (system font). */
    fun interface TextDrawer {
        fun draw(scope: DrawScope, text: String, left: Float, top: Float, right: Float, bottom: Float)
    }

    private var outlineStroke = Stroke(1f)
    private var outlineWidth = -1f
    private val percentText = Array(101) { it.toString() }

    // Inner pill used to clip the fill. Rebuilt only when the layout moves.
    private val pill = Path()
    private var pillKey = Float.NaN

    private fun updatePill(l: BadgeLayout, inset: Float) {
        val key = l.plateLeft * 31 + l.plateRight * 17 + l.plateTop * 7 + l.plateBottom + inset
        if (key == pillKey) return
        pillKey = key
        val h = l.plateHeight - 2 * inset
        pill.reset()
        pill.addRoundRect(
            RoundRect(
                l.plateLeft + inset, l.plateTop + inset, l.plateRight - inset, l.plateBottom - inset,
                CornerRadius(h / 2f),
            ),
        )
    }

    fun draw(
        scope: DrawScope,
        art: CharacterArt,
        f: Frame,
        layout: BadgeLayout,
        showPercent: Boolean = true,
        text: TextDrawer? = null,
    ) = with(scope) {
        val g = art.gauge
        val l = layout
        val h = l.plateHeight
        val w = (h * 0.11f).coerceAtLeast(1.5f)
        if (w != outlineWidth) {
            outlineWidth = w
            outlineStroke = Stroke(w)
        }

        // Plate. Fully opaque: this is what hides the stock battery underneath.
        drawRoundRect(g.track, Offset(l.plateLeft, l.plateTop), Size(l.plateRight - l.plateLeft, h), CornerRadius(h / 2f))

        // Fill, clipped to the inside of the pill so its left end is rounded too.
        val inset = w
        updatePill(l, inset)
        val fillRight = l.gaugeLeft + (l.gaugeRight - l.gaugeLeft) * (f.level / 100f)
        if (fillRight > l.plateLeft + inset) {
            clipPath(pill) {
                clipRect(right = fillRight) {
                    art.drawGaugeFill(this, l.plateLeft + inset, l.plateTop + inset, fillRight, l.plateBottom - inset, f)
                }
            }
        }

        drawRoundRect(
            g.outline, Offset(l.plateLeft + w / 2, l.plateTop + w / 2),
            Size(l.plateRight - l.plateLeft - w, h - w), CornerRadius((h - w) / 2f), style = outlineStroke,
        )
        // Critical pulse: a red ring that swells out and fades.
        if (f.pulse > 0f) {
            val grow = w * 1.4f * f.pulse
            drawRoundRect(
                g.critical, Offset(l.plateLeft + w / 2 - grow, l.plateTop + w / 2 - grow),
                Size(l.plateRight - l.plateLeft - w + 2 * grow, h - w + 2 * grow),
                CornerRadius((h - w) / 2f + grow), alpha = 0.35f + 0.65f * f.pulse, style = outlineStroke,
            )
        }

        if (showPercent && text != null) {
            text.draw(this, percentText[f.level.toInt().coerceIn(0, 100)], l.gaugeLeft, l.plateTop, l.gaugeRight, l.plateBottom)
        }

        // Figure, with the squash-pop on state changes (pivot at the feet).
        val p = f.transition
        val s = if (p < 1f) sin(PI * p).toFloat() else 0f
        val sx = 1f + 0.10f * s
        val sy = 1f - 0.14f * s
        withTransform({
            translate(l.figureLeft, l.figureTop + f.bob * l.unit)
            scale(l.unit, l.unit, Offset.Zero)
            scale(sx, sy, Offset(BadgeLayout.ART_WIDTH / 2f, BadgeLayout.ART_HEIGHT))
        }) {
            art.drawFigure(this, f)
        }
    }
}
