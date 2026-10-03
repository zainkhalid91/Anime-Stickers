package com.zainkhalid.animebattery.overlay

import android.content.Context
import android.graphics.Bitmap
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
 * Our own status bar, drawn over the real one. Because the whole bar is ours the
 * character can be any size, hang below the bar, or sit on an "island" drawn round
 * the camera hole (the iPhone Dynamic Island sticker look).
 *
 * Left: clock. Centre: island. Right: signal, Wi-Fi, battery, and the character when
 * it lives at the battery end.
 *
 * Drawing allocates nothing: paints, paths and rects are fields; the clock string is
 * rebuilt once a minute.
 */
class FullBarView(context: Context) : View(context) {

    enum class Spot { Battery, IslandLean, IslandPeek }

    private val d = resources.displayMetrics.density

    // Set by the service / preview.
    var barHeight = (24 * d).roundToInt()
    var startPad = 40f * d
    var endPad = 40f * d
    var characterId = Characters.all.first().id
    var sizeDp = 40f
        set(v) { field = v; layoutChanged() }
    var spot = Spot.Battery
        set(v) { field = v; layoutChanged() }
    var islandOn = true
        set(v) { field = v; layoutChanged() }
    var islandWidthDp = 104f
        set(v) { field = v; layoutChanged() }
    var sparkles = true
        set(v) { field = v; invalidate() }
    /** Camera hole bounds in this view's coordinates; empty = centre of the bar. */
    val cutout = RectF()
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
    private var clockText = ""
    private var sparkleStep = 0

