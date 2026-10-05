package com.zainkhalid.animebattery.decor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mood effects drawn round the character sticker, plus the manga speech bubble.
 * Plain Canvas, every object is a field, so a frame allocates nothing.
 *
 * Coordinates: the sticker's box is [left], [top], [w] x [h] in px. [t] is a 0..1
 * phase that loops (smooth motion), [step] a slow counter (twinkle).
 */
class MoodPainter(private val d: Float, typeface: Typeface) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface }
    private val path = Path()
    private val r = RectF()
    private var glowKey = 0f

    /** Moods whose effect moves on its own and needs the smooth motion clock. */
    fun moves(m: Mood) = m == Mood.Tired || m == Mood.Fainting || m == Mood.Sleeping || m == Mood.Overheating || m == Mood.Hurt

    /** Moods that only twinkle on the slow step clock. */
    fun twinkles(m: Mood) = m == Mood.Hyped || m == Mood.FullPower || m == Mood.PoweringUp || m == Mood.Hurt

    /** Behind the sticker. */
    fun behind(c: Canvas, m: Mood, left: Float, top: Float, w: Float, h: Float, step: Int) {
        if (m != Mood.FullPower && m != Mood.PoweringUp) return
        val cx = left + w / 2f
        val cy = top + h * 0.55f
        val rad = maxOf(w, h) * (if (step % 2 == 0) 0.62f else 0.56f)
        val key = rad + m.ordinal * 10_000f
        if (glowKey != key) {
            val col = if (m == Mood.FullPower) 0xFFFFD23F.toInt() else 0xFFC6FF3D.toInt()
            glow.shader = RadialGradient(0f, 0f, rad, intArrayOf(col and 0x66FFFFFF, col and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            glowKey = key
        }
        c.save()
        c.translate(cx, cy)
        c.drawCircle(0f, 0f, rad, glow)
        c.restore()
    }

    /** In front of the sticker. */
    fun front(c: Canvas, m: Mood, left: Float, top: Float, w: Float, h: Float, t: Float, step: Int) {
        val cx = left + w / 2f
        when (m) {
            Mood.Hyped, Mood.FullPower -> {
                val col = if (m == Mood.FullPower) 0xFFFFF3B0.toInt() else 0xFFFFD23F.toInt()
                for (i in 0 until 3) {
                    val bright = (i + step) % 2 == 0
                    val a = (-150f + i * 60f) * PI.toFloat() / 180f
                    val sx = cx + cos(a) * w * 0.62f
                    val sy = top + h * 0.32f + sin(a) * h * 0.42f
                    sparkle(c, sx, sy, (if (bright) 3.6f else 2.2f) * d * (h / (40f * d)).coerceIn(0.7f, 1.6f), col)
                }
            }
            Mood.PoweringUp -> {
                // Little lime bolts flicking on and off round the body.
                val s = h * 0.16f
                if (step % 2 == 0) bolt(c, left - s * 0.3f, top + h * 0.35f, s) else bolt(c, left + w + s * 0.3f, top + h * 0.5f, s)
            }
            Mood.Tired -> {
                // A sweat drop slides down beside the head and starts again.
                val k = (t * 2f) % 1f
                val dx = left + w * 0.86f
                val dy = top + h * (0.12f + 0.18f * k)
                drop(c, dx, dy, h * 0.085f, (255 * (1f - k * 0.6f)).toInt())
            }
            Mood.Fainting -> {
                // Stars circling the head.
                val ry = h * 0.07f
                val rx = w * 0.42f
                val cy = top + h * 0.04f
                for (i in 0 until 3) {
                    val a = 2f * PI.toFloat() * (t + i / 3f)
                    val sx = cx + cos(a) * rx
                    val sy = cy + sin(a) * ry
                    star(c, sx, sy, h * 0.07f * (0.8f + 0.2f * sin(a)))
                }
            }
            Mood.Sleeping -> {
                text.color = 0xFFE9E2FF.toInt()
                for (i in 0 until 3) {
                    val k = (t + i / 3f) % 1f
                    text.textSize = h * (0.16f + 0.1f * k)
                    text.alpha = (255 * (1f - k)).toInt()
                    c.drawText("z", left + w * (0.78f + 0.28f * k), top + h * (0.18f - 0.32f * k), text)
                }
                text.alpha = 255
            }
            Mood.Overheating -> {
                // Steam puffs rising off the head.
                for (i in 0 until 3) {
                    val k = (t + i / 3f) % 1f
                    p.color = Color.argb((170 * (1f - k)).toInt(), 255, 255, 255)
                    val px = cx + (i - 1) * w * 0.28f + sin(2 * PI.toFloat() * k) * w * 0.05f
                    c.drawCircle(px, top - h * 0.02f - k * h * 0.28f, h * (0.05f + 0.05f * k), p)
                }
            }
            Mood.Hurt -> {
                // A comic "bonk" burst on top of the head, a bump, and stars going round.
                burst(c, cx + w * 0.12f, top + h * 0.02f, h * 0.2f, step)
                p.color = 0xFFFF8FA0.toInt()
                c.drawCircle(cx - w * 0.05f, top + h * 0.06f, h * 0.06f, p)
                line.color = 0xFF0D0A14.toInt(); line.strokeWidth = 0.9f * d
                c.drawCircle(cx - w * 0.05f, top + h * 0.06f, h * 0.06f, line)
                for (i in 0 until 3) {
                    val a = 2f * PI.toFloat() * (t * 1.6f + i / 3f)
                    star(c, cx + cos(a) * w * 0.4f, top - h * 0.02f + sin(a) * h * 0.06f, h * 0.06f)
                }
            }
            Mood.Chill, Mood.Meh -> Unit
        }
    }

    // ── Speech bubble ─────────────────────────────────────────────────────

    private val bubbleTextSize get() = 12.5f * d
    private val padX get() = 10f * d
    private val padY get() = 6f * d
    private val tail get() = 7f * d

    /** Total height the bubble takes below its anchor, tail included. */
    fun bubbleHeight(): Float {
        text.textSize = bubbleTextSize
        return tail + padY * 2 + (text.descent() - text.ascent()) + 3f * d
    }

    /**
     * A manga bubble under a point at ([anchorX], [anchorY]) with its tail pointing up.
     * Kept inside 0..[maxX]. [pop] 0..1 scales it in from the tail.
     */
    fun bubble(c: Canvas, msg: String, anchorX: Float, anchorY: Float, maxX: Float, pop: Float) {
        if (pop <= 0f) return
        text.textSize = bubbleTextSize
        text.color = 0xFF0D0A14.toInt()
        val tw = text.measureText(msg)
        val bw = tw + padX * 2
        val bh = padY * 2 + (text.descent() - text.ascent())
        val margin = 8f * d
        val bl = (anchorX - bw / 2f).coerceIn(margin, (maxX - margin - bw).coerceAtLeast(margin))
        val bt = anchorY + tail
        c.save()
        c.scale(pop, pop, anchorX, anchorY)
        // Ink shadow, then the white bubble with an ink outline.
        p.color = 0xFF0D0A14.toInt()
        r.set(bl + 2f * d, bt + 2.5f * d, bl + bw + 2f * d, bt + bh + 2.5f * d)
        c.drawRoundRect(r, bh / 2f, bh / 2f, p)
        path.reset()
        val tx = anchorX.coerceIn(bl + bh / 2f, bl + bw - bh / 2f)
        path.moveTo(tx - 5f * d, bt + 1f * d)
        path.lineTo(anchorX, anchorY + 1f * d)
        path.lineTo(tx + 4f * d, bt + 1f * d)
        path.close()
        r.set(bl, bt, bl + bw, bt + bh)
        path.addRoundRect(r, bh / 2f, bh / 2f, Path.Direction.CW)
        p.color = Color.WHITE
        c.drawPath(path, p)
        line.color = 0xFF0D0A14.toInt()
        line.strokeWidth = 1.6f * d
        c.drawRoundRect(r, bh / 2f, bh / 2f, line)
        c.drawLine(tx - 5f * d, bt, anchorX, anchorY + 1f * d, line)
        c.drawLine(anchorX, anchorY + 1f * d, tx + 4f * d, bt, line)
        // Hide the outline segment under the tail opening.
        line.color = Color.WHITE
        line.strokeWidth = 2.2f * d
        c.drawLine(tx - 3.6f * d, bt + 0.4f * d, tx + 2.6f * d, bt + 0.4f * d, line)
        c.drawText(msg, bl + padX, bt + padY - text.ascent(), text)
        c.restore()
    }

    // ── Shapes ────────────────────────────────────────────────────────────

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

    /** Spiky comic impact shape, flickering a little between two sizes. */
    private fun burst(c: Canvas, x: Float, y: Float, rad: Float, step: Int) {
        val r = rad * (if (step % 2 == 0) 1f else 0.85f)
        path.reset()
        for (i in 0 until 16) {
            val a = i * PI / 8
            val rr = if (i % 2 == 0) r else r * 0.5f
            val px = x + (cos(a) * rr).toFloat()
            val py = y + (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        p.color = 0xFFFFF3B0.toInt()
        c.drawPath(path, p)
        line.color = 0xFF0D0A14.toInt(); line.strokeWidth = 1.1f * d
        c.drawPath(path, line)
    }

    private fun bolt(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        path.moveTo(x + s * 0.15f, y - s)
        path.lineTo(x - s * 0.45f, y + s * 0.1f)
        path.lineTo(x - s * 0.02f, y + s * 0.1f)
        path.lineTo(x - s * 0.15f, y + s)
        path.lineTo(x + s * 0.45f, y - s * 0.15f)
        path.lineTo(x + s * 0.02f, y - s * 0.15f)
        path.close()
        p.color = 0xFFC6FF3D.toInt()
        c.drawPath(path, p)
        line.color = 0xFF0D0A14.toInt(); line.strokeWidth = 1f * d
        c.drawPath(path, line)
    }

    private fun drop(c: Canvas, x: Float, y: Float, rad: Float, alpha: Int) {
        path.reset()
        path.moveTo(x, y - rad * 1.8f)
        path.cubicTo(x + rad * 0.4f, y - rad * 0.9f, x + rad, y - rad * 0.2f, x + rad, y + rad * 0.3f)
        r.set(x - rad, y - rad * 0.7f, x + rad, y + rad * 1.3f)
        path.arcTo(r, 0f, 180f)
        path.cubicTo(x - rad, y - rad * 0.2f, x - rad * 0.4f, y - rad * 0.9f, x, y - rad * 1.8f)
        path.close()
        p.color = Color.argb(alpha, 120, 205, 255)
        c.drawPath(path, p)
        line.color = Color.argb(alpha, 13, 10, 20); line.strokeWidth = 1f * d
        c.drawPath(path, line)
        p.color = Color.argb(alpha, 255, 255, 255)
        c.drawCircle(x - rad * 0.35f, y + rad * 0.1f, rad * 0.25f, p)
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
        p.color = 0xFFFFD23F.toInt()
        c.drawPath(path, p)
        line.color = 0xFF0D0A14.toInt(); line.strokeWidth = 0.9f * d
        c.drawPath(path, line)
    }
}
