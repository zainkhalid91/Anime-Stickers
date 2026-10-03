package com.zainkhalid.animebattery.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Reads the status bar through accessibility.
 *
 * What we learned on the Pixel 8 Pro (Android 17):
 *  - the status bar is a SystemUI TYPE_SYSTEM window at the top, as tall as the status bar inset
 *  - with the shade, quick settings or the lock screen up, that window is replaced by a
 *    full-screen SystemUI window
 *  - the bar is Compose; its nodes carry resource ids "battery" and "statusIcons"
 */
class StatusBarReader(private val service: AccessibilityService) {

    enum class Bar { Shown, Covered, Hidden }

    private val bounds = Rect()

    /** Cheap: only looks at window bounds. Safe to call on every window event. */
    fun barState(screenWidth: Int, screenHeight: Int, statusBarHeight: Int): Bar {
        var bar = false
        for (w in service.windows) {
            if (w.type != AccessibilityWindowInfo.TYPE_SYSTEM) continue
            w.getBoundsInScreen(bounds)
            if (bounds.height() > screenHeight / 2 && bounds.width() >= screenWidth / 2) {
                // Shade, quick settings, lock screen or a SystemUI dialog over everything.
                if (isSystemUi(w)) return Bar.Covered
            } else if (bounds.top == 0 && bounds.width() == screenWidth &&
                bounds.height() in 1..statusBarHeight * 3 / 2
            ) {
                bar = true
            }
        }
        return if (bar) Bar.Shown else Bar.Hidden
    }

    class Stock(val battery: Rect, val freeLeft: Int, val barHeight: Int)

    /** Walks the status bar nodes for the stock battery. Null if it can't be found. */
    fun findStock(screenWidth: Int, statusBarHeight: Int): Stock? {
        val window = service.windows.firstOrNull { w ->
            w.getBoundsInScreen(bounds)
            w.type == AccessibilityWindowInfo.TYPE_SYSTEM && bounds.top == 0 &&
                bounds.width() == screenWidth && bounds.height() in 1..statusBarHeight * 3 / 2
        } ?: return null
        window.getBoundsInScreen(bounds)
        val barHeight = bounds.height()
        val root = window.root ?: return null
        val battery = find(root, "battery") ?: return null
        val b = Rect().also(battery::getBoundsInScreen)
        if (b.isEmpty || !battery.isVisibleToUser) return null
        // Free space starts after whatever sits left of the battery (Wi-Fi, signal...).
        val icons = find(root, "statusIcons")
        val freeLeft = icons?.let { Rect().also(it::getBoundsInScreen).right } ?: (b.left - 10)
        return Stock(b, freeLeft, barHeight)
    }

    private fun find(node: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val nodeId = node.viewIdResourceName
        if (nodeId == id || nodeId?.endsWith(":id/$id") == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            find(child, id)?.let { return it }
        }
        return null
    }

    private fun isSystemUi(w: AccessibilityWindowInfo) =
        w.root?.packageName == "com.android.systemui"
}
