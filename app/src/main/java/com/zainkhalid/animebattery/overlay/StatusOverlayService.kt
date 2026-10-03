package com.zainkhalid.animebattery.overlay

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.zainkhalid.animebattery.battery.BatterySnapshot
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.battery.BatteryStateMachine
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.settings.AppSettings

/**
 * Puts the character badge over the stock battery icon.
 *
 * Android 17 won't let us hide the stock icon, so the badge's opaque plate sits
 * exactly on top of it. We find it through the status bar's accessibility nodes and
 * re-measure whenever windows change, the charger is plugged, or the screen rotates.
 *
 * The badge is shown only when the status bar is: it hides for the shade, quick
 * settings, the lock screen, full screen apps and when the screen is off.
 */
class StatusOverlayService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var wm: WindowManager
    private lateinit var settings: AppSettings
    private lateinit var reader: StatusBarReader
    private lateinit var badge: BadgeView
    private lateinit var full: FullBarController
    private val fullMode get() = settings.barMode == AppSettings.MODE_FULL
    private val handler = Handler(Looper.getMainLooper())
    private val machine = BatteryStateMachine()
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = "AnimeBatteryOverlay"
    }

    private var attached = false
    private var fullAttached = false
    private var screenOn = true
    private var powerSave = false
    private var lastPlugged: Boolean? = null
    private var snapshot = BatterySnapshot(50, plugged = false)
    /** Set from adb for screen recordings: overrides the real battery. */
    private var forced: BatterySnapshot? = null

    private val measureNow = Runnable { refresh(measure = true) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_BATTERY_CHANGED -> onBattery(i)
                Intent.ACTION_SCREEN_OFF -> { screenOn = false; refresh(measure = false) }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> { screenOn = true; scheduleMeasure(150) }
                Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED -> full.onClockTick()
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    powerSave = getSystemService(PowerManager::class.java).isPowerSaveMode
                    pushState()
                }
            }
        }
    }

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WindowManager::class.java)
        settings = AppSettings(this)
        reader = StatusBarReader(this)
        badge = BadgeView(this)
        full = FullBarController(this)
        applySettings()
        settings.prefs.registerOnSharedPreferenceChangeListener(this)

        powerSave = getSystemService(PowerManager::class.java).isPowerSaveMode
        screenOn = getSystemService(PowerManager::class.java).isInteractive
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        // Sticky battery broadcast comes back straight away with the current state.
        registerReceiver(receiver, filter)?.let(::onBattery)
        Log.i(TAG, "connected, sdk ${Build.VERSION.SDK_INT}")
        scheduleMeasure(0)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (fullMode) {
            // The bar colour follows the app underneath.
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) { full.onScrolled(); return }
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) full.onAppChanged()
        } else if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        // Hide quickly when the shade comes down; the full re-measure can wait a moment.
        refresh(measure = false)
        scheduleMeasure(150)
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation: the status bar nodes take a moment to settle, so measure twice.
        scheduleMeasure(250)
        handler.postDelayed(measureNow, 900)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        if (instance !== this) return
        handler.removeCallbacksAndMessages(null)
        detach()
        full.release()
        runCatching { unregisterReceiver(receiver) }
        settings.prefs.unregisterOnSharedPreferenceChangeListener(this)
        instance = null
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        applySettings()
        scheduleMeasure(0)
    }

    private fun applySettings() {
        badge.art = Characters.byId(settings.characterId)
        badge.showPercent = settings.showPercent
        full.view.characterId = settings.characterId
        full.view.sizeDp = settings.characterSizeDp
        full.view.spot = when (settings.characterSpot) {
            AppSettings.SPOT_LEAN -> FullBarView.Spot.IslandLean
            AppSettings.SPOT_PEEK -> FullBarView.Spot.IslandPeek
            else -> FullBarView.Spot.Battery
        }
        full.view.islandOn = settings.islandOn
        full.view.islandWidthDp = settings.islandWidthDp
        full.view.sparkles = settings.sparkles
        pushState()
    }

    private fun onBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        snapshot = BatterySnapshot(
            level = if (level < 0) 50 else level * 100 / scale,
            plugged = plugged,
            full = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL,
            temperatureC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) / 10f,
        )
        pushState()
        if (plugged != lastPlugged) {
            // The charging bolt slides in and moves the stock icon; follow it.
            lastPlugged = plugged
            scheduleMeasure(400)
            handler.postDelayed(measureNow, 1200)
        }
    }

    private fun pushState() {
        val s = (forced ?: snapshot).copy(powerSave = (forced?.powerSave ?: false) || powerSave)
        val state = machine.update(s)
        badge.animator.animationsEnabled = settings.animations && !powerSave && screenOn && attached
        badge.show(state, s.level)
        full.show(state, s.level, settings.animations && !powerSave && screenOn)
    }

    private fun scheduleMeasure(delayMs: Long) {
        handler.removeCallbacks(measureNow)
        handler.postDelayed(measureNow, delayMs)
    }

    /** Decides whether the badge should be up, and (if [measure]) re-places it. */
    private fun refresh(measure: Boolean) {
        val metrics = wm.currentWindowMetrics
        val w = metrics.bounds.width()
        val h = metrics.bounds.height()
        val sbHeight = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked

        val bar = reader.barState(w, h, sbHeight)
        val want = screenOn && !settings.paused &&
            !(locked && settings.hideOnLockScreen) && bar == StatusBarReader.Bar.Shown
        if (!want) {
            if (attached) Log.d(TAG, "hide: screen=$screenOn locked=$locked bar=$bar paused=${settings.paused}")
            detach()
            full.detach()
            fullAttached = false
            return
        }
        if (fullMode) {
            detach()
            if (!measure && fullAttached) return
            // Line our clock and icons up with where the stock ones are.
            val stock = reader.findStock(w, sbHeight)
            val d = resources.displayMetrics.density
            val endPad = stock?.let { (w - it.battery.right).toFloat() } ?: (40.8f * d)
            val barH = stock?.barHeight ?: sbHeight
            full.update(true, barH, startPad = endPad, endPad = endPad)
            fullAttached = true
            pushState()
            return
        }
        full.detach()
        fullAttached = false
        if (!measure && attached) return

        val stock = reader.findStock(w, sbHeight)
        if (stock == null) {
            Log.d(TAG, "stock battery not found")
            detach()
            return
        }
        val d = resources.displayMetrics.density
        badge.layout.place(
            stock.battery.left.toFloat(), stock.battery.top.toFloat(),
            stock.battery.right.toFloat(), stock.battery.bottom.toFloat(),
            minLeft = stock.freeLeft + 2f * d, statusBarHeight = stock.barHeight.toFloat(),
            screenWidth = w.toFloat(), density = d,
            size = settings.size, nudgePx = settings.nudgeDp * d,
        )
        attach()
    }

    private fun attach() {
        val l = badge.layout
        val changed = params.x != l.windowLeft || params.y != l.windowTop ||
            params.width != l.width.toInt() + 1 || params.height != l.height.toInt()
        params.x = l.windowLeft
        params.y = l.windowTop
        params.width = l.width.toInt() + 1
        params.height = l.height.toInt()
        if (!attached) {
            wm.addView(badge, params)
            attached = true
            pushState()
            Log.d(TAG, "show at ${params.x},${params.y} ${params.width}x${params.height}")
        } else if (changed) {
            wm.updateViewLayout(badge, params)
            badge.invalidate()
            Log.d(TAG, "move to ${params.x},${params.y} ${params.width}x${params.height}")
        }
    }

    private fun detach() {
        if (!attached) return
        runCatching { wm.removeView(badge) }
        attached = false
        badge.animator.animationsEnabled = false
    }

    /** Debug and recording commands from MainActivity / adb. */
    fun run(cmd: String, extras: Intent) {
        when (cmd) {
            "dump" -> { StatusBarProbe.dumpWindows(this); StatusBarProbe.dumpStatusBar(this) }
            "force" -> {
                forced = BatterySnapshot(
                    level = extras.getIntExtra("level", 50),
                    plugged = extras.getBooleanExtra("plugged", false),
                    temperatureC = extras.getFloatExtra("temp", 30f),
                    powerSave = extras.getBooleanExtra("saver", false),
                )
                Log.i(TAG, "forced $forced")
                pushState()
            }
            "unforce" -> { forced = null; pushState() }
        }
    }

    /** Turns the whole thing off: the stock icon is never touched, so removing the badge restores it. */
    fun turnOff() {
        detach()
        disableSelf()
    }

    val currentState: BatteryState get() = machine.state

    companion object {
        private const val TAG = "AnimeBattery"
        var instance: StatusOverlayService? = null
            private set
    }
}
