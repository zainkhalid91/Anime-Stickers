package com.zainkhalid.animebattery.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import androidx.compose.ui.graphics.asAndroidBitmap
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.characters.CharacterAnimator
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.render.StickerCache
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Our own status bar, drawn over the real one (the approach the "emoji battery"
 * apps use). Because the whole bar is ours, the character can be any size and hang
 * below the bar.
 *
 * Layout, left to right: clock ... signal, Wi-Fi, percent, character.
 * Background is the colour sampled just below the bar; ink flips for contrast.
 */
class FullBarView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    // Set by the service.
    var barHeight = (24 * d).roundToInt()
    var startPad = 40f * d
    var endPad = 40f * d
    var characterId = Characters.all.first().id
    var sizeDp = 40f
        set(v) { field = v; requestLayoutChange() }
    var wifiLevel = -1      // -1 = no Wi-Fi, else 0..4
    var cellLevel = -1      // -1 = unknown, else 0..4
    var level = 50
    var state = BatteryState.Mid
    val animator = CharacterAnimator(seed = SystemClock.uptimeMillis())

    /** Called when the window height must change (bigger character). */
    var onSizeNeeded: ((Int) -> Unit)? = null

    private var bg = Color.BLACK
    private var bgTarget = Color.BLACK
    private var ink = Color.WHITE

    private val font = Typeface.create("google-sans-flex", Typeface.NORMAL)
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 500, false) }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 700, false) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val orb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val wifiPath = Path()
    private val rect = RectF()
    private val tick = Runnable { tickNow() }

    /** Total window height: the bar plus whatever the character hangs below it. */
    fun neededHeight(): Int {
        val s = sizeDp * d
        val top = ((barHeight - s) / 2f).coerceAtLeast(2f * d)
        return maxOf(barHeight, (top + s + 4f * d).roundToInt())
    }

    private fun requestLayoutChange() {
        onSizeNeeded?.invoke(neededHeight())
        invalidate()
    }

    fun setBackgroundSample(color: Int) {
        bgTarget = color or 0xFF000000.toInt()
        kick()
    }

    fun show(state: BatteryState, level: Int) {
        this.state = state
        this.level = level
        animator.setLevel(level)
        animator.setState(state, SystemClock.uptimeMillis())
        kick()
    }

    fun kick() {
        removeCallbacks(tick)
        tickNow()
    }

    private fun tickNow() {
        animator.pxPerUnit = sizeDp * d / 27.6f
        var changed = animator.advance(SystemClock.uptimeMillis())
        if (bg != bgTarget) {
            bg = blend(bg, bgTarget, 0.35f)
            if (closeEnough(bg, bgTarget)) bg = bgTarget
            changed = true
        }
        if (changed) invalidate()
        val delay = animator.nextDelayMs()
        val fading = bg != bgTarget
        if ((delay >= 0 || fading) && isAttachedToWindow) postDelayed(tick, if (fading) 40 else delay)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        kick()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = barHeight.toFloat()
        val cy = h / 2f
        ink = if (luminance(bg) > 0.55f) Color.rgb(28, 27, 31) else Color.WHITE

        // Bar background (only the bar; the overhang below stays transparent).
        bgPaint.color = bg
        c.drawRect(0f, 0f, w, h, bgPaint)

        // Clock.
        clockPaint.color = ink
        clockPaint.textSize = 14.5f * d
        val time = DateFormat.format(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm", System.currentTimeMillis()).toString()
        c.drawText(time, startPad, cy - (clockPaint.ascent() + clockPaint.descent()) / 2f, clockPaint)

        // Character, at the right edge.
        val s = sizeDp * d
        val top = ((h - s) / 2f).coerceAtLeast(2f * d)
        val sticker = StickerCache.get(context, characterId, s.roundToInt())?.asAndroidBitmap()
        val f = animator.frame
        val bobPx = f.bob * animator.pxPerUnit
        val sw = sticker?.width?.toFloat() ?: s * 0.76f
        val sx = w - endPad - sw
        if (sticker != null) {
            val p = f.transition
            val squash = if (p < 1f) sin(PI * p).toFloat() else 0f
            c.save()
            c.scale(1f + 0.10f * squash, 1f - 0.14f * squash, sx + sw / 2f, top + s)
            c.drawBitmap(sticker, sx.roundToInt().toFloat(), (top + bobPx).roundToInt().toFloat(), bmpPaint)
            c.restore()
            if (state == BatteryState.Charging) drawOrb(c, sx + sw * 0.70f, top + bobPx + s * 0.77f, s * 0.13f, f.chargeFrame)
        }

        // Percent, just left of the character.
        pctPaint.textSize = 13.5f * d
        pctPaint.color = when (state) {
            BatteryState.Critical, BatteryState.Low -> Color.rgb(255, 84, 84)
            BatteryState.Charging, BatteryState.Charged -> if (luminance(bg) > 0.55f) Color.rgb(20, 140, 70) else Color.rgb(90, 230, 140)
            else -> ink
        }
        val pct = "$level%"
        val pw = pctPaint.measureText(pct)
        var x = sx - 3f * d - pw
        c.drawText(pct, x, cy - (pctPaint.ascent() + pctPaint.descent()) / 2f, pctPaint)

        // Wi-Fi and signal.
        fill.color = ink
        dim.color = Color.argb(80, Color.red(ink), Color.green(ink), Color.blue(ink))
        x -= 6f * d
        if (wifiLevel >= 0) {
            val r = 8.6f * d
            drawWifi(c, x - r, cy + 5.6f * d, r, wifiLevel)
            x -= 2 * r + 5f * d
        }
        val barW = 2.6f * d
        x -= 4 * (barW + 1.4f * d)
        for (i in 0 until 4) {
            val bh = (4f + i * 2.6f) * d
            rect.set(x, cy + 5.4f * d - bh, x + barW, cy + 5.4f * d)
            c.drawRoundRect(rect, barW / 2, barW / 2, if (i < cellLevel) fill else dim)
            x += barW + 1.4f * d
        }
    }

    private fun drawWifi(c: Canvas, cx: Float, cy: Float, r: Float, lvl: Int) {
        // Full fan dimmed, then the filled part for the level.
        for ((paint, frac) in listOf(dim to 1f, fill to (lvl.coerceIn(0, 4) / 4f).coerceAtLeast(0.34f))) {
            val rr = r * frac
            wifiPath.reset()
            wifiPath.moveTo(cx, cy)
            rect.set(cx - rr, cy - rr, cx + rr, cy + rr)
            wifiPath.arcTo(rect, 225f, 90f)
            wifiPath.close()
            c.drawPath(wifiPath, paint)
        }
    }

    private fun drawOrb(c: Canvas, x: Float, y: Float, r: Float, frame: Int) {
        orb.color = 0x553A9BFF
        c.drawCircle(x, y, r * 1.7f, orb)
        orb.color = 0xFF8FD8FF.toInt()
        c.drawCircle(x, y, r, orb)
        orb.color = Color.WHITE
        c.drawCircle(x, y, r * 0.45f, orb)
        orbLine.strokeWidth = r * 0.22f
        rect.set(x - r * 0.72f, y - r * 0.72f, x + r * 0.72f, y + r * 0.72f)
        orbLine.color = Color.WHITE
        c.drawArc(rect, frame * 36f, 120f, false, orbLine)
        orbLine.color = 0xFF2C8BE6.toInt()
        c.drawArc(rect, frame * 36f + 180f, 120f, false, orbLine)
    }

    private fun luminance(c: Int) =
        (0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)) / 255f

    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).roundToInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).roundToInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).roundToInt(),
    )

    private fun closeEnough(a: Int, b: Int) =
        kotlin.math.abs(Color.red(a) - Color.red(b)) < 3 && kotlin.math.abs(Color.green(a) - Color.green(b)) < 3 &&
            kotlin.math.abs(Color.blue(a) - Color.blue(b)) < 3
}
