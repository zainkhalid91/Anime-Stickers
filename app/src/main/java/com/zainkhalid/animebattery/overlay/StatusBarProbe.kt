package com.zainkhalid.animebattery.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Spike helpers: find the status bar window and the stock battery icon inside it,
 * and dump what SystemUI says about its own sizes.
 */
object StatusBarProbe {
    private const val TAG = "AnimeBattery"
    private const val SYSTEMUI = "com.android.systemui"

    /** Logs every window the service can see. */
    fun dumpWindows(service: AccessibilityService) {
        service.windows.forEach { w ->
            val b = Rect().also(w::getBoundsInScreen)
            Log.i(TAG, "window type=${typeName(w.type)} layer=${w.layer} title=${w.title} " +
                "pkg=${w.root?.packageName} bounds=$b")
        }
    }

    fun statusBarWindow(service: AccessibilityService): AccessibilityWindowInfo? =
        service.windows.firstOrNull { w ->
            val b = Rect().also(w::getBoundsInScreen)
            w.type == AccessibilityWindowInfo.TYPE_SYSTEM &&
                w.root?.packageName == SYSTEMUI && b.top == 0 && b.height() in 1..400
        }

    /** Logs the status bar node tree with bounds. Returns the battery node bounds if found. */
    fun dumpStatusBar(service: AccessibilityService): Rect? {
        val window = statusBarWindow(service)
        if (window == null) {
            Log.w(TAG, "status bar window not found")
            return null
        }
        var battery: Rect? = null
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            val b = Rect().also(node::getBoundsInScreen)
            val id = node.viewIdResourceName?.substringAfter(":id/")
            Log.i(TAG, "${"  ".repeat(depth)}$id ${node.className?.toString()?.substringAfterLast('.')} " +
                "desc=${node.contentDescription} text=${node.text} $b vis=${node.isVisibleToUser}")
            val looksLikeBattery = id?.contains("battery", ignoreCase = true) == true ||
                node.contentDescription?.contains("battery", ignoreCase = true) == true
            if (battery == null && looksLikeBattery && node.isVisibleToUser && !b.isEmpty) battery = b
            for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, depth + 1) }
        }
        window.root?.let { walk(it, 0) }
        Log.i(TAG, "battery bounds = $battery")
        return battery
    }

    /** Reads a few SystemUI dimens by name, in px. Missing names are skipped. */
    fun systemUiDimens(context: Context): Map<String, Int> {
        val res = try {
            context.packageManager.getResourcesForApplication(SYSTEMUI)
        } catch (e: Exception) {
            Log.w(TAG, "can't read SystemUI resources", e)
            return emptyMap()
        }
        val names = listOf(
            "status_bar_height", "status_bar_padding_end", "status_bar_padding_start",
            "status_bar_battery_icon_width", "status_bar_battery_icon_height",
            "status_bar_icon_size", "status_bar_icon_size_sp", "signal_cluster_battery_padding",
            "rounded_corner_content_padding", "status_bar_system_icons_padding_end",
        )
        return buildMap {
            for (n in names) {
                val id = res.getIdentifier(n, "dimen", SYSTEMUI)
                if (id != 0) put(n, res.getDimensionPixelSize(id))
            }
        }.also { Log.i(TAG, "systemui dimens: $it") }
    }

    fun typeName(t: Int) = when (t) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "app"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "ime"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "system"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "a11y_overlay"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "divider"
        else -> "type$t"
    }
}
