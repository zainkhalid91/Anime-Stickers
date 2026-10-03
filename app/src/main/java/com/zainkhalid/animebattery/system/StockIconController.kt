package com.zainkhalid.animebattery.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

/**
 * Hides and restores the stock battery icon through icon_blacklist.
 * Remembers whether "battery" was already there before we first hid it.
 */
class StockIconController(private val context: Context) {

    private val prefs = context.getSharedPreferences("stock_icon", Context.MODE_PRIVATE)

    fun canWrite(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun current(): String? = Settings.Secure.getString(context.contentResolver, IconBlacklist.KEY)

    fun isHidden(): Boolean = IconBlacklist.contains(current())

    /** Returns false if we lack the permission or the write was refused. */
    fun hide(): Boolean {
        if (!canWrite()) return false
        val now = current()
        if (!prefs.getBoolean(KEY_SAVED, false)) {
            prefs.edit()
                .putBoolean(KEY_SAVED, true)
                .putBoolean(KEY_HAD_BATTERY, IconBlacklist.contains(now))
                .apply()
        }
        return write(IconBlacklist.withSlot(now))
    }

    fun restore(): Boolean {
        if (!canWrite()) return false
        val had = prefs.getBoolean(KEY_HAD_BATTERY, false)
        val ok = write(IconBlacklist.restored(current(), had))
        if (ok) prefs.edit().clear().apply()
        return ok
    }

    private fun write(value: String?): Boolean = try {
        Settings.Secure.putString(context.contentResolver, IconBlacklist.KEY, value)
        Log.i(TAG, "icon_blacklist -> $value (read back: ${current()})")
        current() == value
    } catch (e: SecurityException) {
        Log.e(TAG, "write refused", e)
        false
    }

    private companion object {
        const val TAG = "AnimeBattery"
        const val KEY_SAVED = "saved"
        const val KEY_HAD_BATTERY = "had_battery"
    }
}
