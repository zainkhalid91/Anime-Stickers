package com.zainkhalid.animebattery.render

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zainkhalid.animebattery.R
import com.zainkhalid.animebattery.cast.Look
import java.io.File
import kotlin.math.roundToInt

/**
 * Sticker art per character and [Look], shrunk to the exact pixel height it's drawn at.
 *
 * Built-in art is res/drawable-nodpi/sticker_<id>_<look>.png; characters you made
 * yourself live in files/cast/<id>/<look>.png. A missing look falls back to idle.
 *
 * Going from ~750px to ~46px in one step aliases badly, so it halves first (each
 * halving with filtering), then does one last filtered resize. Done once per size.
 */
object StickerCache {
    private val cache = HashMap<String, ImageBitmap>()
    private val resIds = HashMap<String, Int>()

    /** Drawable for a built-in sticker, or 0. Looked up by name so new art needs no code. */
    @SuppressLint("DiscouragedApi")
    fun res(context: Context, characterId: String, look: Look = Look.Idle): Int {
        val key = "$characterId/${look.key}"
        resIds[key]?.let { return it }
        val r = context.resources
        var id = r.getIdentifier("sticker_${characterId}_${look.key}", "drawable", context.packageName)
        if (id == 0 && look != Look.Idle) id = res(context, characterId, Look.Idle)
        if (id == 0 && characterId == "naruto") id = R.drawable.sticker_naruto_happy
        resIds[key] = id
        return id
    }

    /** A sticker you made, if there is one. */
    fun file(context: Context, characterId: String, look: Look = Look.Idle): File? {
        val dir = File(context.filesDir, "cast/$characterId")
        return File(dir, "${look.key}.png").takeIf { it.exists() } ?: File(dir, "idle.png").takeIf { it.exists() }
    }

    /** True if [look] has its own art (not just the idle fallback). */
    fun hasOwn(context: Context, characterId: String, look: Look): Boolean =
        File(context.filesDir, "cast/$characterId/${look.key}.png").exists() ||
            res(context, characterId, look).let { it != 0 && (look == Look.Idle || it != res(context, characterId, Look.Idle)) }

    fun get(context: Context, characterId: String, heightPx: Int): ImageBitmap? = get(context, characterId, Look.Idle, heightPx)

    fun get(context: Context, characterId: String, look: Look, heightPx: Int): ImageBitmap? {
        if (heightPx <= 0) return null
        val key = "$characterId/${look.key}@$heightPx"
        cache[key]?.let { return it }
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val file = file(context, characterId, look)
        var bmp = when {
            file != null -> BitmapFactory.decodeFile(file.path, opts)
            else -> res(context, characterId, look).takeIf { it != 0 }?.let { BitmapFactory.decodeResource(context.resources, it, opts) }
        } ?: return null
        while (bmp.height / 2 >= heightPx * 2) {
            val half = Bitmap.createScaledBitmap(bmp, bmp.width / 2, bmp.height / 2, true)
            if (half !== bmp) bmp.recycle()
            bmp = half
        }
        val w = (bmp.width * heightPx / bmp.height.toFloat()).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(bmp, w, heightPx, true)
        if (out !== bmp) bmp.recycle()
        // Keep only the latest size per character and look.
        cache.keys.removeAll { it.startsWith("$characterId/${look.key}@") }
        return out.asImageBitmap().also { cache[key] = it }
    }

    /** Forget a character's art (after you remake it). */
    fun forget(characterId: String) {
        cache.keys.removeAll { it.startsWith("$characterId/") }
        resIds.keys.removeAll { it.startsWith("$characterId/") }
    }
}
