package com.zainkhalid.animebattery.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Draws a sticker through a small grid so it can bend like jelly: the head stays
 * pinned to the rope while the feet trail sideways ([wobble], px), it stretches when
 * the rope pulls ([stretch], 0 = normal, 0.2 = 20% longer) and squashes after a
 * bounce ([squash], 0..1). One still image, lots of life. Allocates nothing per frame.
 */
class StickerMesh {
    private val verts = FloatArray((COLS + 1) * (ROWS + 1) * 2)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    /** True if the shape would look different from a plain drawBitmap. */
    fun deforms(wobble: Float, stretch: Float, squash: Float) =
        kotlin.math.abs(wobble) > 0.3f || kotlin.math.abs(stretch) > 0.004f || squash > 0.01f

    fun draw(c: Canvas, bmp: Bitmap, left: Float, top: Float, wobble: Float, stretch: Float, squash: Float) {
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val cx = left + w / 2f
        val sy = (1f + stretch.coerceIn(-0.15f, 0.35f)) * (1f - 0.22f * squash)
        val sx = (1f - 0.35f * stretch.coerceIn(0f, 0.35f)) * (1f + 0.18f * squash)
        var i = 0
        for (r in 0..ROWS) {
            val v = r / ROWS.toFloat()
            // Feet trail the most; the head (top row) doesn't move.
            val shift = wobble * v * v
            // Squash bulges the middle a little more than the ends.
            val bulge = 1f + 0.12f * squash * (1f - (2f * v - 1f) * (2f * v - 1f))
            val y = top + h * v * sy
            for (col in 0..COLS) {
                val u = col / COLS.toFloat()
                verts[i++] = cx + (u - 0.5f) * w * sx * bulge + shift
                verts[i++] = y
            }
        }
        c.drawBitmapMesh(bmp, COLS, ROWS, verts, 0, null, 0, paint)
    }

    private companion object {
        const val COLS = 4
        const val ROWS = 6
    }
}
