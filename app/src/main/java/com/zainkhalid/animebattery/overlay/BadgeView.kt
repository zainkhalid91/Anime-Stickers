package com.zainkhalid.animebattery.overlay

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.characters.BadgeLayout
import com.zainkhalid.animebattery.characters.CharacterAnimator
import com.zainkhalid.animebattery.characters.CharacterArt
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.render.BadgePainter

/**
 * The badge in the status bar. A plain View rather than a ComposeView: it only needs
 * to draw, and this way there's no composition, lifecycle or recomposer running in
 * the service.
 *
 * Frame clock: [tick] runs at most at 12 fps and only invalidates when the animator
 * says something visible changed. When nothing can move, it stops posting.
 */
class BadgeView(context: Context) : View(context) {

    val layout = BadgeLayout()
    val animator = CharacterAnimator(seed = SystemClock.uptimeMillis())
    var art: CharacterArt = Characters.all.first()
        set(v) { field = v; invalidate() }
    var showPercent = true
        set(v) { field = v; invalidate() }

    private val painter = BadgePainter(resources.displayMetrics.density)
    private val tick = Runnable { tickNow() }

    fun show(state: BatteryState, level: Int) {
        val now = SystemClock.uptimeMillis()
        animator.setLevel(level)
        animator.setState(state, now)
        kick()
    }

    /** Re-run the clock now, e.g. after a setting or state change. */
    fun kick() {
        removeCallbacks(tick)
        tickNow()
    }

    private fun tickNow() {
        animator.pxPerUnit = layout.unit
        if (animator.advance(SystemClock.uptimeMillis())) invalidate()
        val delay = animator.nextDelayMs()
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

    override fun onDraw(canvas: Canvas) {
        painter.paint(canvas, art, animator.frame, layout, showPercent)
    }
}
