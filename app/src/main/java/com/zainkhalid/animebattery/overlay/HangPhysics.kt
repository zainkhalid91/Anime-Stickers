package com.zainkhalid.animebattery.overlay

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The hanging character as a pendulum on a stretchy rope, so it can be grabbed,
 * pulled, flicked round the camera and left to settle. Plain maths, no Android, so
 * it's unit tested.
 *
 * Angle [theta] is 0 straight down, positive swings to the right (screen x+).
 * [stretch] is how far past its rest length the rope is, in px.
 * Units: px, seconds, radians.
 */
class HangPhysics {

    var theta = 0f; private set
    var omega = 0f; private set
    var stretch = 0f; private set
    var stretchV = 0f; private set

    /** Sideways jiggle of the body (px at the feet), for the mesh wobble. */
    var wobble = 0f; private set
    private var wobbleV = 0f

    /** Squash on landing a bounce, 0..1. */
    var squash = 0f; private set

    /** Net turns round the camera since release (a swing back and forth cancels out). */
    var spins = 0f; private set

    var dragging = false; private set

    /** How far the rubber band goes, in rest lengths (more for a rubber character). */
    var maxStretch = 3.5f

    /**
     * Rope length at which the head touches the camera. Coming up faster than a gentle
     * float, it bonks: it bounces off and [takeHit] reports how hard.
     */
    var cameraLength = 0f

    /** Bonks only count when this is on (off while hiding in / coming out of the camera). */
    var collide = true

    private var hit = 0f
    private var pressedIn = false

    /** How hard the head last hit the camera (px/s), once; 0 if it didn't. */
    fun takeHit(): Float = hit.also { hit = 0f }

    /** Rope length at rest, px. Set from the battery level. */
    var restLength = 100f

    /** px/s². Set from the screen density so it feels the same on every phone. */
    var gravity = 2600f
    private val swingDamping = 1.1f    // per second
    private val ropeK = 260f           // spring stiffness, 1/s²
    private val ropeDamping = 6f
    private val wobbleK = 420f
    private val wobbleDamping = 7f

    // Recent drag samples for the release velocity.
    private val sampleT = FloatArray(6)
    private val sampleTheta = FloatArray(6)
    private val sampleLen = FloatArray(6)
    private var samples = 0

    /** True while anything visible is still moving and needs per-frame updates. */
    val active: Boolean
        get() = dragging || abs(omega) > 0.02f || abs(theta) > 0.004f ||
            abs(stretch - target) > 0.6f || abs(stretchV) > 4f || abs(wobble) > 0.3f || abs(wobbleV) > 3f || squash > 0.01f

    /** Length of the rope right now. */
    val length: Float get() = (restLength + stretch).coerceAtLeast(restLength * 0.15f)

    /** Finger went down on the character. */
    fun grab() {
        dragging = true
        samples = 0
    }

    /**
     * Finger is at ([dx], [dy]) relative to the pivot, holding the character [grabOffset]
     * px below the rope's end. Time in seconds.
     */
    fun drag(dx: Float, dy: Float, grabOffset: Float, t: Float) {
        val newTheta = atan2(dx, dy)
        val dist = hypot(dx, dy) - grabOffset
        // Rubber band: easy to pull a bit, harder the further you go.
        val raw = dist - restLength
        val max = restLength * maxStretch
        var s = if (raw > 0) max * (1f - 1f / (1f + raw / max)) else raw.coerceAtLeast(-restLength * 0.98f)
        if (collide && cameraLength > 0f) {
            val floor = cameraLength - restLength
            if (s <= floor) {
                // Shoved into the lens by the finger: one bonk per push.
                if (!pressedIn) { hit = maxOf(hit, 600f); pressedIn = true }
                s = floor
            } else if (s > floor + cameraLength) pressedIn = false
        }
        // Legs trail behind the movement.
        wobble += (unwrap(newTheta - theta)) * -60f
        wobble = wobble.coerceIn(-40f, 40f)
        theta = newTheta
        stretch = s
        push(t, theta, s)
    }

