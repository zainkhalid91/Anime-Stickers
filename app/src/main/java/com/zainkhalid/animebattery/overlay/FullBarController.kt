package com.zainkhalid.animebattery.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import com.zainkhalid.animebattery.battery.BatteryState

/**
 * The full custom status bar mode: owns the window, keeps its background in step with
 * the app underneath, and feeds it Wi-Fi/signal/clock.
 *
 * Background colour: we take an accessibility screenshot (Android 11+) and read a
 * handful of pixels on the row just below the status bar. The bitmap is only read in
 * memory and dropped straight away; nothing is stored. Throttled to at most ~1.5/s.
 */
class FullBarController(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val d = service.resources.displayMetrics.density
    val view = FullBarView(service)
    private var attached = false
    private var lastSampleMs = 0L
    private var sampling = false
    private val sampleNow = Runnable { sample() }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, 1,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 30) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        title = "AnimeBatteryFullBar"
    }

    private val net = service.getSystemService(ConnectivityManager::class.java)
    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = readRadios()
        override fun onLost(network: Network) = readRadios()
    }

    init {
        view.onSizeNeeded = { h -> if (attached && params.height != h) { params.height = h; wm.updateViewLayout(view, params) } }
        runCatching { net.registerDefaultNetworkCallback(netCallback, service.mainThreadHandler()) }
    }

    /** Show or hide, and place. [endPad]/[startPad] mirror where the stock icons sit. */
    fun update(visible: Boolean, barHeight: Int, startPad: Float, endPad: Float) {
        if (!visible) {
            detach()
            return
        }
        view.barHeight = barHeight
        view.startPad = startPad
        view.endPad = endPad
        params.x = 0
        params.y = 0
        params.height = view.neededHeight()
        if (!attached) {
            wm.addView(view, params)
            attached = true
            readRadios()
            scheduleSample(0)
        } else {
            wm.updateViewLayout(view, params)
        }
    }

    fun detach() {
        if (!attached) return
        runCatching { wm.removeView(view) }
        attached = false
    }

    fun show(state: BatteryState, level: Int, animations: Boolean) {
        view.animator.animationsEnabled = animations && attached
        view.show(state, level)
    }

    fun onAppChanged() = scheduleSample(250)
    fun onScrolled() = scheduleSample(350)
    fun onClockTick() = view.invalidate()

    fun release() {
        detach()
        runCatching { net.unregisterNetworkCallback(netCallback) }
        view.removeCallbacks(sampleNow)
    }

    private fun scheduleSample(delay: Long) {
        if (!attached) return
        view.removeCallbacks(sampleNow)
        val wait = maxOf(delay, MIN_GAP_MS - (SystemClock.uptimeMillis() - lastSampleMs))
        view.postDelayed(sampleNow, wait)
    }

    private fun sample() {
        if (Build.VERSION.SDK_INT < 30 || sampling || !attached) return
        sampling = true
        lastSampleMs = SystemClock.uptimeMillis()
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY, service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    sampling = false
                    val buffer = result.hardwareBuffer
                    val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                    val color = hw?.let { readRow(it) }
                    hw?.recycle()
                    buffer.close()
                    if (color != null) view.setBackgroundSample(color)
                }

                override fun onFailure(errorCode: Int) {
                    sampling = false
                    Log.d("AnimeBattery", "screenshot failed $errorCode")
                }
            },
        )
    }

    /** Most common colour among a few points on the row just below the bar. */
    private fun readRow(hw: Bitmap): Int? {
        val y = (view.barHeight + 3 * d).toInt().coerceIn(0, hw.height - 1)
        // Copy only that one row out of the hardware bitmap.
        val row = runCatching { Bitmap.createBitmap(hw, 0, y, hw.width, 1).copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
            ?: runCatching {
                // Fallback: copy the whole frame, then cut the row.
                val full = hw.copy(Bitmap.Config.ARGB_8888, false)
                Bitmap.createBitmap(full, 0, y, full.width, 1).also { if (it !== full) full.recycle() }
            }.getOrNull() ?: return null
        val counts = HashMap<Int, Int>()
        for (i in 1..9) {
            val px = row.getPixel(row.width * i / 10, 0)
            // Bucket nearby colours together so tiny noise doesn't win.
            val key = Color.rgb(Color.red(px) / 8 * 8, Color.green(px) / 8 * 8, Color.blue(px) / 8 * 8)
            counts[key] = (counts[key] ?: 0) + 1
        }
        row.recycle()
        return counts.maxByOrNull { it.value }?.key
    }

    private fun readRadios() {
        val caps = runCatching { net.getNetworkCapabilities(net.activeNetwork) }.getOrNull()
        view.wifiLevel = if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val rssi = caps.signalStrength
            if (Build.VERSION.SDK_INT >= 30) {
                service.getSystemService(WifiManager::class.java).calculateSignalLevel(rssi).coerceIn(0, 4)
            } else 4
        } else -1
        view.cellLevel = runCatching {
            service.getSystemService(TelephonyManager::class.java).signalStrength?.level ?: 0
        }.getOrDefault(0)
        view.invalidate()
    }

    private fun Context.mainThreadHandler() = android.os.Handler(mainLooper)

    companion object {
        private const val MIN_GAP_MS = 650L
    }
}
