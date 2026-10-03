package com.zainkhalid.animebattery.characters

import com.zainkhalid.animebattery.battery.BatteryState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drives a [Frame] from a clock. Call [advance] each tick; it returns true only when
 * something visible changed, so the caller can skip the redraw. [nextDelayMs] says
 * when to tick again, or -1 when nothing moves (power saver, animations off).
 *
 * Bob is snapped to whole pixels, so an idle character only redraws a few times a
 * second even though the clock ticks at 12 fps.
 */
class CharacterAnimator(seed: Long = 7L) {

    val frame = Frame()

    /** Off when the screen is off, power saver is on, the overlay is hidden, or the user turned it off. */
    var animationsEnabled = true

    /** Pixels per art unit. Only used to snap the bob to whole pixels. */
    var pxPerUnit = 2f

    private val random = Random(seed)
    private var startMs = -1L
    private var transitionStartMs = -1_000_000L
    private var nextBlinkMs = 0L
    private var targetLevel = 50f
    private var lastLevelMs = 0L

    // Last values we reported, to detect visible change.
    private var shownTransitionStep = -1
    private var shownBlink = false
    private var shownBobPx = 0
    private var shownCharge = -1
    private var shownPulseStep = -1
    private var shownLevel = -1
    private var shownLook: BatteryState? = null

    fun setState(state: BatteryState, nowMs: Long) {
        if (state == frame.look && shownLook != null) return
        if (shownLook == null) {
            // First state: no transition, just show it.
            frame.look = state
            frame.previous = state
            frame.transition = 1f
            return
        }
        frame.previous = frame.look
        frame.look = state
        transitionStartMs = nowMs
    }

    fun setLevel(level: Int, immediate: Boolean = false) {
        targetLevel = level.coerceIn(0, 100).toFloat()
        if (immediate || shownLook == null) frame.level = targetLevel
    }

    fun advance(nowMs: Long): Boolean {
        if (startMs < 0) {
            startMs = nowMs
            lastLevelMs = nowMs
            nextBlinkMs = nowMs + nextBlinkGap()
        }
        val t = nowMs - startMs
        frame.timeMs = t
        val moving = animationsEnabled && frame.look != BatteryState.PowerSaver

        // State change: squash-pop over TRANSITION_MS. Instant when animations are off.
        frame.transition = if (!animationsEnabled) 1f
        else ((nowMs - transitionStartMs).toFloat() / TRANSITION_MS).coerceIn(0f, 1f)

        // Gauge eases toward the real level.
        val dt = (nowMs - lastLevelMs).coerceAtLeast(0)
        lastLevelMs = nowMs
        frame.level = if (!animationsEnabled) targetLevel else {
            val step = dt * LEVEL_PER_MS
            val diff = targetLevel - frame.level
            if (abs(diff) <= step) targetLevel else frame.level + step * if (diff > 0) 1 else -1
        }

        // Blink every 3 to 6 s, eyes shut for BLINK_MS.
        if (moving && nowMs >= nextBlinkMs + BLINK_MS) nextBlinkMs = nowMs + nextBlinkGap()
        frame.blink = moving && nowMs >= nextBlinkMs && nowMs < nextBlinkMs + BLINK_MS

        // Breathing bob, ±0.5 unit, snapped to pixels.
        val bobPx = if (moving) (sin(2 * PI * t / BOB_PERIOD_MS) * 0.5 * pxPerUnit).roundToInt() else 0
        frame.bob = bobPx / pxPerUnit

        val tick = (t / FRAME_MS).toInt()
        frame.chargeFrame = if (moving && frame.look == BatteryState.Charging) tick % Frame.CHARGE_FRAMES else 0

        // Critical: a short swell at the start of every 2 s cycle, still in between.
        frame.pulse = if (moving && frame.look == BatteryState.Critical) {
            val c = (t % PULSE_PERIOD_MS).toFloat() / PULSE_MS
            if (c < 1f) sin(PI * c).toFloat() else 0f
        } else 0f

        return detectChange(bobPx)
    }

    private fun detectChange(bobPx: Int): Boolean {
        val transitionStep = (frame.transition * TRANSITION_MS / FRAME_MS).toInt()
        val pulseStep = (frame.pulse * 8).roundToInt()
        val level = frame.level.roundToInt()
        val changed = transitionStep != shownTransitionStep || frame.blink != shownBlink ||
            bobPx != shownBobPx || frame.chargeFrame != shownCharge || pulseStep != shownPulseStep ||
            level != shownLevel || frame.look != shownLook
        shownTransitionStep = transitionStep
        shownBlink = frame.blink
        shownBobPx = bobPx
        shownCharge = frame.chargeFrame
        shownPulseStep = pulseStep
        shownLevel = level
        shownLook = frame.look
        return changed
    }

    /** When to call [advance] again, or -1 if the picture can't change on its own. */
    fun nextDelayMs(): Long {
        val settling = frame.transition < 1f || frame.level != targetLevel
        if (settling) return FRAME_MS.toLong()
        if (!animationsEnabled || frame.look == BatteryState.PowerSaver) return -1
        return FRAME_MS.toLong()
    }

    private fun nextBlinkGap() = random.nextLong(3000, 6001)

    companion object {
        const val FPS = 12
        const val FRAME_MS = 1000f / FPS
        const val TRANSITION_MS = 320f
        const val BLINK_MS = 130L
        const val BOB_PERIOD_MS = 3200.0
        const val PULSE_PERIOD_MS = 2000L
        const val PULSE_MS = 700f
        const val LEVEL_PER_MS = 0.08f
    }
}
