package com.zainkhalid.animebattery.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.zainkhalid.animebattery.characters.BadgeLayout
import com.zainkhalid.animebattery.characters.CharacterArt
import com.zainkhalid.animebattery.characters.Frame

/**
 * A Pixel-style status bar drawn at real pixels: clock, signal, Wi-Fi, and the
 * Android 17 battery pill, then our badge on top, exactly as the overlay places it.
 * Geometry comes from the Pixel 8 Pro measurements in docs/PHASE0.md, in dp.
 */
class StatusBarMock(private val context: android.content.Context, private val density: Float) {

    private val painter = BadgePainter(density)
    val layout = BadgeLayout()

    private val font = Typeface.create("google-sans-flex", Typeface.NORMAL)
    private val clock = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 500, false) }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wifiPath = Path()
    private val pill = RectF()
    private val pillFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(font, 700, false)
        textAlign = Paint.Align.CENTER
    }

    fun draw(
        bitmap: Bitmap, light: Boolean, level: Int, charging: Boolean,
        art: CharacterArt, frame: Frame, showPercent: Boolean, size: Float = 1f,
    ) {
        val d = density
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val c = Canvas(bitmap)
        c.drawColor(if (light) Color.rgb(246, 242, 247) else Color.rgb(14, 14, 18))
        val fg = if (light) Color.rgb(28, 27, 31) else Color.WHITE
        ink.color = fg
        dim.color = Color.argb(90, Color.red(fg), Color.green(fg), Color.blue(fg))
        val cy = h / 2f

        // Clock on the left.
        clock.color = fg
        clock.textSize = 14.5f * d
        c.drawText("7:09", 40.8f * d, cy - (clock.ascent() + clock.descent()) / 2f, clock)

        // Stock battery pill on the right, end padding 40.8dp like the real one.
        val pillW = 24f * d
        val pillH = 13f * d
        val boltW = if (charging) 6.3f * d else 0f
        val right = w - 40.8f * d
        pill.set(right - boltW - pillW, cy - pillH / 2f, right - boltW, cy + pillH / 2f)
        pillFill.color = if (light) Color.rgb(170, 170, 176) else Color.rgb(150, 150, 156)
        c.drawRoundRect(pill, pillH / 2, pillH / 2, pillFill)
        pillFill.color = if (charging) Color.rgb(36, 200, 80) else fg
        c.save()
        c.clipRect(pill.left, pill.top, pill.left + pill.width() * level / 100f, pill.bottom)
        c.drawRoundRect(pill, pillH / 2, pillH / 2, pillFill)
        c.restore()
        pillText.textSize = 9.5f * d
        pillText.color = Color.rgb(20, 20, 22)
        c.drawText("$level", pill.centerX(), cy - (pillText.ascent() + pillText.descent()) / 2f, pillText)
        val stockRight = right

        // Wi-Fi: a quarter-circle fan, 5.75dp left of the pill.
        val wifiRight = pill.left - 5.75f * d
        val r = 8.4f * d
        val wcx = wifiRight - r
        val wcy = cy + 5.6f * d
        wifiPath.reset()
        wifiPath.moveTo(wcx, wcy)
        wifiPath.arcTo(RectF(wcx - r, wcy - r, wcx + r, wcy + r), 225f, 90f)
        wifiPath.close()
        c.drawPath(wifiPath, ink)

        // Signal: four bars, the last two dimmed.
        val barW = 2.6f * d
        var x = wcx - r - 6f * d - 4 * (barW + 1.4f * d)
        for (i in 0 until 4) {
            val bh = (4f + i * 2.6f) * d
            c.drawRoundRect(x, cy + 5.4f * d - bh, x + barW, cy + 5.4f * d, barW / 2, barW / 2, if (i < 2) ink else dim)
            x += barW + 1.4f * d
        }

        layout.place(
            pill.left, pill.top, stockRight, pill.bottom,
            minLeft = wifiRight + 2f * d, statusBarHeight = h, screenWidth = w,
            density = d, size = size,
        )
        c.save()
        c.translate(layout.windowLeft.toFloat(), 0f)
        val sticker = StickerCache.get(context, art.id, kotlin.math.round(BadgeLayout.ART_HEIGHT * layout.unit).toInt())
        painter.paint(c, art, frame, layout, showPercent, sticker)
        c.restore()
    }
}
