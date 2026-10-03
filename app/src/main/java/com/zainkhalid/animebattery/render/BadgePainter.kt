package com.zainkhalid.animebattery.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.zainkhalid.animebattery.characters.BadgeLayout
import com.zainkhalid.animebattery.characters.BadgeRenderer
import com.zainkhalid.animebattery.characters.CharacterArt
import com.zainkhalid.animebattery.characters.Frame

/**
 * Draws a badge onto a plain android.graphics.Canvas (the overlay View, and the
 * PNG sheet). Wraps the canvas for Compose only when it changes, so steady drawing
 * allocates nothing.
 */
class BadgePainter(density: Float) {
    private val density = Density(density)
    private val scope = CanvasDrawScope()
    private val renderer = BadgeRenderer()
    private var native: android.graphics.Canvas? = null
    private var wrapped: Canvas? = null
    private var text: PercentText? = null
    private var textFor: CharacterArt? = null

    fun paint(
        canvas: android.graphics.Canvas,
        art: CharacterArt,
        frame: Frame,
        layout: BadgeLayout,
        showPercent: Boolean,
    ) {
        if (canvas !== native) {
            native = canvas
            wrapped = Canvas(canvas)
        }
        if (textFor !== art) {
            text = PercentText(art.gauge)
            textFor = art
        }
        scope.draw(density, LayoutDirection.Ltr, wrapped!!, Size(layout.width, layout.height)) {
            renderer.draw(this, art, frame, layout, showPercent, text)
        }
    }
}
