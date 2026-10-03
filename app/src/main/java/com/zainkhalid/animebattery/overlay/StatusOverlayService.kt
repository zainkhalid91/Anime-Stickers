package com.zainkhalid.animebattery.overlay

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 0 spike. Puts a red square where the stock battery icon was and logs
 * everything we need to judge alignment: SystemUI dimens, the status bar node
 * tree, the window list, and the battery broadcast.
 */
class StatusOverlayService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var square: View? = null
    private val prefs by lazy { getSharedPreferences("spike", MODE_PRIVATE) }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = logBattery(i)
    }

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WindowManager::class.java)
        Log.i(TAG, "service connected, sdk=${Build.VERSION.SDK_INT} ${Build.DISPLAY}")
        StatusBarProbe.systemUiDimens(this)
        // Sticky broadcast: the return value is the current state.
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let(::logBattery)
        showSquare()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            Log.d(TAG, "event ${AccessibilityEvent.eventTypeToString(event.eventType)} " +
                "pkg=${event.packageName} cls=${event.className} changes=${event.windowChanges}")
        }
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "config changed, rotation=${rotation()}")
        if (square != null) showSquare()
    }

    override fun onDestroy() {
        removeSquare()
        runCatching { unregisterReceiver(batteryReceiver) }
        instance = null
        super.onDestroy()
    }

    /** Commands from MainActivity or `adb shell am start ... --es cmd <name>`. */
    fun run(cmd: String) {
        Log.i(TAG, "cmd $cmd")
        when (cmd) {
            "measure" -> measure()
            "dump" -> { StatusBarProbe.dumpWindows(this); StatusBarProbe.dumpStatusBar(this) }
            "show" -> showSquare()
            "hide_square" -> removeSquare()
        }
    }

    /** Run with the stock battery visible. Saves its bounds for the current rotation. */
    private fun measure() {
        StatusBarProbe.dumpWindows(this)
        val b = StatusBarProbe.dumpStatusBar(this) ?: return
        prefs.edit().putString("rot${rotation()}", "${b.left},${b.top},${b.right},${b.bottom}").apply()
        Log.i(TAG, "saved rot${rotation()} = $b")
        if (square != null) showSquare()
    }

    private fun savedRect(): Rect? = prefs.getString("rot${rotation()}", null)
        ?.split(',')?.map { it.toInt() }?.let { Rect(it[0], it[1], it[2], it[3]) }

    /** Measured bounds when we have them, else a guess from SystemUI dimens. */
    private fun targetRect(): Rect {
        savedRect()?.let { return it }
        val metrics = wm.currentWindowMetrics
        val insets = metrics.windowInsets
        val sb = insets.getInsets(WindowInsets.Type.statusBars()).top
        val dims = StatusBarProbe.systemUiDimens(this)
        val w = dims["status_bar_battery_icon_width"] ?: (sb * 0.6f).toInt()
        val h = dims["status_bar_battery_icon_height"] ?: (sb * 0.5f).toInt()
        val end = (dims["status_bar_padding_end"] ?: 0) +
            insets.getInsets(WindowInsets.Type.displayCutout()).right
        val right = metrics.bounds.width() - end
        val top = (sb - h) / 2
        return Rect(right - w, top, right, top + h).also { Log.i(TAG, "guessed rect $it (sb=$sb)") }
    }

    private fun showSquare() {
        removeSquare()
        val r = targetRect()
        val lp = WindowManager.LayoutParams(
            r.width(), r.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = r.left
            y = r.top
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "AnimeBatteryOverlay"
        }
        val v = RedSquare(this)
        wm.addView(v, lp)
        square = v
        Log.i(TAG, "square at $r, rotation=${rotation()}")
    }

    private fun removeSquare() {
        square?.let { runCatching { wm.removeView(it) } }
        square = null
    }

    @Suppress("DEPRECATION")
    private fun rotation(): Int = wm.defaultDisplay.rotation

    private fun logBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val saver = getSystemService(PowerManager::class.java).isPowerSaveMode
        Log.i(TAG, "battery level=${level * 100 / scale} status=$status plugged=$plugged temp=$temp saver=$saver")
    }

    /** Translucent red fill with a 1px outline, so the stock icon edge shows through when comparing. */
    private class RedSquare(c: Context) : View(c) {
        private val fill = Paint().apply { color = Color.argb(170, 255, 0, 0) }
        private val line = Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 1f }
        override fun onDraw(canvas: Canvas) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
            canvas.drawRect(0.5f, 0.5f, width - 0.5f, height - 0.5f, line)
        }
    }

    companion object {
        private const val TAG = "AnimeBattery"
        var instance: StatusOverlayService? = null
            private set
    }
}
