package com.zainkhalid.animebattery.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.zainkhalid.animebattery.characters.BadgeRenderer
import com.zainkhalid.animebattery.characters.GaugeStyle

/**
 * Draws the percentage in the system status bar font (Google Sans Flex on Pixel),
 * white with a dark outline so it reads over both the fill and the empty track.
 */
class PercentText(style: GaugeStyle) : BadgeRenderer.TextDrawer {

    private val typeface: Typeface = listOf("google-sans-flex", "google-sans-text", "sans-serif")
        .map { Typeface.create(it, Typeface.BOLD) }
        .firstOrNull { it != Typeface.DEFAULT_BOLD } ?: Typeface.DEFAULT_BOLD

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = this@PercentText.typeface
        color = style.text.toArgb()
        textAlign = Paint.Align.CENTER
    }
    private val outline = Paint(fill).apply {
        this.style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = style.textOutline.toArgb()
    }

    override fun draw(scope: DrawScope, text: String, left: Float, top: Float, right: Float, bottom: Float) {
        val h = bottom - top
        val size = h * 0.56f
        if (fill.textSize != size) {
            fill.textSize = size
            outline.textSize = size
            outline.strokeWidth = size * 0.16f
        }
        // Squeeze "100" a little if the gauge is narrow.
        val width = right - left
        val textW = fill.measureText(text)
        val sx = if (textW > width * 0.92f) width * 0.92f / textW else 1f
        fill.textScaleX = sx
        outline.textScaleX = sx
        val x = (left + right) / 2f
        val y = (top + bottom) / 2f - (fill.ascent() + fill.descent()) / 2f
        val c = scope.drawContext.canvas.nativeCanvas
        c.drawText(text, x, y, outline)
        c.drawText(text, x, y, fill)
    }
}
