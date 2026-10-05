package com.zainkhalid.animebattery.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.ui.graphics.asAndroidBitmap
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.cast.Cast
import com.zainkhalid.animebattery.cast.Event
import com.zainkhalid.animebattery.cast.Look
import com.zainkhalid.animebattery.cast.look
import com.zainkhalid.animebattery.characters.CharacterAnimator
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.decor.ChargeFx
import com.zainkhalid.animebattery.decor.ChargePainter
import com.zainkhalid.animebattery.decor.DecorPainter
import com.zainkhalid.animebattery.decor.DecorType
import com.zainkhalid.animebattery.decor.Lines
import com.zainkhalid.animebattery.decor.Mood
import com.zainkhalid.animebattery.decor.MoodPainter
import com.zainkhalid.animebattery.decor.Pose
import com.zainkhalid.animebattery.render.PopType
import com.zainkhalid.animebattery.render.StickerCache
import com.zainkhalid.animebattery.render.StickerMesh
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
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
 * The character shows a [Mood] for the battery state (sweat, zzz, steam...) and says
 * a line in a speech bubble under it when the mood changes.
 *
 * In the hanging pose you can play with it (through the small touch window the
 * controller puts over it, see [onCharacterTouch]): tap to boop, drag to pull the rope,
 * flick to swing it round the camera, long-press to make it hide in the camera.
 * [HangPhysics] moves it and [StickerMesh] bends the sticker like jelly.
 *
 * Frame rate: 25 fps only while something visibly moves (sway, floating decor, the
 * charging prop, a moving mood); every vsync while the rope moves after a touch or a
 * pose change is fading; otherwise the character animator's slow idle clock; 0 when
 * nothing moves.
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
    val mood: Mood get() = Mood.of(state)
    /** Speech bubbles on mood changes and [greet]. */
    var speech = true
        set(v) { field = v; if (!v) hideBubble() }
    val animator = CharacterAnimator(seed = SystemClock.uptimeMillis())

    /** Called when the window height must change (bigger character, hanging thread). */
    var onSizeNeeded: ((Int) -> Unit)? = null

    /** Called when where the hanging character rests changes, so its touch area can follow. */
    var onCharacterMoved: (() -> Unit)? = null

    /** Lets you play with the hanging character. */
    var interactive = true
        set(v) { field = v; onCharacterMoved?.invoke() }

    private var bg = Color.BLACK
    private var bgTarget = Color.BLACK
    private var ink = Color.WHITE
    private var clockText = ""
    private var step = 0
    private var phase = 0f

    private val decor = DecorPainter(d)
    private val moods = MoodPainter(d, PopType.nunito(resources, 800))
    private var bubbleText: String? = null
    private var bubbleStartMs = 0L
    private var lastGreetMs = -GREET_GAP_MS
    private var shownMood: Mood? = null
    private var charX = 0f
    private val hideBubbleNow = Runnable { hideBubble() }

    // Playing with the hanging character.
    private val hang = HangPhysics().apply { gravity = 1000f * d }
    private val mesh = StickerMesh()
    private var lastPhysicsNs = 0L
    /** Idle sway fades back in after a touch instead of jumping. */
    private var swayGain = 1f
    /** Window kept tall while being played with, so a long pull isn't clipped. */
    private var playing = false
    private var touching = false
    private var moved = false
    private var longPressed = false
    private var downX = 0f
    private var downY = 0f
    private var grabOffset = 0f
    private var lastTapMs = -1000L
    private var lastBonkMs = -1000L
    /** Coming back out of the camera; keeps the shrink until it's fully out. */
    private var revealing = false
    private var reaction: Mood? = null
    private var reactionUntil = 0L
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val longPress = Runnable { hideInCamera() }
    private val comeOut = Runnable { peek() }
    private val font = Typeface.create("google-sans-flex", Typeface.NORMAL)
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 500, false) }
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(font, 700, false) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val thread = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
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

    /** Thread length for the hanging pose. Fixed: the battery shows in the pose, not the rope. */
    private fun threadLength(): Float = sizePx * 0.55f

    /** Total window height: the bar plus anything hanging below it, and the bubble while it shows. */
    fun neededHeight(): Int {
        cameraRect(rect)
        var bottom = when (layout.pose) {
            Pose.Hanging -> rect.bottom + threadLength() + sizePx * 1.3f
            Pose.Eyes -> barHeight.toFloat()
            else -> standingTop() + sizePx + 4f * d
        }
        for (item in layout.decor) {
            if (item.type == DecorType.Web || item.type.attached) continue
            bottom = maxOf(bottom, item.y * d + decor.base * item.scale * 1.6f)
        }
        if (bubbleText != null) bottom = maxOf(bottom, characterBottom() + 2f * d + moods.bubbleHeight())
        if (playing && layout.pose == Pose.Hanging) bottom = maxOf(bottom, rect.bottom + threadLength() * 3.6f + sizePx * 1.5f + moods.bubbleHeight())
        return maxOf(barHeight, bottom.roundToInt() + (6 * d).roundToInt())
    }

    /** Where the character's feet are right now (the bubble hangs from here). */
    private fun characterBottom(): Float {
        cameraRect(rect)
        return when (layout.pose) {
            Pose.Hanging -> if (hang.hidden) pivotY() + 6f * d else pivotY() + cos(hang.theta) * hang.length + sizePx * hangDrop()
            Pose.Eyes -> barHeight.toFloat()
            else -> standingTop() + sizePx
        }
    }

    /** Rope end to the character's feet, in sizes: lower when it hangs from its fists. */
    private fun hangDrop(): Float = if (gripsOk && stickerLook == Look.Hang) 1.10f else 0.94f

    /** Where the rope is tied, just under the camera. */
    private fun pivotX(): Float { cameraRect(rect); return rect.centerX() }
    private fun pivotY(): Float { cameraRect(rect); return rect.bottom - rect.height() * 0.2f }

    /**
     * Where the hanging character sits at rest, for its touch window: a little bigger
     * than the sticker, and never over the status bar itself so the shade still pulls down.
     * False when there's nothing to touch.
     */
    fun characterTouchBounds(out: RectF): Boolean {
        if (!interactive || layout.pose != Pose.Hanging || width == 0) return false
        val px = pivotX()
        val top = pivotY() + threadLength() + sizePx * (hangDrop() - 1f)
        val w = currentSticker()?.width?.toFloat() ?: (sizePx * 0.8f)
        val pad = 10f * d
        out.set(px - w / 2f - pad, maxOf(top - pad, barHeight + 2f * d), px + w / 2f + pad, top + sizePx + pad)
        return out.height() > 12f * d
    }

    private fun layoutChanged() {
        onSizeNeeded?.invoke(neededHeight())
        onCharacterMoved?.invoke()
        invalidate()
    }

    // ── State ─────────────────────────────────────────────────────────────

    fun setBackgroundSample(color: Int) {
        bgTarget = color or 0xFF000000.toInt()
        kick()
    }

    fun show(state: BatteryState, level: Int) {
        this.state = state
        val levelChanged = level != this.level
        this.level = level
        if (levelChanged) onCharacterMoved?.invoke()
        animator.setLevel(level)
        animator.setState(state, SystemClock.uptimeMillis())
        val m = Mood.of(state)
        if (shownMood != null && m != shownMood) say(Lines.forMood(m, characterId))
        shownMood = m
        kick()
    }

    /** Shows [text] in a bubble under the character for [ms]. */
    fun say(text: String, ms: Long = BUBBLE_MS) {
        if (!speech) return
        bubbleText = text
        bubbleStartMs = SystemClock.uptimeMillis()
        removeCallbacks(hideBubbleNow)
        postDelayed(hideBubbleNow, ms)
        layoutChanged()
        kick()
    }

    /** A line for the current mood, at most once every [GREET_GAP_MS] (e.g. on unlock). */
    fun greet() {
        val now = SystemClock.uptimeMillis()
        if (now - lastGreetMs < GREET_GAP_MS) return
        lastGreetMs = now
        say(Lines.forMood(mood, characterId))
    }

    private fun hideBubble() {
        removeCallbacks(hideBubbleNow)
        if (bubbleText == null) return
        bubbleText = null
        layoutChanged()
    }

    private fun bubblePop(now: Long): Float {
        if (!animator.animationsEnabled) return 1f
        val k = ((now - bubbleStartMs) / BUBBLE_POP_MS.toFloat()).coerceIn(0f, 1f)
        // Ease out with a little overshoot.
        val c = 1.7f
        val x = k - 1f
        return 1f + (c + 1f) * x * x * x + c * x * x
    }

    // ── Playing ───────────────────────────────────────────────────────────

    /** Touches from the controller's touch window, in screen coordinates. */
    fun onCharacterTouch(ev: MotionEvent): Boolean {
        if (layout.pose != Pose.Hanging) return false
        val t = ev.eventTime / 1000f
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                moved = false
                longPressed = false
                downX = ev.rawX
                downY = ev.rawY
                startPlaying()
                postDelayed(longPress, LONG_PRESS_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moved && hypot(ev.rawX - downX, ev.rawY - downY) > slop && !longPressed) {
                    moved = true
                    removeCallbacks(longPress)
                    if (hang.hidden) peek()
                    // Hold it where the finger is, so it doesn't jump.
                    grabOffset = hypot(downX - pivotX(), downY - pivotY()) - hang.length
                    hang.grab()
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    say(Lines.on(Event.Grab, characterId), 1600)
                }
                if (moved) {
                    hang.collide = !hang.hidden && !revealing
                    hang.drag(ev.rawX - pivotX(), ev.rawY - pivotY(), grabOffset, t)
                    hang.takeHit().let { if (it > 0f) bonk(it) }
                    kick()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touching = false
                removeCallbacks(longPress)
                when {
                    moved -> {
                        hang.release(t)
                        if (abs(hang.omega) > 6f) say(Lines.on(Event.Fling, characterId), 1800)
                    }
                    longPressed -> Unit
                    ev.actionMasked == MotionEvent.ACTION_UP -> tap(ev)
                }
                kick()
            }
        }
        return true
    }

    private fun tap(ev: MotionEvent) {
        if (hang.hidden) { peek(); return }
        val now = SystemClock.uptimeMillis()
        val dir = if (ev.rawX < pivotX()) 1f else -1f
        if (now - lastTapMs < DOUBLE_TAP_MS) {
            // Double tap: a big happy spin.
            hang.poke(dir); hang.poke(dir); hang.poke(dir)
            react(Mood.Hyped, 2500)
            say(Lines.on(Event.Cheer, characterId), 2200)
            lastTapMs = -1000L
        } else {
            hang.poke(dir)
            say(Lines.on(Event.Poke, characterId), 1800)
            lastTapMs = now
        }
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun hideInCamera() {
        if (!touching || moved) return
        longPressed = true
        hang.hidden = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        say(Lines.on(Event.Hide, characterId), 1500)
        removeCallbacks(comeOut)
        postDelayed(comeOut, HIDE_MS)
        kick()
    }

    private fun peek() {
        removeCallbacks(comeOut)
        if (!hang.hidden) return
        hang.hidden = false
        revealing = true
        say(Lines.on(Event.Peek, characterId), 1800)
        kick()
    }

    /** The head hit the camera. [speed] px/s, only used to scale the shake. */
    private fun bonk(speed: Float) {
        val now = SystemClock.uptimeMillis()
        if (now - lastBonkMs < 700) return
        lastBonkMs = now
        react(Mood.Hurt, HURT_MS)
        say(Lines.on(Event.Hurt, characterId), 2200)
        performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
        if (speed > 1200f) hang.resetSpins()
    }

    private fun startPlaying() {
        if (playing) return
        playing = true
        layoutChanged()
    }

    /** Shows another mood's effect for a while (e.g. dizzy after spinning). */
    private fun react(m: Mood, ms: Long) {
        reaction = m
        reactionUntil = SystemClock.uptimeMillis() + ms
        kick()
    }

    private fun onSettled() {
        lastPhysicsNs = 0L
        revealing = false
        if (hang.dizzy()) {
            react(Mood.Fainting, 3000)
            say(Lines.on(Event.Dizzy, characterId), 2500)
        }
        hang.resetSpins()
        if (playing && !touching && !hang.hidden) {
            playing = false
            layoutChanged()
        }
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

    /** True when something on the bar moves continuously and needs the 8 fps clock. */
    private fun needsMotion(): Boolean {
        if (reaction != null) return true
        if (!animator.animationsEnabled) return false
        if (layout.pose == Pose.Hanging && layout.sway) return true
        if (state == BatteryState.Charging && layout.pose != Pose.Eyes) return true
        if (layout.pose != Pose.Eyes && moods.moves(mood)) return true
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
        // The rope after a touch: every frame until it settles.
        val physics = layout.pose == Pose.Hanging && hang.active
        if (physics) {
            val nowNs = System.nanoTime()
            val dt = if (lastPhysicsNs == 0L) 1f / 60f else (nowNs - lastPhysicsNs) / 1e9f
            lastPhysicsNs = nowNs
            hang.restLength = threadLength()
            hang.cameraLength = sizePx * 0.06f + 2f * d
            hang.collide = !hang.hidden && !revealing
            hang.step(dt)
            hang.takeHit().let { if (it > 0f) bonk(it) }
            swayGain = 0f
            changed = true
            if (!hang.active) onSettled()
        } else if (swayGain < 1f) {
            swayGain = (swayGain + MOTION_FRAME_MS / 1500f).coerceAtMost(1f)
        }
        if (reaction != null && now >= reactionUntil) { reaction = null; changed = true }
        val motion = needsMotion()
        if (motion) {
            phase = (now % MOTION_PERIOD_MS).toFloat() / MOTION_PERIOD_MS
            changed = true
        }
        val twinkle = animator.animationsEnabled &&
            (sparklesAroundCamera || layout.pose == Pose.Eyes || layout.decor.any { it.type == DecorType.Sparkle } ||
                (layout.pose != Pose.Eyes && moods.twinkles(mood)))
        if (twinkle) {
            val s = (now / STEP_MS).toInt()
            if (s != step) { step = s; changed = true }
        }
        if (changed) invalidate()

        var delay = animator.nextDelayMs()
        if (twinkle) delay = if (delay < 0) STEP_MS - now % STEP_MS else minOf(delay, STEP_MS)
        if (motion) delay = MOTION_FRAME_MS
        if (bg != bgTarget) delay = 40
        if (bubbleText != null && now - bubbleStartMs < BUBBLE_POP_MS && animator.animationsEnabled) {
            invalidate()
            delay = 33
        }
        if ((physics && hang.active || easing(now)) && isAttachedToWindow) {
            if (!physics) invalidate()
            postOnAnimation(tick)
            return
        }
        if (delay >= 0 && isAttachedToWindow) postDelayed(tick, delay)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) onCharacterMoved?.invoke()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        kick()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        removeCallbacks(longPress)
        removeCallbacks(comeOut)
        touching = false
        playing = false
        hang.hidden = false
        // No relayout here: the window is going away.
        removeCallbacks(hideBubbleNow)
        bubbleText = null
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

        val sticker = if (layout.pose == Pose.Eyes) null else currentSticker()
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
            charX = x + sw / 2f
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
            if (item.type.attached) continue
            decor.draw(c, item.type, item.x * w, item.y * d, item.scale, step, phase)
        }
        if (sparklesAroundCamera) drawCameraSparkles(c)

        // The pose that lives at the camera.
        when (layout.pose) {
            Pose.Camera -> if (sticker != null) {
                drawCharacter(c, sticker, cam.right + 6f * d, standingTop())
                charX = cam.right + 6f * d + sw / 2f
            }
            Pose.Hanging -> if (sticker != null) drawHanging(c, sticker, sw)
            Pose.Eyes -> { drawEyes(c); charX = cam.centerX() }
            Pose.Battery -> Unit
        }

        bubbleText?.let { moods.bubble(c, it, charX, characterBottom() + 2f * d, w, bubblePop(SystemClock.uptimeMillis())) }
    }

    /** Which pose art fits right now: a reaction, being held, or the battery mood. */
    private fun currentLook(): Look = when {
        reaction == Mood.Hurt -> Look.Hurt
        hang.dragging -> Look.Grabbed
        reaction == Mood.Fainting -> Look.Dizzy
        reaction == Mood.Hyped -> Look.Cheer
        else -> mood.look(hanging = layout.pose == Pose.Hanging).let {
            // No art for that mood yet: keep holding the rope rather than standing on air.
            if (layout.pose == Pose.Hanging && it != Look.Hang && !StickerCache.hasOwn(context, characterId, it)) Look.Hang else it
        }
    }

    // The sticker for the current size and look, kept so a frame doesn't even build a cache key.
    private var stickerBmp: Bitmap? = null
    private var stickerKey = ""
    private var stickerLook: Look? = null
    private var stickerPx = -1

    private fun currentSticker(): Bitmap? {
        val px = sizePx.roundToInt()
        val look = currentLook()
        if (px != stickerPx || characterId != stickerKey || look != stickerLook) {
            // Same character, new pose: fade the old one out instead of snapping.
            if (stickerBmp != null && characterId == stickerKey && px == stickerPx && look != stickerLook) {
                swapFrom = stickerBmp
                swapStartMs = SystemClock.uptimeMillis()
            } else {
                swapFrom = null
            }
            stickerBmp = StickerCache.get(context, characterId, look, px)?.asAndroidBitmap()
            stickerPx = px
            stickerKey = characterId
            stickerLook = look
        }
        return stickerBmp
    }

    private fun drawHanging(c: Canvas, sticker: Bitmap, sw: Float) {
        hang.restLength = threadLength()
        val sway = if (layout.sway && animator.animationsEnabled) sin(2 * PI * phase).toFloat() * 7f * swayGain else 0f
        // Positive theta swings right; Canvas.rotate is clockwise, so flip it.
        val deg = sway - Math.toDegrees(hang.theta.toDouble()).toFloat()
        val px = cam.centerX()
        val py = cam.bottom - cam.height() * 0.2f
        val len = hang.length
        c.save()
        c.rotate(deg, px, py)
        thread.color = if (luminance(bg) > 0.55f) 0xAA333333.toInt() else 0xCCFFFFFF.toInt()
        thread.strokeWidth = 1.4f * d
        c.drawLine(px, py, px, py + len, thread)
        // Pulled up into the camera (only when hiding, not on a boop's yo-yo): shrink towards the lens.
        if (hang.hidden || revealing) {
            val inCamera = (len / (sizePx * 0.5f)).coerceIn(0.12f, 1f)
            if (inCamera < 1f) c.scale(inCamera, inCamera, px, py)
        }
        val now = SystemClock.uptimeMillis()
        // Holding-the-rope art hangs below a knot from both fists; other art hangs by
        // the top of its head. Ease between the two so a pose change doesn't jump.
        val grips = findGrips(sticker)
        val target = if (grips) 0.10f else -0.06f
        // Two fists: centred under the knot. One thread: tied to the highest point.
        val attach = if (grips) 0.5f else topAttach(sticker)
        if (dropNow.isNaN()) { dropNow = target; dropTarget = target; attachNow = attach; attachTarget = attach }
        if (target != dropTarget || attach != attachTarget) {
            dropFrom = dropNow
            dropTarget = target
            attachFrom = attachNow
            attachTarget = attach
            dropStartMs = now
        }
        val k = ease(dropStartMs, now)
        dropNow = dropFrom + (dropTarget - dropFrom) * k
        attachNow = attachFrom + (attachTarget - attachFrom) * k
        val left = px - sw * attachNow
        forkNow = if (grips) (if (dropTarget > dropFrom) k else 1f) else (if (dropTarget < dropFrom) 1f - k else 0f)
        val knot = py + len
        val top = knot + sizePx * dropNow
        if (grips && forkNow > 0f) {
            val a = thread.alpha
            thread.alpha = (a * forkNow).toInt()
            c.drawLine(px, knot, left + gripLx, top + gripLy, thread)
            c.drawLine(px, knot, left + gripRx, top + gripRy, thread)
            thread.alpha = a
        }
        gripped = grips
        drawCharacter(c, sticker, left, top)
        gripped = false
        c.restore()
        val rad = Math.toRadians(-deg.toDouble())
        charX = px + (sin(rad) * len).toFloat()
    }

    // Where the fists are on the current sticker, for the split thread. Found once per bitmap.
    private var gripsOf: Bitmap? = null
    private var gripsOk = false
    private var gripLx = 0f
    private var gripLy = 0f
    private var gripRx = 0f
    private var gripRy = 0f
    /** Drawing a gripping sticker: the rope holds it upright, so no mood lean. */
    private var gripped = false
    /** x of the highest point of the art (where a single thread ties on), as a fraction. */
    private var topU = 0.5f
    private var topUOf: Bitmap? = null
    private var attachNow = Float.NaN
    private var attachFrom = 0.5f
    private var attachTarget = 0.5f

    /** Where a single thread ties on: the highest solid point, kept near the middle. */
    private fun topAttach(bmp: Bitmap): Float {
        if (topUOf === bmp) return topU
        topUOf = bmp
        topU = 0.5f
        val w = bmp.width
        loop@ for (y in 0 until (bmp.height * 0.3f).toInt()) for (x in 0 until w) {
            if ((bmp.getPixel(x, y) ushr 24) > 160) { topU = (x / w.toFloat()).coerceIn(0.2f, 0.8f); break@loop }
        }
        return topU
    }
    private val charge = ChargePainter(d)
    private var chargeFx = ChargeFx.None

    // Smoothing: pose swaps crossfade, the rope's attach point and the mood lean ease.
    private var swapFrom: Bitmap? = null
    private var swapStartMs = -1_000L
    private var dropNow = Float.NaN
    private var dropFrom = 0f
    private var dropTarget = 0f
    private var dropStartMs = -1_000L
    private var forkNow = 0f
    private var tiltNow = 0f
    private var lastDrawMs = 0L

    /** 0..1 eased progress of a [SWAP_MS] transition that started at [start]. */
    private fun ease(start: Long, now: Long): Float {
        val k = ((now - start) / SWAP_MS.toFloat()).coerceIn(0f, 1f)
        return k * k * (3f - 2f * k)
    }

    /** Something is mid-transition and needs smooth frames. */
    private fun easing(now: Long): Boolean =
        now - swapStartMs < SWAP_MS || now - dropStartMs < SWAP_MS ||
            abs(tiltNow - ((reaction ?: mood).tiltDeg)) > 0.2f

    private var chargeOf: String? = null

    /** The current character's charging prop. */
    private fun chargeProp(): ChargeFx {
        if (chargeOf != characterId) {
            chargeOf = characterId
            chargeFx = Cast.find(characterId)?.charge ?: ChargeFx.None
        }
        return chargeFx
    }

    /**
     * For the hang look: two raised fists, i.e. the highest solid points in the left and
     * right thirds are about as high as anything in the middle. A single rope held
     * above the head (or hair buns lower than the middle) means one thread, not two.
     */
    private fun findGrips(bmp: Bitmap): Boolean {
        if (stickerLook != Look.Hang) return false
        if (gripsOf === bmp) return gripsOk
        gripsOf = bmp
        val w = bmp.width
        val h = bmp.height
        fun highest(from: Int, to: Int): Pair<Float, Float>? {
            for (y in 0 until (h * 0.45f).toInt()) for (x in from until to) {
                if ((bmp.getPixel(x, y) ushr 24) > 160) return x.toFloat() to y.toFloat()
            }
            return null
        }
        val l = highest(0, (w * 0.38f).toInt())
        val m = highest((w * 0.38f).toInt(), (w * 0.62f).toInt())
        val r = highest((w * 0.62f).toInt(), w)
        val slack = h * GRIP_SLACK
        gripsOk = l != null && r != null && (m == null || (l.second <= m.second + slack && r.second <= m.second + slack))
        if (gripsOk) {
            gripLx = l!!.first; gripLy = l.second
            gripRx = r!!.first; gripRy = r.second
        }
        return gripsOk
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
        val m = reaction ?: mood
        val sw = sticker.width.toFloat()
        val y = (top + bob).roundToInt().toFloat()
        val x = left.roundToInt().toFloat()
        moods.behind(c, m, x, y, sw, s, step)
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastDrawMs).coerceIn(0L, 100L)) / 1000f
        lastDrawMs = now
        // Leans over a bit as the mood gets worse, pivoting on the feet; eases in.
        val tiltTarget = if (gripped) 0f else m.tiltDeg
        tiltNow += (tiltTarget - tiltNow) * (dt * 7f).coerceAtMost(1f)
        c.save()
        if (abs(tiltNow) > 0.05f) c.rotate(tiltNow, left + sw / 2f, top + s)
        // The pose we're leaving fades out underneath.
        val fade = ease(swapStartMs, now)
        val old = swapFrom
        if (old != null && fade < 1f) {
            bmpPaint.alpha = (255 * (1f - fade)).toInt()
            c.drawBitmap(old, (left + (sw - old.width) / 2f).roundToInt().toFloat(), y + (s - old.height), bmpPaint)
            bmpPaint.alpha = 255
        } else if (fade >= 1f) {
            swapFrom = null
        }
        mesh.alpha = if (old != null && fade < 1f) (255 * fade).toInt() else 255
        bmpPaint.alpha = mesh.alpha
        c.scale(1f + 0.10f * squash, 1f - 0.14f * squash, left + sw / 2f, top + s)
        val stretch = if (layout.pose == Pose.Hanging) (hang.stretch / (sizePx * 6f)).coerceIn(-0.1f, 0.3f) else 0f
        if (layout.pose == Pose.Hanging && mesh.deforms(hang.wobble, stretch, hang.squash)) {
            mesh.draw(c, sticker, x, y, hang.wobble.coerceIn(-sw * 0.6f, sw * 0.6f), stretch, hang.squash)
        } else {
            c.drawBitmap(sticker, x, y, bmpPaint)
        }
        bmpPaint.alpha = 255
        mesh.alpha = 255
        if (layout.decor.any { it.type == DecorType.WitchHat }) decor.witchHat(c, x, y, sw, s)
        // Held by the rope: a nervous sweat drop, whatever the battery says.
        moods.front(c, if (hang.dragging) Mood.Tired else m, x, y, sw, s, phase, step)
        c.restore()
        if (state == BatteryState.Charging) {
            // Each character's own charging prop, floating beside them.
            charge.draw(c, chargeProp(), left + sw + s * 0.08f, top + bob + s * 0.55f, s * 0.13f, f.chargeFrame)
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
        /** Sway and floating decor: 25 fps, smooth enough without costing much. */
        private const val MOTION_FRAME_MS = 40L
        private const val SWAP_MS = 220L
        private const val MOTION_PERIOD_MS = 2600L
        private val MOVING = setOf(DecorType.Heart, DecorType.Ghost, DecorType.Spider, DecorType.Leaf, DecorType.Wings, DecorType.Bat)
        private const val BUBBLE_MS = 4000L
        private const val BUBBLE_POP_MS = 260L
        private const val GREET_GAP_MS = 20 * 60 * 1000L
        private const val LONG_PRESS_MS = 450L
        private const val DOUBLE_TAP_MS = 300L
        private const val HIDE_MS = 5000L
        private const val HURT_MS = 1600L
        /** How much lower than the middle a fist may be and still count as raised. */
        private const val GRIP_SLACK = 0.05f
        private val PERCENT = Array(101) { "$it%" }
    }
}
