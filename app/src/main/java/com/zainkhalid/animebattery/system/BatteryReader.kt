package com.zainkhalid.animebattery.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.zainkhalid.animebattery.battery.BatterySnapshot
import kotlin.math.abs

/**
 * Reads the battery for everything in the app (overlay, widget, app screens).
 *
 * The ACTION_BATTERY_CHANGED broadcast can freeze: after `adb shell dumpsys battery
 * unplug` (or `set level ...`) Android stops updating it, and the stock status bar,
 * low-battery warning and auto-shutdown all keep the old number until
 * `dumpsys battery reset`. BatteryManager's CAPACITY property asks the battery
 * hardware directly, so when the two disagree we trust the hardware and say the
 * system reading is [Reading.stuck].
 */
object BatteryReader {

    data class Reading(
        val snapshot: BatterySnapshot,
        /** Android's own battery reading is frozen (left over from a dumpsys battery test). */
        val stuck: Boolean,
        /** What Android's frozen reading claims, for the warning. */
        val systemLevel: Int,
    )

    fun sticky(context: Context): Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    fun read(context: Context, intent: Intent? = sticky(context)): Reading {
        val raw = intent?.let {
            val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            if (l < 0) -1 else l * 100 / s
        } ?: -1
        val rawPlugged = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val rawFull = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL

        val bm = context.getSystemService(BatteryManager::class.java)
        val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        val halStatus = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1

        // A point or two apart is just rounding/timing; further means the broadcast is frozen.
        val stuck = capacity != null && raw >= 0 && abs(capacity - raw) >= STUCK_GAP
        val level = when {
            stuck -> capacity!!
            raw >= 0 -> raw
            else -> capacity ?: 50
        }
        // When frozen, "plugged" is frozen too (unplug fakes it), so ask the hardware.
        val plugged = if (stuck && halStatus > BatteryManager.BATTERY_STATUS_UNKNOWN) {
            halStatus == BatteryManager.BATTERY_STATUS_CHARGING || halStatus == BatteryManager.BATTERY_STATUS_FULL
        } else rawPlugged
        val full = if (stuck) halStatus == BatteryManager.BATTERY_STATUS_FULL else rawFull

        return Reading(
            BatterySnapshot(
                level = level,
                plugged = plugged,
                full = full,
                temperatureC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300) / 10f,
                powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true,
            ),
            stuck = stuck,
            systemLevel = raw,
        )
    }

    private const val STUCK_GAP = 3
}
