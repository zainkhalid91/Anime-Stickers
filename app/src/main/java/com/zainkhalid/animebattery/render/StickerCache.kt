package com.zainkhalid.animebattery.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zainkhalid.animebattery.R
import kotlin.math.roundToInt

/**
 * Sticker art per character, shrunk to the exact pixel height it's drawn at.
 *
 * Going from ~750px to ~46px in one step aliases badly, so it halves first (each
 * halving with filtering), then does one last filtered resize. Done once per size.
 */
object StickerCache {
    private val sources = mapOf("naruto" to R.drawable.sticker_naruto_happy)
    private val cache = HashMap<String, ImageBitmap>()

    fun has(characterId: String) = characterId in sources

    fun get(context: Context, characterId: String, heightPx: Int): ImageBitmap? {
        val res = sources[characterId] ?: return null
        if (heightPx <= 0) return null
        val key = "$characterId@$heightPx"
        cache[key]?.let { return it }
        val opts = BitmapFactory.Options().apply { inScaled = false }
        var bmp = BitmapFactory.decodeResource(context.resources, res, opts) ?: return null
        while (bmp.height / 2 >= heightPx * 2) {
            val half = Bitmap.createScaledBitmap(bmp, bmp.width / 2, bmp.height / 2, true)
            if (half !== bmp) bmp.recycle()
            bmp = half
        }
        val w = (bmp.width * heightPx / bmp.height.toFloat()).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(bmp, w, heightPx, true)
        if (out !== bmp) bmp.recycle()
        // Keep only the latest size per character.
        cache.keys.removeAll { it.startsWith("$characterId@") }
        return out.asImageBitmap().also { cache[key] = it }
    }
}
