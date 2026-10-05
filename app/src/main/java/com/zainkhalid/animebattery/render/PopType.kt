package com.zainkhalid.animebattery.render

import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import com.zainkhalid.animebattery.R

/** The app's variable fonts as plain Typefaces, for the overlay and other Canvas drawing. */
object PopType {
    private val cache = HashMap<Int, Typeface>()

    /** Nunito at [weight] (700, 800 or 900 look best). */
    fun nunito(res: Resources, weight: Int = 800): Typeface = cache.getOrPut(weight) {
        runCatching {
            val font = Font.Builder(res, R.font.nunito).setWeight(weight)
                .setFontVariationSettings("'wght' $weight").build()
            Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).build()
        }.getOrElse { Typeface.create(Typeface.DEFAULT, weight, false) }
    }
}
