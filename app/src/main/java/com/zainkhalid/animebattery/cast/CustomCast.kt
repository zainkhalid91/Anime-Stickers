package com.zainkhalid.animebattery.cast

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.zainkhalid.animebattery.render.StickerCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A character you made from a photo. Its art is files/cast/<id>/idle.png. */
data class CustomMember(val id: String, val name: String)

/**
 * Make-your-own characters: pick a photo (your pet, your OC, fan art), the subject is
 * cut out on the device with ML Kit, then it gets the same white sticker border as the
 * built-in art. Only an idle look; every other look falls back to it, and the moods
 * are still drawn round it.
 */
object CustomCast {
    private const val KEY = "custom_cast"

    fun list(context: Context): List<CustomMember> {
        val json = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it).let { o -> CustomMember(o.getString("id"), o.getString("name")) } }
        }.getOrDefault(emptyList())
    }

    fun find(context: Context, id: String?): CustomMember? = list(context).firstOrNull { it.id == id }

    /** Saves [sticker] as a new character and returns it. */
    fun add(context: Context, name: String, sticker: Bitmap): CustomMember {
        val id = "my_${System.currentTimeMillis().toString(36)}"
        val dir = File(context.filesDir, "cast/$id").apply { mkdirs() }
        File(dir, "idle.png").outputStream().use { sticker.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val member = CustomMember(id, name.trim().ifEmpty { "My buddy" })
        save(context, list(context) + member)
        return member
    }

    fun remove(context: Context, id: String) {
        File(context.filesDir, "cast/$id").deleteRecursively()
        StickerCache.forget(id)
        save(context, list(context).filterNot { it.id == id })
    }

    private fun save(context: Context, all: List<CustomMember>) {
        val arr = JSONArray()
        all.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name)) }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // ── Making the sticker ────────────────────────────────────────────────

    /** Cuts the subject out of the photo at [uri] and turns it into a sticker. Null if nothing was found. */
    suspend fun makeSticker(context: Context, uri: Uri): Bitmap? {
        val photo = withContext(Dispatchers.IO) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                // ML Kit needs a software bitmap; keep it to a sensible size.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_INPUT) decoder.setTargetSampleSize((longest / MAX_INPUT).coerceAtLeast(1))
            }
        }
        val cut = segment(photo) ?: return null
        return withContext(Dispatchers.Default) { border(crop(cut) ?: return@withContext null) }
    }

    private suspend fun segment(photo: Bitmap): Bitmap? = suspendCancellableCoroutine { cont ->
        val segmenter = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
        segmenter.process(InputImage.fromBitmap(photo, 0))
            .addOnSuccessListener { cont.resume(it.foregroundBitmap); segmenter.close() }
            .addOnFailureListener { cont.resumeWithException(it); segmenter.close() }
    }

    /** Trims to the visible pixels and scales to [HEIGHT] tall. */
    private fun crop(src: Bitmap): Bitmap? {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) {
            if ((px[y * w + x] ushr 24) > 24) {
                if (x < x0) x0 = x; if (x > x1) x1 = x
                if (y < y0) y0 = y; if (y > y1) y1 = y
            }
        }
        if (x1 < 0) return null
        val cw = x1 - x0 + 1
        val ch = y1 - y0 + 1
        val scale = HEIGHT / ch.toFloat()
        val out = Bitmap.createBitmap((cw * scale).roundToInt().coerceAtLeast(1), HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, Rect(x0, y0, x1 + 1, y1 + 1), Rect(0, 0, out.width, out.height), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /**
     * Adds the white sticker border: the silhouette, filled white and stamped in a
     * ring round itself (a cheap dilation), with the subject on top.
     */
    private fun border(src: Bitmap): Bitmap {
        val b = (HEIGHT * 0.022f).roundToInt().coerceAtLeast(3)
        val out = Bitmap.createBitmap(src.width + b * 2 + 4, src.height + b * 2 + 4, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val white = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN) }
        val ox = b + 2f
        val oy = b + 2f
        for (ring in listOf(b.toFloat(), b * 0.6f)) {
            for (i in 0 until 24) {
                val a = i * 2 * PI / 24
                c.drawBitmap(src, ox + (cos(a) * ring).toFloat(), oy + (sin(a) * ring).toFloat(), white)
            }
        }
        c.drawBitmap(src, ox, oy, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private const val MAX_INPUT = 1600
    private const val HEIGHT = 640
}

/** Display name for any character id: built-in, one you made, or a fallback. */
fun castName(context: Context, id: String?): String =
    Cast.find(id)?.name ?: CustomCast.find(context, id)?.name ?: "Buddy"
