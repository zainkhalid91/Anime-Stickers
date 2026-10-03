package com.zainkhalid.animebattery.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.asAndroidBitmap
import com.zainkhalid.animebattery.render.StickerCache
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wallpapers drawn around the camera hole, so the lens becomes part of the art.
 * Rendered at the real screen size with the real cutout position.
 */
object WallpaperArt {

    enum class Design(val title: String, val blurb: String) {
        RasenganLens("Rasengan Lens", "The camera is the centre of the Rasengan"),
        LeafNight("Hidden Leaf Night", "The camera is the moon over the village"),
    }

    /** [camX]/[camY]/[camR] are the lens centre and radius in px. */
    fun render(context: Context, design: Design, w: Int, h: Int, camX: Float, camY: Float, camR: Float, characterId: String): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        when (design) {
            Design.RasenganLens -> rasengan(context, c, w.toFloat(), h.toFloat(), camX, camY, camR, characterId)
            Design.LeafNight -> leafNight(context, c, w.toFloat(), h.toFloat(), camX, camY, camR, characterId)
        }
        return bmp
    }

    private fun stickerAt(context: Context, c: Canvas, id: String, centerX: Float, bottom: Float, height: Float) {
        val s = StickerCache.get(context, id, height.roundToInt())?.asAndroidBitmap() ?: return
        c.drawBitmap(s, centerX - s.width / 2f, bottom - s.height, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun rasengan(context: Context, c: Canvas, w: Float, h: Float, cx: Float, cy: Float, r: Float, id: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF071233.toInt(), 0xFF0E1A3A.toInt(), 0xFF1D3A8A.toInt(), 0xFFF97316.toInt()), floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        // Glow, then the orb: the lens sits in the bright core.
        val glowR = w * 0.62f
        p.shader = RadialGradient(cx, cy, glowR, intArrayOf(0x8860C8FF.toInt(), 0x2260C8FF, 0x0060C8FF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, glowR, p)
        val orbR = w * 0.24f
        p.shader = RadialGradient(cx, cy, orbR, intArrayOf(Color.WHITE, 0xFFC8F0FF.toInt(), 0xFF5AB4FF.toInt(), 0xFF2C6FE0.toInt()), floatArrayOf(0f, 0.3f, 0.75f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, orbR, p)
        p.shader = null
        // Swirl arms spiralling out of the lens.
        val arm = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        for (k in 0 until 6) {
            val path = Path()
            val start = k * PI / 3
            for (i in 0..60) {
                val t = i / 60.0
                val a = start + t * PI * 1.6
                val rr = r * 1.6f + (orbR * 1.25f - r * 1.6f) * t.toFloat()
                val x = cx + (cos(a) * rr).toFloat()
                val y = cy + (sin(a) * rr).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            arm.strokeWidth = r * (0.55f - k % 2 * 0.2f)
            arm.color = if (k % 2 == 0) 0xCCFFFFFF.toInt() else 0x998FD8FF.toInt()
            c.drawPath(path, arm)
        }
        // A dark ring right round the lens so it reads as the core.
        p.color = 0xFF0A1A3F.toInt()
        c.drawCircle(cx, cy, r * 1.35f, p)
        p.color = 0xFF9FE0FF.toInt()
        arm.strokeWidth = r * 0.22f; arm.color = 0xFF9FE0FF.toInt()
        c.drawCircle(cx, cy, r * 1.35f, arm)
        // Sparks.
        val rnd = Random(7)
        for (i in 0 until 60) {
            val a = rnd.nextDouble() * 2 * PI
            val d = orbR * (1.1f + rnd.nextFloat() * 1.6f)
            p.color = if (i % 3 == 0) Color.WHITE else 0xFF8FD8FF.toInt()
            p.alpha = 120 + rnd.nextInt(135)
            c.drawCircle(cx + (cos(a) * d).toFloat(), cy + (sin(a) * d).toFloat(), r * (0.08f + rnd.nextFloat() * 0.12f), p)
        }
        p.alpha = 255
        stickerAt(context, c, id, w * 0.5f, h * 0.9f, h * 0.34f)
    }

    private fun leafNight(context: Context, c: Canvas, w: Float, h: Float, cx: Float, cy: Float, r: Float, id: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF0B1030.toInt(), 0xFF27306B.toInt(), 0xFF7A3B6E.toInt(), 0xFFF2A65A.toInt()), floatArrayOf(0f, 0.4f, 0.75f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        // Moon: the lens is a crater on a big pale moon.
        val moonR = w * 0.16f
        p.shader = RadialGradient(cx, cy, moonR * 2.6f, intArrayOf(0x55FFF4D6, 0x00FFF4D6), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, moonR * 2.6f, p)
        p.shader = RadialGradient(cx - moonR * 0.3f, cy - moonR * 0.3f, moonR, intArrayOf(0xFFFFF9E8.toInt(), 0xFFF1DFB0.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, moonR, p)
        p.shader = null
        p.color = 0x33A08A5A
        c.drawCircle(cx + moonR * 0.45f, cy + moonR * 0.35f, moonR * 0.18f, p)
        c.drawCircle(cx - moonR * 0.5f, cy + moonR * 0.45f, moonR * 0.12f, p)
        p.color = 0xFFD9C38E.toInt()
        c.drawCircle(cx, cy, r * 1.4f, p)
        // Stars.
        val rnd = Random(11)
        for (i in 0 until 90) {
            p.color = Color.WHITE
            p.alpha = 80 + rnd.nextInt(175)
            c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.55f, 1f + rnd.nextFloat() * 2.4f, p)
        }
        p.alpha = 255
        // Falling leaves.
        val leaf = Path()
        for (i in 0 until 14) {
            val lx = rnd.nextFloat() * w
            val ly = h * 0.25f + rnd.nextFloat() * h * 0.5f
            val s = w * (0.018f + rnd.nextFloat() * 0.02f)
            c.save(); c.rotate(rnd.nextFloat() * 180f, lx, ly)
            leaf.reset(); leaf.moveTo(lx - s, ly); leaf.quadTo(lx, ly - s, lx + s, ly); leaf.quadTo(lx, ly + s, lx - s, ly); leaf.close()
            p.color = if (i % 2 == 0) 0xFF4CC26B.toInt() else 0xFFE9A23B.toInt()
            c.drawPath(leaf, p); c.restore()
        }
        // Cliff silhouette at the bottom.
        val cliff = Path().apply {
            moveTo(0f, h * 0.86f)
            cubicTo(w * 0.25f, h * 0.8f, w * 0.45f, h * 0.84f, w * 0.62f, h * 0.82f)
            cubicTo(w * 0.8f, h * 0.8f, w * 0.92f, h * 0.86f, w, h * 0.84f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        p.color = 0xFF1A1036.toInt()
        c.drawPath(cliff, p)
        stickerAt(context, c, id, w * 0.62f, h * 0.83f, h * 0.22f)
        p.color = 0x66FFFFFF
        c.drawRoundRect(RectF(w * 0.08f, h * 0.9f, w * 0.3f, h * 0.905f), 4f, 4f, p)
    }
}