    private val font = Typeface.create("google-sans-flex", Typeface.NORMAL)
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 500, false) }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 700, false) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private val islandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val orb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparkle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val battLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val rect = RectF()
    private val island = RectF()
    private val tick = Runnable { tickNow() }

    init { updateClock() }

    // ── Layout ────────────────────────────────────────────────────────────

    private val sizePx get() = sizeDp * d

    private fun islandRect(out: RectF) {
        // Android reports the cutout as a strip the full height of the status bar, so
        // use its width (lens plus margin) and assume the lens sits as far down from the
        // top as it is in from the sides.
        val cx = if (cutout.isEmpty) width / 2f else cutout.centerX()
        val cy = if (cutout.isEmpty) barHeight / 2f else cutout.top + cutout.width() / 2f
        val h = if (cutout.isEmpty) barHeight * 0.58f else minOf(barHeight * 0.62f, cutout.width() * 0.82f)
        val w = maxOf(islandWidthDp * d, h)
        out.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
    }

    /** Where the character's top-left goes, for the current spot. */
    private fun characterTop(): Float = ((barHeight - sizePx) / 2f).coerceAtLeast(2f * d)

    /** Total window height: the bar plus whatever the character hangs below it. */
    fun neededHeight(): Int {
        val bottom = characterTop() + sizePx + 4f * d
        return maxOf(barHeight, bottom.roundToInt())
    }

    private fun layoutChanged() {
        onSizeNeeded?.invoke(neededHeight())
        invalidate()
    }

    // ── State ─────────────────────────────────────────────────────────────

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

    /** Once a minute from ACTION_TIME_TICK. */
    fun updateClock() {
        clockText = DateFormat.format(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm", System.currentTimeMillis()).toString()
        invalidate()
    }

    fun kick() {
        removeCallbacks(tick)
        tickNow()
    }

    private fun tickNow() {
        val now = SystemClock.uptimeMillis()
        animator.pxPerUnit = sizePx / 27.6f
        var changed = animator.advance(now)
        if (bg != bgTarget) {
            bg = blend(bg, bgTarget, 0.35f)
            if (closeEnough(bg, bgTarget)) bg = bgTarget
            changed = true
        }
        // Sparkles twinkle at 2 steps a second, only while animations run.
        val twinkle = sparkles && islandOn && animator.animationsEnabled
        if (twinkle) {
            val step = (now / SPARKLE_STEP_MS).toInt()
            if (step != sparkleStep) { sparkleStep = step; changed = true }
        }
        if (changed) invalidate()

        val fading = bg != bgTarget
        var delay = animator.nextDelayMs()
        if (twinkle) delay = if (delay < 0) SPARKLE_STEP_MS - now % SPARKLE_STEP_MS else minOf(delay, SPARKLE_STEP_MS)
        if (fading) delay = 40
        if (delay >= 0 && isAttachedToWindow) postDelayed(tick, delay)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        kick()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = barHeight.toFloat()
        val cy = h / 2f
        val light = luminance(bg) > 0.55f
        ink = if (light) Color.rgb(28, 27, 31) else Color.WHITE

        bgPaint.color = bg
        c.drawRect(0f, 0f, w, h, bgPaint)

        clockPaint.color = ink
        clockPaint.textSize = 14.5f * d
        c.drawText(clockText, startPad, cy - (clockPaint.ascent() + clockPaint.descent()) / 2f, clockPaint)

        val sticker = StickerCache.get(context, characterId, sizePx.roundToInt())?.asAndroidBitmap()
        val sw = sticker?.width?.toFloat() ?: sizePx * 0.76f
        islandRect(island)

        // Right cluster. With the character at the battery end it takes the last slot;
        // otherwise a compact battery icon does.
        var x = w - endPad
        if (spot == Spot.Battery) {
            x -= sw
            if (sticker != null) drawCharacter(c, sticker, x, characterTop())
            x -= 3f * d
        } else {
            x -= 24f * d
            drawBatteryIcon(c, x, cy, 24f * d, 12f * d, light)
            x -= 4f * d
        }
        pctPaint.textSize = 13.5f * d
        pctPaint.color = when (state) {
            BatteryState.Critical, BatteryState.Low -> Color.rgb(255, 84, 84)
            BatteryState.Charging, BatteryState.Charged -> if (light) Color.rgb(20, 140, 70) else Color.rgb(90, 230, 140)
            else -> ink
        }
        val pct = PERCENT[level.coerceIn(0, 100)]
        x -= pctPaint.measureText(pct)
        c.drawText(pct, x, cy - (pctPaint.ascent() + pctPaint.descent()) / 2f, pctPaint)

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

        // Island and its character. Peek: character first, half tucked behind the island's
        // left end, so it looks round the corner at you.
        if (islandOn) {
            if (spot == Spot.IslandPeek && sticker != null) {
                drawCharacter(c, sticker, island.left - sw * 0.62f, characterTop())
            }
            val r = island.height() / 2f
            c.drawRoundRect(island, r, r, islandPaint)
            if (sparkles) drawSparkles(c)
            if (spot == Spot.IslandLean && sticker != null) {
                drawCharacter(c, sticker, island.right - sw * 0.22f, characterTop())
            }
        } else if (spot != Spot.Battery && sticker != null) {
            drawCharacter(c, sticker, w / 2f - sw / 2f, characterTop())
        }
    }

    private fun drawCharacter(c: Canvas, sticker: Bitmap, left: Float, top: Float) {
        val f = animator.frame
        val bob = f.bob * animator.pxPerUnit
        val p = f.transition
        val squash = if (p < 1f) sin(PI * p).toFloat() else 0f
        val s = sizePx
        c.save()
        c.scale(1f + 0.10f * squash, 1f - 0.14f * squash, left + sticker.width / 2f, top + s)
        c.drawBitmap(sticker, left.roundToInt().toFloat(), (top + bob).roundToInt().toFloat(), bmpPaint)
        c.restore()
        if (state == BatteryState.Charging) {
            drawOrb(c, left + sticker.width * 0.70f, top + bob + s * 0.77f, s * 0.13f, f.chargeFrame)
        }
    }

    private fun drawBatteryIcon(c: Canvas, left: Float, cy: Float, w: Float, h: Float, light: Boolean) {
        val top = cy - h / 2f
        battLine.strokeWidth = 1.4f * d
        battLine.color = dim.color
        rect.set(left, top, left + w - 2.5f * d, top + h)
        c.drawRoundRect(rect, h * 0.3f, h * 0.3f, battLine)
        rect.set(left + w - 2f * d, cy - h * 0.2f, left + w, cy + h * 0.2f)
        c.drawRoundRect(rect, d, d, dim)
        fill.color = when (state) {
            BatteryState.Critical, BatteryState.Low -> Color.rgb(255, 84, 84)
            BatteryState.Charging, BatteryState.Charged -> if (light) Color.rgb(20, 160, 80) else Color.rgb(90, 230, 140)
            else -> ink
        }
        val inner = w - 2.5f * d - 4f * d
        rect.set(left + 2f * d, top + 2f * d, left + 2f * d + inner * level / 100f, top + h - 2f * d)
        c.drawRoundRect(rect, h * 0.2f, h * 0.2f, fill)
        fill.color = ink
    }

    private fun drawSparkles(c: Canvas) {
        // Four little stars round the island, two of them bright on alternate steps.
        val r = island.height() / 2f
        for (i in 0 until 4) {
            val bright = (i + sparkleStep) % 2 == 0
            val size = (if (bright) 4.2f else 2.6f) * d
            val sx = if (i < 2) island.left - (6f + i * 9f) * d else island.right + (6f + (i - 2) * 9f) * d
            val sy = island.centerY() + (if (i % 2 == 0) -r * 0.55f else r * 0.6f)
            sparkle.color = if (i % 2 == 0) 0xFFFFE27A.toInt() else 0xFFFF9EC7.toInt()
            sparkle.alpha = if (bright) 255 else 150
            path.reset()
            path.moveTo(sx, sy - size)
            path.quadTo(sx, sy, sx + size, sy)
            path.quadTo(sx, sy, sx, sy + size)
            path.quadTo(sx, sy, sx - size, sy)
            path.quadTo(sx, sy, sx, sy - size)
            path.close()
            c.drawPath(path, sparkle)
        }
    }

    private fun drawWifi(c: Canvas, cx: Float, cy: Float, r: Float, lvl: Int) {
        drawFan(c, cx, cy, r, dim)
        drawFan(c, cx, cy, r * (lvl.coerceIn(0, 4) / 4f).coerceAtLeast(0.34f), fill)
    }

    private fun drawFan(c: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        path.reset()
        path.moveTo(cx, cy)
        rect.set(cx - r, cy - r, cx + r, cy + r)
        path.arcTo(rect, 225f, 90f)
        path.close()
        c.drawPath(path, paint)
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

    companion object {
        private const val SPARKLE_STEP_MS = 500L
        private val PERCENT = Array(101) { "$it%" }
    }
}
