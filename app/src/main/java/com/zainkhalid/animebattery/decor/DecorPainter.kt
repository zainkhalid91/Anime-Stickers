package com.zainkhalid.animebattery.decor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the decorations with plain Canvas calls. Everything is a field, so drawing
 * allocates nothing. [step] is a slow animation counter (twinkle, float), [t] a
 * 0..1 phase for smooth motion.
 */
class DecorPainter(private val d: Float) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val r = RectF()

    /** Base size of a decoration at scale 1, in px. */
    val base = 16f * d

    fun draw(c: Canvas, type: DecorType, cx: Float, cy: Float, scale: Float, step: Int, t: Float, barTop: Float = 0f) {
        val s = base * scale
        when (type) {
            DecorType.Sparkle -> sparkle(c, cx, cy, s * (if (step % 2 == 0) 0.55f else 0.38f), 0xFFFFE27A.toInt())
            DecorType.Star -> star(c, cx, cy, s * 0.5f)
            DecorType.Heart -> heart(c, cx, cy + sin(2 * PI * t).toFloat() * 0.6f * d, s * 0.5f)
            DecorType.Spider -> spider(c, cx, cy, s * 0.42f, barTop, t)
            DecorType.Web -> web(c, cx, s * 2.4f)
            DecorType.Ghost -> ghost(c, cx, cy + sin(2 * PI * t).toFloat() * 1.5f * d, s * 0.55f)
            DecorType.Cloud -> cloud(c, cx, cy, s * 0.6f)
            DecorType.Leaf -> leaf(c, cx, cy, s * 0.55f, sin(2 * PI * t).toFloat() * 12f)
            DecorType.Wings -> Unit // drawn round the battery icon by the bar
        }
    }

    fun sparkle(c: Canvas, x: Float, y: Float, size: Float, color: Int) {
        p.color = color
        path.reset()
        path.moveTo(x, y - size)
        path.quadTo(x, y, x + size, y)
        path.quadTo(x, y, x, y + size)
        path.quadTo(x, y, x - size, y)
        path.quadTo(x, y, x, y - size)
        path.close()
        c.drawPath(path, p)
    }

    private fun star(c: Canvas, x: Float, y: Float, rad: Float) {
        path.reset()
        for (i in 0 until 10) {
            val a = -PI / 2 + i * PI / 5
            val rr = if (i % 2 == 0) rad else rad * 0.45f
            val px = x + (cos(a) * rr).toFloat()
            val py = y + (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        p.color = 0xFFFFD54A.toInt()
        c.drawPath(path, p)
        line.color = 0xFF8A5A00.toInt(); line.strokeWidth = 1.2f * d
        c.drawPath(path, line)
    }

    private fun heart(c: Canvas, x: Float, y: Float, rad: Float) {
        path.reset()
        path.moveTo(x, y + rad)
        path.cubicTo(x - rad * 1.7f, y - rad * 0.2f, x - rad * 0.8f, y - rad * 1.4f, x, y - rad * 0.45f)
        path.cubicTo(x + rad * 0.8f, y - rad * 1.4f, x + rad * 1.7f, y - rad * 0.2f, x, y + rad)
        path.close()
        p.color = 0xFFFF6FA0.toInt()
        c.drawPath(path, p)
        p.color = Color.WHITE
        c.drawCircle(x - rad * 0.55f, y - rad * 0.35f, rad * 0.18f, p)
    }

    private fun spider(c: Canvas, x: Float, y: Float, rad: Float, barTop: Float, t: Float) {
        // Bobs on its thread.
        val by = y + sin(2 * PI * t).toFloat() * 2f * d
        line.color = 0xCCFFFFFF.toInt(); line.strokeWidth = 1f * d
        c.drawLine(x, barTop, x, by - rad, line)
        line.color = 0xFF111111.toInt(); line.strokeWidth = 1.6f * d
        for (side in intArrayOf(-1, 1)) for (i in 0 until 4) {
            val a = (-50f + i * 32f) * PI.toFloat() / 180f
            val kx = x + side * rad * 1.1f
            val ky = by + rad * 0.2f * (i - 1.5f)
            val ex = x + side * rad * (1.9f + 0.2f * cos(a))
            val ey = ky + rad * 0.9f * sin(a)
            c.drawLine(x, by, kx, ky - rad * 0.5f, line)
            c.drawLine(kx, ky - rad * 0.5f, ex, ey, line)
        }
        p.color = 0xFF1A1A1A.toInt()
        c.drawCircle(x, by, rad, p)
        c.drawCircle(x, by - rad * 0.9f, rad * 0.62f, p)
        p.color = Color.WHITE
        c.drawCircle(x - rad * 0.25f, by - rad, rad * 0.22f, p)
        c.drawCircle(x + rad * 0.25f, by - rad, rad * 0.22f, p)
        p.color = Color.BLACK
        c.drawCircle(x - rad * 0.25f, by - rad * 0.95f, rad * 0.1f, p)
        c.drawCircle(x + rad * 0.25f, by - rad * 0.95f, rad * 0.1f, p)
    }

    /** Web hanging from the nearest top corner. [x] is in px; left half = left corner. */
    private fun web(c: Canvas, x: Float, size: Float) {
        val left = x < c.width / 2f
        val ox = if (left) 0f else c.width.toFloat()
        val dir = if (left) 1f else -1f
        line.color = 0x99FFFFFF.toInt(); line.strokeWidth = 1f * d
        for (i in 0..4) {
            val a = (i * 22.5f) * PI.toFloat() / 180f
            c.drawLine(ox, 0f, ox + dir * cos(a) * size, sin(a) * size, line)
        }
        for (ring in 1..3) {
            val rr = size * ring / 3.4f
            path.reset()
            for (i in 0..4) {
                val a = (i * 22.5f) * PI.toFloat() / 180f
                val px = ox + dir * cos(a) * rr
                val py = sin(a) * rr
                if (i == 0) path.moveTo(px, py) else path.quadTo(ox + dir * cos(a - 0.2f) * rr * 0.86f, sin(a - 0.2f) * rr * 0.86f, px, py)
            }
            c.drawPath(path, line)
        }
    }

    private fun ghost(c: Canvas, x: Float, y: Float, rad: Float) {
        path.reset()
        path.moveTo(x - rad, y + rad)
        path.lineTo(x - rad, y)
        path.cubicTo(x - rad, y - rad * 1.4f, x + rad, y - rad * 1.4f, x + rad, y)
        path.lineTo(x + rad, y + rad)
        for (i in 0 until 3) {
            val x0 = x + rad - i * rad * 2 / 3f
            path.quadTo(x0 - rad / 6f, y + rad * 0.7f, x0 - rad / 3f, y + rad)
            path.quadTo(x0 - rad / 2f, y + rad * 1.3f, x0 - rad * 2 / 3f, y + rad)
        }
        path.close()
        p.color = 0xF2FFFFFF.toInt()
        c.drawPath(path, p)
        p.color = 0xFF1E1B4B.toInt()
        r.set(x - rad * 0.5f, y - rad * 0.35f, x - rad * 0.15f, y + rad * 0.1f); c.drawOval(r, p)
        r.set(x + rad * 0.15f, y - rad * 0.35f, x + rad * 0.5f, y + rad * 0.1f); c.drawOval(r, p)
    }

    private fun cloud(c: Canvas, x: Float, y: Float, rad: Float) {
        p.color = 0xF2FFFFFF.toInt()
        c.drawCircle(x - rad * 0.7f, y + rad * 0.15f, rad * 0.55f, p)
        c.drawCircle(x, y - rad * 0.15f, rad * 0.75f, p)
        c.drawCircle(x + rad * 0.75f, y + rad * 0.2f, rad * 0.5f, p)
        r.set(x - rad * 1.2f, y + rad * 0.1f, x + rad * 1.2f, y + rad * 0.7f)
        c.drawRoundRect(r, rad * 0.3f, rad * 0.3f, p)
    }

    private fun leaf(c: Canvas, x: Float, y: Float, rad: Float, tilt: Float) {
        c.save()
        c.rotate(-30f + tilt, x, y)
        path.reset()
        path.moveTo(x - rad, y)
        path.quadTo(x, y - rad * 0.9f, x + rad, y)
        path.quadTo(x, y + rad * 0.9f, x - rad, y)
        path.close()
        p.color = 0xFF4CC26B.toInt()
        c.drawPath(path, p)
        line.color = 0xFF2E7D45.toInt(); line.strokeWidth = 1.1f * d
        c.drawLine(x - rad * 0.9f, y, x + rad * 0.8f, y, line)
        c.restore()
    }

    /** Little angel wings either side of a battery icon at [battery]. */
    fun wings(c: Canvas, battery: RectF, flap: Float) {
        val h = battery.height() * 0.85f
        for (side in intArrayOf(-1, 1)) {
            val ax = if (side < 0) battery.left - 1f * d else battery.right + 1f * d
            val ay = battery.centerY()
            c.save()
            c.rotate(side * (8f + flap * 10f), ax, ay)
            path.reset()
            path.moveTo(ax, ay)
            path.cubicTo(ax + side * h * 0.3f, ay - h * 0.9f, ax + side * h * 1.1f, ay - h * 0.7f, ax + side * h * 1.05f, ay - h * 0.2f)
            path.cubicTo(ax + side * h * 0.8f, ay - h * 0.1f, ax + side * h * 0.85f, ay + h * 0.15f, ax + side * h * 0.55f, ay + h * 0.15f)
            path.cubicTo(ax + side * h * 0.4f, ay + h * 0.35f, ax + side * h * 0.15f, ay + h * 0.3f, ax, ay)
            path.close()
            p.color = Color.WHITE
            c.drawPath(path, p)
            line.color = 0x55A0AEC0; line.strokeWidth = 1f * d
            c.drawPath(path, line)
            c.restore()
        }
    }

    /**
     * Big anime eyes either side of the camera. [look] -1..1 moves the pupils sideways,
     * [shut] 0..1 closes the lids (blink, sleepy at low battery). [lid] is the bar colour.
     */
    fun eyes(c: Canvas, camX: Float, camY: Float, rad: Float, gap: Float, look: Float, shut: Float, lid: Int) {
        for (side in intArrayOf(-1, 1)) {
            val ex = camX + side * gap
            r.set(ex - rad * 0.8f, camY - rad, ex + rad * 0.8f, camY + rad)
            p.color = Color.WHITE
            c.drawOval(r, p)
            // Iris and pupil, clipped to the eye.
            c.save()
            path.reset(); path.addOval(r, Path.Direction.CW); c.clipPath(path)
            val ix = ex + look * rad * 0.3f
            p.color = 0xFF2E7FE6.toInt(); c.drawCircle(ix, camY + rad * 0.15f, rad * 0.62f, p)
            p.color = 0xFF0E1A3C.toInt(); c.drawCircle(ix, camY + rad * 0.2f, rad * 0.32f, p)
            p.color = Color.WHITE; c.drawCircle(ix - rad * 0.22f, camY - rad * 0.12f, rad * 0.16f, p)
            if (shut > 0f) {
                p.color = lid
                c.drawRect(r.left, r.top, r.right, r.top + r.height() * shut, p)
            }
            c.restore()
            line.color = 0xFF111111.toInt(); line.strokeWidth = 1.6f * d
            c.drawOval(r, line)
            // Lash line along the lid edge, only once the lid is coming down.
            if (shut > 0.05f) {
                line.strokeWidth = 2.2f * d
                val ly = r.top + r.height() * shut
                c.drawLine(r.left - rad * 0.1f, ly, r.right + rad * 0.1f, ly, line)
            }
        }
    }
}
