package com.zainkhalid.animebattery.characters

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import com.zainkhalid.animebattery.battery.BatteryState
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
        sticker: ImageBitmap? = null,
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
        if (sticker != null) {
            drawSticker(sticker, l, f, sx, sy)
            return@with
        }
        withTransform({
            translate(l.figureLeft, l.figureTop + f.bob * l.unit)
            scale(l.unit, l.unit, Offset.Zero)
            scale(sx, sy, Offset(BadgeLayout.ART_WIDTH / 2f, BadgeLayout.ART_HEIGHT))
        }) {
            art.drawFigure(this, f)
        }
    }

    private var orbStroke = Stroke(1f)
    private var orbStrokeWidth = -1f

    /**
     * Sticker art instead of the vector figure. The bitmap must already be the exact
     * height to draw (see the app's sticker cache), so it's drawn 1:1 and stays sharp.
     * Feet sit where the vector figure's feet would be.
     */
    private fun DrawScope.drawSticker(img: ImageBitmap, l: BadgeLayout, f: Frame, sx: Float, sy: Float) {
        val h = img.height.toFloat()
        val w = img.width.toFloat()
        val feetX = l.figureLeft + BadgeLayout.ART_WIDTH * l.unit / 2f
        val feetY = l.figureTop + BadgeLayout.ART_HEIGHT * l.unit + f.bob * l.unit
        val left = kotlin.math.round(feetX - w / 2f)
        val top = kotlin.math.round(feetY - h)
        withTransform({ scale(sx, sy, Offset(feetX, feetY)) }) {
            drawImage(img, Offset(left, top))
        }
        if (f.look == BatteryState.Charging) {
            // A small spinning orb at the right hand, one step per charge frame.
            val c = Offset(left + w * HAND_X, top + h * HAND_Y)
            val r = h * 0.13f
            val sw = r * 0.22f
            if (sw != orbStrokeWidth) {
                orbStrokeWidth = sw
                orbStroke = Stroke(sw, cap = StrokeCap.Round)
            }
            drawCircle(ORB_GLOW, r * 1.7f, c)
            drawCircle(ORB, r, c)
            drawCircle(Color.White, r * 0.45f, c)
            drawArc(
                Color.White, f.chargeFrame * 36f, 120f, false,
                Offset(c.x - r * 0.72f, c.y - r * 0.72f), Size(r * 1.44f, r * 1.44f), style = orbStroke,
            )
            drawArc(
                ORB_RING, f.chargeFrame * 36f + 180f, 120f, false,
                Offset(c.x - r * 0.72f, c.y - r * 0.72f), Size(r * 1.44f, r * 1.44f), style = orbStroke,
            )
        }
    }

    private companion object {
        /** Right hand position in the sticker, as a fraction of its size. */
        const val HAND_X = 0.70f
        const val HAND_Y = 0.77f
        val ORB = Color(0xFF8FD8FF)
        val ORB_GLOW = Color(0x553A9BFF)
        val ORB_RING = Color(0xFF2C8BE6)
    }
}