    /** Finger lifted: carry on with the velocity it had. */
    fun release(t: Float) {
        dragging = false
        if (samples >= 2) {
            val i = (samples - 1) % sampleT.size
            // Oldest sample no more than ~90 ms back.
            var j = i
            for (k in 1 until minOf(samples, sampleT.size)) {
                val c = (i - k + sampleT.size) % sampleT.size
                if (t - sampleT[c] > 0.09f) break
                j = c
            }
            val dt = (sampleT[i] - sampleT[j]).coerceAtLeast(0.016f)
            omega = (unwrap(sampleTheta[i] - sampleTheta[j]) / dt).coerceIn(-40f, 40f)
            stretchV = ((sampleLen[i] - sampleLen[j]) / dt).coerceIn(-4000f, 4000f)
        }
        spins = 0f
    }

    /** A boop: a kick sideways and a little yo-yo. [dir] -1 or 1. */
    fun poke(dir: Float) {
        omega += dir * 3.2f
        stretchV -= 260f
        wobbleV += dir * 260f
        squash = 0.6f
    }

    /** Pull the rope up into the camera ([hidden] true) or let it back down. */
    var hidden = false

    /** Where the rope wants to be: at rest, or pulled up into the camera. */
    private val target: Float get() = if (hidden) -restLength * 0.92f else 0f

    fun step(dtIn: Float) {
        if (dragging) {
            // Only the body wobble settles while held.
            stepWobble(dtIn)
            return
        }
        // Small fixed steps keep the spring stable even if a frame is late.
        var left = dtIn.coerceAtMost(0.05f)
        while (left > 0f) {
            val dt = minOf(left, 1f / 120f)
            left -= dt
            val a = -ropeK * (stretch - target) - ropeDamping * stretchV
            stretchV += a * dt
            val before = stretch
            stretch += stretchV * dt
            // Head meets the camera: bounce off it.
            if (collide && cameraLength > 0f && restLength + stretch < cameraLength && stretchV < 0f) {
                if (-stretchV > HIT_SPEED) hit = maxOf(hit, -stretchV)
                stretch = cameraLength - restLength
                stretchV = -stretchV * 0.35f
                squash = maxOf(squash, 0.8f)
                wobbleV += if (omega >= 0f) 220f else -220f
            }
            // Coming back up past rest: squash the body a bit.
            if (before > target + 6f && stretch <= target + 6f && stretchV < -200f) squash = maxOf(squash, (-stretchV / 2200f).coerceAtMost(0.7f))

            val len = length
            val alpha = -(gravity / len) * sin(theta) - swingDamping * omega
            omega += alpha * dt
            val dTheta = omega * dt
            theta += dTheta
            spins += dTheta / (2f * PI.toFloat())
            // Keep theta in -π..π so a loop doesn't wind it up forever.
            if (theta > PI) theta -= (2 * PI).toFloat()
            if (theta < -PI) theta += (2 * PI).toFloat()

            stepWobble(dt)
            squash = (squash - dt * 3.5f).coerceAtLeast(0f)
        }
        if (!active) {
            theta = 0f; omega = 0f; stretch = target; stretchV = 0f
            wobble = 0f; wobbleV = 0f; squash = 0f
        }
    }

    private fun stepWobble(dt: Float) {
        // The body lags behind the swing like jelly.
        val drive = -omega * 9f
        val a = -wobbleK * (wobble - drive) - wobbleDamping * wobbleV
        wobbleV += a * dt
        wobble += wobbleV * dt
    }

    /** Settled after a dizzy spin round the camera? */
    fun dizzy() = abs(spins) >= 1.5f

    fun resetSpins() { spins = 0f }

    private companion object {
        /** Slower than this and it just nudges the camera, no "ouch". */
        const val HIT_SPEED = 280f
    }

    private fun push(t: Float, th: Float, len: Float) {
        val i = samples % sampleT.size
        sampleT[i] = t; sampleTheta[i] = th; sampleLen[i] = len
        samples++
    }

    private fun unwrap(a: Float): Float {
        var x = a
        while (x > PI) x -= (2 * PI).toFloat()
        while (x < -PI) x += (2 * PI).toFloat()
        return x
    }
}
