package com.zainkhalid.animebattery.characters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import com.zainkhalid.animebattery.battery.BatteryState

/** Colours for the plate and gauge. Flat fill plus one shade, like the figures. */
class GaugeStyle(
    val track: Color,
    val outline: Color,
    val fill: Color,
    val fillShade: Color,
    val critical: Color,
    val text: Color = Color.White,
    val textOutline: Color = outline,
)

/**
 * A character. Draws its figure in a 20 x 27.6 unit box (see [BadgeLayout]) and can
 * dress up the gauge fill. Parts should be built once, as properties.
 */
abstract class CharacterArt {
    abstract val id: String
    abstract val name: String
    abstract val series: String
    abstract val rightsHolder: String
    abstract val gauge: GaugeStyle

    /** Draws the figure. The transform is already set, so draw in art units. */
    abstract fun drawFigure(scope: DrawScope, f: Frame)

    /**
     * Fills the gauge from [left] to [right] (already cut to the level).
     * Default: flat fill, a darker band along the bottom, and moving stripes while charging.
     */
    open fun drawGaugeFill(scope: DrawScope, left: Float, top: Float, right: Float, bottom: Float, f: Frame) {
        if (right <= left) return
        val critical = f.look == BatteryState.Critical
        val fill = if (critical) gauge.critical else gauge.fill
        val h = bottom - top
        scope.drawRect(fill, Offset(left, top), androidx.compose.ui.geometry.Size(right - left, h))
        scope.drawRect(
            if (critical) gauge.critical.darken(0.25f) else gauge.fillShade,
            Offset(left, top + h * 0.62f),
            androidx.compose.ui.geometry.Size(right - left, h * 0.38f),
        )
        if (f.look == BatteryState.Charging) {
            // Light diagonal stripes sliding right, one step per charge frame.
            val gap = h * 1.1f
            val shift = gap * f.chargeFrame / Frame.CHARGE_FRAMES
            scope.clipRect(left, top, right, bottom) {
                var x = left - h - gap + shift
                while (x < right + h) {
                    drawLine(STRIPE, Offset(x, bottom), Offset(x + h * 0.7f, top), strokeWidth = h * 0.28f)
                    x += gap
                }
            }
        }
    }

    /**
     * Helper for state changes: draws the old expression fading out and the new one
     * fading in. [block] gets the state to draw and its alpha.
     */
    protected inline fun crossfade(f: Frame, block: (BatteryState, Float) -> Unit) {
        val p = f.transition
        if (p >= 1f || f.previous == f.look) {
            block(f.look, 1f)
        } else {
            val e = p * p * (3 - 2 * p)
            block(f.previous, 1f - e)
            block(f.look, e)
        }
    }

    companion object {
        private val STRIPE = Color(0x55FFFFFF)
    }
}

internal fun Color.darken(by: Float) = Color(red * (1 - by), green * (1 - by), blue * (1 - by), alpha)
