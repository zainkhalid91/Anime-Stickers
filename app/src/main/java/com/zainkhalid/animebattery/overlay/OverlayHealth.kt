package com.zainkhalid.animebattery.overlay

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.delay

/**
 * Is the overlay actually running?
 *
 * The service lives in the app's process, so if the app crashes the service dies
 * with it, and Android marks it as crashed: it stays switched on in Settings (maybe
 * with "Not working") but is never bound again until it's switched off and on. That's
 * [Status.Stopped]. With WRITE_SECURE_SETTINGS (granted once over adb) we can do the
 * off/on ourselves, see [restart].
 */
object OverlayHealth {

    enum class Status { Off, Running, Stopped }

    private fun component(context: Context) = ComponentName(context, StatusOverlayService::class.java)

    /** Switched on in Accessibility settings. */
    fun enabledInSettings(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = component(context)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** Bound by the system right now. Crashed services drop out of this list. */
    fun bound(context: Context): Boolean {
        if (StatusOverlayService.instance != null) return true
        val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val me = component(context)
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo?.serviceInfo?.let { s -> ComponentName(s.packageName, s.name) } == me }
    }

    fun status(context: Context): Status = when {
        !enabledInSettings(context) -> Status.Off
        bound(context) -> Status.Running
        else -> Status.Stopped
    }

    fun canRestart(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /**
     * Switches the service off and on again through the secure setting, which clears
     * Android's "crashed" mark. Returns true if it came back.
     */
    suspend fun restart(context: Context): Boolean {
        if (!canRestart(context)) return false
        val resolver = context.contentResolver
        val key = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        val me = component(context)
        val others = (Settings.Secure.getString(resolver, key) ?: "").split(':')
            .filter { it.isNotBlank() && ComponentName.unflattenFromString(it) != me }
        return try {
            Settings.Secure.putString(resolver, key, others.joinToString(":").ifEmpty { null })
            delay(700) // let the system notice it's off before turning it back on
            Settings.Secure.putString(resolver, key, (others + me.flattenToString()).joinToString(":"))
            Settings.Secure.putString(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
            var tries = 0
            while (!bound(context) && tries++ < 10) delay(200)
            bound(context).also { Log.i(TAG, "overlay restart -> $it") }
        } catch (e: SecurityException) {
            Log.e(TAG, "overlay restart refused", e)
            false
        }
    }

    private const val TAG = "AnimeBattery"
}
