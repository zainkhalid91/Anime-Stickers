package com.zainkhalid.animebattery.decor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** What a character holds while charging. Each hero gets their own, or nothing. */
enum class ChargeFx { None, Rasengan, Meat, HollowPurple, Peanut, Spark }

/**
 * Draws the charging prop beside the character. [frame] counts 0..9 round the loop
 * (the animator's charge frame), so everything turns at the same pace. No allocation.
 */
class ChargePainter(private val d: Float) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val r = RectF()

    fun draw(c: Canvas, fx: ChargeFx, x: Float, y: Float, rad: Float, frame: Int) {
        val t = frame / 10f
        when (fx) {
            ChargeFx.None -> Unit
            ChargeFx.Rasengan -> orb(c, x, y, rad, frame, 0x553A9BFF, 0xFF8FD8FF.toInt(), Color.WHITE, 0xFF2C8BE6.toInt())
            ChargeFx.HollowPurple -> orb(c, x, y, rad, frame, 0x669B5CFF, 0xFFB98CFF.toInt(), 0xFFFF5A7A.toInt(), 0xFF4C8DFF.toInt())
            ChargeFx.Meat -> meat(c, x, y + sin(2 * PI * t).toFloat() * rad * 0.15f, rad)
            ChargeFx.Peanut -> peanut(c, x, y + sin(2 * PI * t).toFloat() * rad * 0.25f, rad, sin(2 * PI * t).toFloat() * 12f)
            ChargeFx.Spark -> spark(c, x, y, rad, t * 90f)
        }
    }

    /** A glowing ball with two spinning arcs (Rasengan blue, Hollow Purple violet). */
    private fun orb(c: Canvas, x: Float, y: Float, rad: Float, frame: Int, glow: Int, body: Int, arcA: Int, arcB: Int) {
        p.color = glow
        c.drawCircle(x, y, rad * 1.7f, p)
        p.color = body
        c.drawCircle(x, y, rad, p)
        p.color = Color.WHITE
        c.drawCircle(x, y, rad * 0.45f, p)
        line.strokeWidth = rad * 0.22f
        r.set(x - rad * 0.72f, y - rad * 0.72f, x + rad * 0.72f, y + rad * 0.72f)
        line.color = arcA
        c.drawArc(r, frame * 36f, 120f, false, line)
        line.color = arcB
        c.drawArc(r, frame * 36f + 180f, 120f, false, line)
    }

    /** Meat on the bone, cartoon style. */
    private fun meat(c: Canvas, x: Float, y: Float, rad: Float) {
        c.save()
        c.rotate(-35f, x, y)
        // Bone: a stick with two knobs.
        p.color = 0xFFFFF4E0.toInt()
        r.set(x + rad * 0.4f, y - rad * 0.16f, x + rad * 1.5f, y + rad * 0.16f)
        c.drawRect(r, p)
        c.drawCircle(x + rad * 1.55f, y - rad * 0.18f, rad * 0.22f, p)
        c.drawCircle(x + rad * 1.55f, y + rad * 0.18f, rad * 0.22f, p)
        // The meat.
        p.color = 0xFFB5482A.toInt()
        r.set(x - rad, y - rad * 0.75f, x + rad * 0.65f, y + rad * 0.75f)
        c.drawOval(r, p)
        p.color = 0xFFD9714A.toInt()
        r.set(x - rad * 0.75f, y - rad * 0.55f, x + rad * 0.2f, y + rad * 0.1f)
        c.drawOval(r, p)
        line.color = 0xFF0D0A14.toInt(); line.strokeWidth = 0.9f * d
        r.set(x - rad, y - rad * 0.75f, x + rad * 0.65f, y + rad * 0.75f)
        c.drawOval(r, line)
        c.restore()
    }

    /** A peanut in its shell, wobbling. */
    private fun peanut(c: Canvas, x: Float, y: Float, rad: Float, tilt: Float) {
        c.save()
        c.rotate(30f + tilt, x, y)
        p.color = 0xFFE3B36B.toInt()
        c.drawCircle(x, y - rad * 0.45f, rad * 0.55f, p)
        c.drawCircle(x, y + rad * 0.45f, rad * 0.6f, p)
        r.set(x - rad * 0.42f, y - rad * 0.4f, x + rad * 0.42f, y + rad * 0.4f)
        c.drawRect(r, p)
        line.color = 0xFFA87732.toInt(); line.strokeWidth = 0.8f * d
        for (i in -1..1) c.drawLine(x - rad * 0.3f, y + i * rad * 0.35f, x + rad * 0.3f, y + i * rad * 0.35f + rad * 0.1f, line)
        line.color = 0xFF0D0A14.toInt(); line.strokeWidth = 0.9f * d
        c.drawCircle(x, y - rad * 0.45f, rad * 0.55f, line)
        c.drawCircle(x, y + rad * 0.45f, rad * 0.6f, line)
        c.restore()
    }

    /** Claude's spark: a soft orange asterisk, slowly turning. */
    private fun spark(c: Canvas, x: Float, y: Float, rad: Float, deg: Float) {
        p.color = 0x55D97757
        c.drawCircle(x, y, rad * 1.4f, p)
        c.save()
        c.rotate(deg, x, y)
        line.color = 0xFFD97757.toInt()
        line.strokeWidth = rad * 0.36f
        for (i in 0 until 6) {
            val a = i * PI / 3
            val ex = x + (cos(a) * rad).toFloat()
            val ey = y + (sin(a) * rad).toFloat()
            c.drawLine(x, y, ex, ey, line)
        }
        c.restore()
        p.color = 0xFFFFE6D6.toInt()
        c.drawCircle(x, y, rad * 0.22f, p)
    }
}
