package com.zainkhalid.animebattery.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.asAndroidBitmap
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.render.StickerCache
import kotlin.math.roundToInt

/**
 * Draws widget art as one bitmap: Glance's own building blocks can't do gradients,
 * liquid fills or stickers, but an Image can show anything. Used by the home screen
 * widget and by the in-app previews, so they always match.
 */
object WidgetArt {

    enum class Kind { Pill, Face, Buddy }

    fun kindFor(widthDp: Float, heightDp: Float): Kind = when {
        heightDp < 100f -> Kind.Pill
        widthDp / heightDp >= 1.55f -> Kind.Buddy
        else -> Kind.Face
    }

    fun mood(state: BatteryState): String = when (state) {
        BatteryState.Full -> "Sage mode"
        BatteryState.Good -> "Believe it!"
        BatteryState.Mid -> "Training"
        BatteryState.Low -> "Running low"
        BatteryState.Critical -> "Out of chakra"
        BatteryState.Charging -> "Charging"
        BatteryState.Charged -> "Full power"
        BatteryState.PowerSaver -> "Napping"
        BatteryState.Hot -> "Too hot!"
    }

    fun render(
        context: Context, characterId: String, widthPx: Int, heightPx: Int, density: Float,
        level: Int, state: BatteryState,
    ): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        when (kindFor(w / density, h / density)) {
            Kind.Pill -> pill(context, c, characterId, w.toFloat(), h.toFloat(), density, level, state)
            Kind.Face -> face(context, c, characterId, w.toFloat(), h.toFloat(), density, level, state)
            Kind.Buddy -> buddy(context, c, characterId, w.toFloat(), h.toFloat(), density, level, state)
        }
        return bmp
    }

    private fun levelColors(level: Int, state: BatteryState): IntArray = when {
        state == BatteryState.Charging || state == BatteryState.Charged -> intArrayOf(0xFF9BE7FF.toInt(), 0xFF3A9BFF.toInt())
        level <= 20 -> intArrayOf(0xFFFF8A8A.toInt(), 0xFFFF4D6D.toInt())
        level <= 50 -> intArrayOf(0xFFFFE08A.toInt(), 0xFFFFB020.toInt())
        else -> intArrayOf(0xFF9BF2C9.toInt(), 0xFF2ECC8F.toInt())
    }

    private fun sticker(context: Context, id: String, heightPx: Float): Bitmap? =
        StickerCache.get(context, id, heightPx.roundToInt())?.asAndroidBitmap()

    private fun bold(size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.create("google-sans-flex", Typeface.NORMAL), 800, false)
        textSize = size
        this.color = color
    }

    /** 4x2: mint card, sticker on the left, a big battery with a liquid fill. */
    private fun buddy(context: Context, c: Canvas, id: String, w: Float, h: Float, d: Float, level: Int, state: BatteryState) {
        val r = 28f * d
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, w, h, 0xFFE6FFF4.toInt(), 0xFFA8EBD3.toInt(), Shader.TileMode.CLAMP) }
        c.drawRoundRect(RectF(0f, 0f, w, h), r, r, bg)
        // Soft rays behind the character.
        val ray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30FFFFFF }
        val cx = h * 0.5f; val cy = h * 0.55f
        for (i in 0 until 10) {
            c.save(); c.rotate(i * 36f, cx, cy)
            val p = Path().apply { moveTo(cx, cy); lineTo(cx + w, cy - w * 0.12f); lineTo(cx + w, cy + w * 0.12f); close() }
            c.drawPath(p, ray); c.restore()
        }
        // Battery.
        val bl = h * 0.95f
        val bt = h * 0.22f
        val br = w - 22f * d
        val bb = h - h * 0.22f
        val body = RectF(bl, bt, br, bb)
        val rr = (bb - bt) * 0.24f
        c.drawRoundRect(body, rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xC0FFFFFF.toInt() })
        val inset = 6f * d
        val fillR = RectF(bl + inset, bt + inset, bl + inset + (body.width() - 2 * inset) * level / 100f, bb - inset)
        val cols = levelColors(level, state)
        c.drawRoundRect(fillR, rr - inset, rr - inset, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, bt, 0f, bb, cols[0], cols[1], Shader.TileMode.CLAMP) })
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f * d; color = 0xFF34C08E.toInt() }
        c.drawRoundRect(body, rr, rr, outline)
        c.drawRoundRect(RectF(br + 4f * d, bt + body.height() * 0.32f, br + 11f * d, bb - body.height() * 0.32f), 3f * d, 3f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF34C08E.toInt() })
        val pct = bold(body.height() * 0.46f, 0xFF0E4D3A.toInt())
        val label = "$level%"
        c.drawText(label, body.centerX() - pct.measureText(label) / 2f, body.centerY() - (pct.ascent() + pct.descent()) / 2f, pct)
        val small = bold(12f * d, 0xFF0E4D3A.toInt())
        val moodText = if (state == BatteryState.Charging) "⚡ " + mood(state) else mood(state)
        c.drawText(moodText, bl, bt - 8f * d, small)
        // Character, popping out of the left.
        sticker(context, id, h * 0.92f)?.let { s -> c.drawBitmap(s, h * 0.5f - s.width / 2f, h - s.height - 4f * d, Paint(Paint.FILTER_BITMAP_FLAG)) }
    }

    /** 2x2: night-blue card, big sticker, mood and level underneath. */
    private fun face(context: Context, c: Canvas, id: String, w: Float, h: Float, d: Float, level: Int, state: BatteryState) {
        val r = 28f * d
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, h, 0xFF1B1F3B.toInt(), 0xFF2563EB.toInt(), Shader.TileMode.CLAMP) }
        c.drawRoundRect(RectF(0f, 0f, w, h), r, r, bg)
        // Level ring.
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f * d; strokeCap = Paint.Cap.ROUND }
        val rad = minOf(w, h) * 0.36f
        val cx = w / 2f; val cy = h * 0.43f
        ring.color = 0x30FFFFFF
        c.drawCircle(cx, cy, rad, ring)
        ring.color = levelColors(level, state)[1]
        c.drawArc(RectF(cx - rad, cy - rad, cx + rad, cy + rad), -90f, 360f * level / 100f, false, ring)
        sticker(context, id, rad * 1.75f)?.let { s -> c.drawBitmap(s, cx - s.width / 2f, cy - s.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG)) }
        val t = bold(13f * d, Color.WHITE)
        val text = "${mood(state)} · $level%"
        c.drawText(text, cx - t.measureText(text) / 2f, h - 14f * d, t)
    }

    /** 2x1: dark pill with a small sticker and the state. */
    private fun pill(context: Context, c: Canvas, id: String, w: Float, h: Float, d: Float, level: Int, state: BatteryState) {
        val rr = h / 2f
        c.drawRoundRect(RectF(0f, 0f, w, h), rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE60B1020.toInt() })
        val s = sticker(context, id, h * 0.86f)
        s?.let { c.drawBitmap(it, 10f * d, (h - it.height) / 2f, Paint(Paint.FILTER_BITMAP_FLAG)) }
        val left = 10f * d + (s?.width ?: 0) + 8f * d
        val big = bold(minOf(h * 0.36f, 22f * d), levelColors(level, state)[0])
        c.drawText("$level%", left, h * 0.52f, big)
        val small = bold(11f * d, 0xFFB8C4E0.toInt())
        c.drawText(mood(state), left, h * 0.52f + 15f * d, small)
    }
}
