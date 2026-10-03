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
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.decor.DecorPainter
import com.zainkhalid.animebattery.decor.DecorType
import com.zainkhalid.animebattery.decor.Pose
import com.zainkhalid.animebattery.render.StickerCache
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Our own status bar, drawn over the real one. Because the whole bar is ours the
 * character can be any size, hang from the camera, or be swapped for big eyes, and
 * decorations can go anywhere.
 *
 * Left: clock. Middle: camera (with whatever pose lives there). Right: signal,
 * Wi-Fi, percent, battery icon, and the character when it stands at the battery.
 *
 * Frame rate: 10 fps only while something visibly moves (sway, floating decor, the
 * charging orb); otherwise the character animator's slow idle clock; 0 when nothing moves.
 */
class FullBarView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    // Set by the service / preview / editor.
    var barHeight = (24 * d).roundToInt()
    var startPad = 40f * d
    var endPad = 40f * d
    var characterId = Characters.all.first().id
    var layout = BarLayout()
        set(v) { field = v; layoutChanged(); kick() }
    var sparklesAroundCamera = true
        set(v) { field = v; invalidate() }
    /** Camera hole bounds in this view's coordinates; empty = centre of the bar. */
    val cutout = RectF()
    var wifiLevel = -1      // -1 = no Wi-Fi, else 0..4
    var cellLevel = -1      // -1 = unknown, else 0..4
    var level = 50
    var state = BatteryState.Mid
    val animator = CharacterAnimator(seed = SystemClock.uptimeMillis())

    /** Called when the window height must change (bigger character, hanging thread). */
    var onSizeNeeded: ((Int) -> Unit)? = null

    private var bg = Color.BLACK
    private var bgTarget = Color.BLACK
    private var ink = Color.WHITE
    private var clockText = ""
    private var step = 0
    private var phase = 0f

    private val decor = DecorPainter(d)
    private val font = Typeface.create("google-sans-flex", Typeface.NORMAL)
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 500, false) }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 700, false) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val orb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thread = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val orbLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val battLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val rect = RectF()
    private val cam = RectF()
    private val battery = RectF()
    private val tick = Runnable { tickNow() }

    init { updateClock() }

    // ── Layout ────────────────────────────────────────────────────────────

    private val sizePx get() = layout.characterSizeDp * d

    /** The camera lens. Android reports the cutout as a full-height strip, so use its width. */
    private fun cameraRect(out: RectF) {
        val cx = if (cutout.isEmpty) width / 2f else cutout.centerX()
        val cy = if (cutout.isEmpty) barHeight / 2f else cutout.top + cutout.width() / 2f
        val r = if (cutout.isEmpty) barHeight * 0.2f else cutout.width() * 0.32f
        out.set(cx - r, cy - r, cx + r, cy + r)
    }

    private fun standingTop(): Float = ((barHeight - sizePx) / 2f).coerceAtLeast(2f * d)

    /** Thread length for the hanging pose: longer as the battery runs down. */
    private fun threadLength(): Float = sizePx * 0.25f + sizePx * 0.9f * (100 - level) / 100f

    /** Total window height: the bar plus anything hanging below it. */
    fun neededHeight(): Int {
        cameraRect(rect)
        var bottom = when (layout.pose) {
            Pose.Hanging -> rect.bottom + sizePx * 1.15f + sizePx * 0.25f + sizePx * 0.9f
            Pose.Eyes -> barHeight.toFloat()
            else -> standingTop() + sizePx + 4f * d
        }
        for (item in layout.decor) {
            if (item.type == DecorType.Web || item.type == DecorType.Wings) continue
            bottom = maxOf(bottom, item.y * d + decor.base * item.scale * 1.6f)
        }
        return maxOf(barHeight, bottom.roundToInt() + (6 * d).roundToInt())
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

    /** True when something on the bar moves continuously and needs the 10 fps clock. */
    private fun needsMotion(): Boolean {
        if (!animator.animationsEnabled) return false
        if (layout.pose == Pose.Hanging && layout.sway) return true
        if (state == BatteryState.Charging && layout.pose != Pose.Eyes) return true
        return layout.decor.any { it.type in MOVING }
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
        val motion = needsMotion()
        if (motion) {
            phase = (now % MOTION_PERIOD_MS).toFloat() / MOTION_PERIOD_MS
            changed = true
        }
        val twinkle = animator.animationsEnabled &&
            (sparklesAroundCamera || layout.pose == Pose.Eyes || layout.decor.any { it.type == DecorType.Sparkle })
        if (twinkle) {
            val s = (now / STEP_MS).toInt()
            if (s != step) { step = s; changed = true }
        }
        if (changed) invalidate()

        var delay = animator.nextDelayMs()
        if (twinkle) delay = if (delay < 0) STEP_MS - now % STEP_MS else minOf(delay, STEP_MS)
        if (motion) delay = MOTION_FRAME_MS
        if (bg != bgTarget) delay = 40
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

        val sticker = if (layout.pose == Pose.Eyes) null
        else StickerCache.get(context, characterId, sizePx.roundToInt())?.asAndroidBitmap()
        val sw = sticker?.width?.toFloat() ?: sizePx * 0.76f
        cameraRect(cam)
        fill.color = ink
        dim.color = Color.argb(80, Color.red(ink), Color.green(ink), Color.blue(ink))

        // Right cluster, right to left: [character] battery percent wifi signal.
        var x = w - endPad
        if (layout.pose == Pose.Battery && sticker != null) {
            x -= sw
            if (layout.decor.any { it.type == DecorType.Wings }) x -= 10f * d
            drawCharacter(c, sticker, x, standingTop())
            x -= 4f * d
        }
        x -= 24f * d
        battery.set(x, cy - 6f * d, x + 24f * d, cy + 6f * d)
        drawBatteryIcon(c, battery, light)
        val hasWings = layout.decor.any { it.type == DecorType.Wings }
        if (hasWings) {
            val flap = if (animator.animationsEnabled) sin(2 * PI * phase * 4).toFloat() * 0.5f + 0.5f else 0f
            decor.wings(c, battery, flap)
        }
        // Leave room for the left wing so it doesn't sit on the percent.
        x -= if (hasWings) 15f * d else 5f * d
        pctPaint.textSize = 13.5f * d
        pctPaint.color = when (state) {
            BatteryState.Critical, BatteryState.Low -> Color.rgb(255, 84, 84)
            BatteryState.Charging, BatteryState.Charged -> if (light) Color.rgb(20, 140, 70) else Color.rgb(90, 230, 140)
            else -> ink
        }
        val pct = PERCENT[level.coerceIn(0, 100)]
        x -= pctPaint.measureText(pct)
        c.drawText(pct, x, cy - (pctPaint.ascent() + pctPaint.descent()) / 2f, pctPaint)

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

        // Placed decorations.
        for (item in layout.decor) {
            if (item.type == DecorType.Wings) continue
            decor.draw(c, item.type, item.x * w, item.y * d, item.scale, step, phase)
        }
        if (sparklesAroundCamera) drawCameraSparkles(c)

        // The pose that lives at the camera.
        when (layout.pose) {
            Pose.Camera -> if (sticker != null) drawCharacter(c, sticker, cam.right + 6f * d, standingTop())
            Pose.Hanging -> if (sticker != null) drawHanging(c, sticker, sw)
            Pose.Eyes -> drawEyes(c)
            Pose.Battery -> Unit
        }
    }

    private fun drawHanging(c: Canvas, sticker: Bitmap, sw: Float) {
        val swing = if (layout.sway && animator.animationsEnabled) sin(2 * PI * phase).toFloat() * 7f else 0f
        val px = cam.centerX()
        val py = cam.bottom - cam.height() * 0.2f
        val len = threadLength()
        c.save()
        c.rotate(swing, px, py)
        thread.color = if (luminance(bg) > 0.55f) 0xAA333333.toInt() else 0xCCFFFFFF.toInt()
        thread.strokeWidth = 1.4f * d
        c.drawLine(px, py, px, py + len, thread)
        // The sticker hangs from the end of the thread by the top of its head.
        drawCharacter(c, sticker, px - sw / 2f, py + len - sizePx * 0.06f)
        c.restore()
    }

    private fun drawEyes(c: Canvas) {
        val rad = barHeight * 0.26f
        val gap = rad * 2.1f
        val t = animator.frame.timeMs
        // Glance somewhere new every ~3 s.
        val look = when (((t / 3000) % 4).toInt()) { 1 -> 0.8f; 3 -> -0.8f; else -> 0f }
        val sleepy = when (state) {
            BatteryState.Critical -> 0.8f
            BatteryState.Low -> 0.5f
            BatteryState.PowerSaver -> 1f
            else -> 0f
        }
        val shut = if (animator.frame.blink) 1f else sleepy
        decor.eyes(c, cam.centerX(), cam.centerY() + 1f * d, rad, gap, look, shut, bg)
    }

    private fun drawCharacter(c: Canvas, sticker: Bitmap, left: Float, top: Float) {
        val f = animator.frame
        val bob = if (layout.pose == Pose.Hanging) 0f else f.bob * animator.pxPerUnit
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

    private fun drawCameraSparkles(c: Canvas) {
        val r = cam.height() / 2f + 2f * d
        for (i in 0 until 4) {
            val bright = (i + step) % 2 == 0
            val size = (if (bright) 4.2f else 2.6f) * d
            val sx = if (i < 2) cam.left - (8f + i * 10f) * d else cam.right + (8f + (i - 2) * 10f) * d
            val sy = cam.centerY() + (if (i % 2 == 0) -r * 0.55f else r * 0.6f)
            decor.sparkle(c, sx, sy, size, if (i % 2 == 0) 0xFFFFE27A.toInt() else 0xFFFF9EC7.toInt())
        }
    }

    private fun drawBatteryIcon(c: Canvas, b: RectF, light: Boolean) {
        val hgt = b.height()
        battLine.strokeWidth = 1.4f * d
        battLine.color = dim.color
        rect.set(b.left, b.top, b.right - 2.5f * d, b.bottom)
        c.drawRoundRect(rect, hgt * 0.3f, hgt * 0.3f, battLine)
        rect.set(b.right - 2f * d, b.centerY() - hgt * 0.2f, b.right, b.centerY() + hgt * 0.2f)
        c.drawRoundRect(rect, d, d, dim)
        fill.color = when (state) {
            BatteryState.Critical, BatteryState.Low -> Color.rgb(255, 84, 84)
            BatteryState.Charging, BatteryState.Charged -> if (light) Color.rgb(20, 160, 80) else Color.rgb(90, 230, 140)
            else -> ink
        }
        val inner = b.width() - 2.5f * d - 4f * d
        rect.set(b.left + 2f * d, b.top + 2f * d, b.left + 2f * d + inner * level / 100f, b.bottom - 2f * d)
        c.drawRoundRect(rect, hgt * 0.2f, hgt * 0.2f, fill)
        fill.color = ink
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
        private const val STEP_MS = 500L
        private const val MOTION_FRAME_MS = 100L
        private const val MOTION_PERIOD_MS = 2600L
        private val MOVING = setOf(DecorType.Heart, DecorType.Ghost, DecorType.Spider, DecorType.Leaf, DecorType.Wings)
        private val PERCENT = Array(101) { "$it%" }
    }
}
