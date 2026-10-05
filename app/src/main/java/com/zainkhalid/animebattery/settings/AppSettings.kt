package com.zainkhalid.animebattery.settings

import android.content.Context
import android.content.SharedPreferences
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.decor.Pose

/**
 * User settings, shared by the app UI and the overlay service (same process).
 * Plain SharedPreferences so the service can listen for changes.
 */
class AppSettings(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var characterId: String
        get() = prefs.getString(CHARACTER, "naruto")!!
        set(v) = prefs.edit().putString(CHARACTER, v).apply()

    var showPercent: Boolean
        get() = prefs.getBoolean(SHOW_PERCENT, true)
        set(v) = prefs.edit().putBoolean(SHOW_PERCENT, v).apply()

    /** 0.90 to 1.15. */
    var size: Float
        get() = prefs.getFloat(SIZE, 1f)
        set(v) = prefs.edit().putFloat(SIZE, v.coerceIn(0.9f, 1.15f)).apply()

    /** -8 to 8 dp, moves the figure only. */
    var nudgeDp: Float
        get() = prefs.getFloat(NUDGE, 0f)
        set(v) = prefs.edit().putFloat(NUDGE, v.coerceIn(-8f, 8f)).apply()

    var hideOnLockScreen: Boolean
        get() = prefs.getBoolean(HIDE_ON_LOCK, true)
        set(v) = prefs.edit().putBoolean(HIDE_ON_LOCK, v).apply()

    var animations: Boolean
        get() = prefs.getBoolean(ANIMATIONS, true)
        set(v) = prefs.edit().putBoolean(ANIMATIONS, v).apply()

    /** Overlay hidden but the service stays on. */
    var paused: Boolean
        get() = prefs.getBoolean(PAUSED, false)
        set(v) = prefs.edit().putBoolean(PAUSED, v).apply()

    /** "badge" = sit on the stock battery, "full" = draw our own whole status bar. */
    var barMode: String
        get() = prefs.getString(BAR_MODE, MODE_FULL)!!
        set(v) = prefs.edit().putString(BAR_MODE, v).apply()

    /** Character size in the full bar, 24 to 56 dp. */
    var characterSizeDp: Float
        get() = prefs.getFloat(CHARACTER_SIZE, 40f)
        set(v) = prefs.edit().putFloat(CHARACTER_SIZE, v.coerceIn(24f, 56f)).apply()

    /** Where the character lives in the full bar: "battery" or "camera" (older values map to camera). */
    var characterSpot: String
        get() = when (val v = prefs.getString(SPOT, SPOT_BATTERY)) {
            SPOT_BATTERY, SPOT_CAMERA -> v
            else -> SPOT_CAMERA
        }
        set(v) = prefs.edit().putString(SPOT, v).apply()

    var sparkles: Boolean
        get() = prefs.getBoolean(SPARKLES, true)
        set(v) = prefs.edit().putBoolean(SPARKLES, v).apply()

    /** The custom bar: pose, size, sway and placed decorations. */
    var barLayout: BarLayout
        get() = BarLayout.fromJson(prefs.getString(LAYOUT, null))
            ?: BarLayout(characterSizeDp = characterSizeDp, pose = if (characterSpot == SPOT_CAMERA) Pose.Camera else Pose.Hanging)
        set(v) = prefs.edit().putString(LAYOUT, v.toJson()).apply()

    /** The character says a line when its mood changes and when you unlock. */
    var speech: Boolean
        get() = prefs.getBoolean(SPEECH, true)
        set(v) = prefs.edit().putBoolean(SPEECH, v).apply()

    /** You can grab, flick and poke the hanging character (blocks taps right under it). */
    var touchBuddy: Boolean
        get() = prefs.getBoolean(TOUCH, true)
        set(v) = prefs.edit().putBoolean(TOUCH, v).apply()

    /** Id of the collection last applied, for the "Applied" badge. */
    var themeId: String?
        get() = prefs.getString(THEME, null)
        set(v) = prefs.edit().putString(THEME, v).apply()

    companion object {
        const val LAYOUT = "bar_layout"
        const val THEME = "theme_id"
        const val SPOT = "character_spot"
        const val SPOT_BATTERY = "battery"
        const val SPOT_CAMERA = "camera"
        const val SPARKLES = "sparkles"
        const val BAR_MODE = "bar_mode"
        const val MODE_BADGE = "badge"
        const val MODE_FULL = "full"
        const val CHARACTER_SIZE = "character_size_dp"
        const val CHARACTER = "character"
        const val SHOW_PERCENT = "show_percent"
        const val SIZE = "size"
        const val NUDGE = "nudge_dp"
        const val HIDE_ON_LOCK = "hide_on_lock"
        const val ANIMATIONS = "animations"
        const val PAUSED = "paused"
        const val SPEECH = "speech"
        const val TOUCH = "touch_buddy"
    }
}
