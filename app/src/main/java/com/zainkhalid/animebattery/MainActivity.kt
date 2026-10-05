package com.zainkhalid.animebattery

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.decor.Pose
import com.zainkhalid.animebattery.overlay.OverlayHealth
import com.zainkhalid.animebattery.overlay.StatusOverlayService
import com.zainkhalid.animebattery.render.ArtSheet
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.system.StockIconController
import com.zainkhalid.animebattery.ui.AnimeBatteryTheme
import com.zainkhalid.animebattery.ui.AppShell
import com.zainkhalid.animebattery.widget.BatteryBuddyWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hosts the app UI. Debug commands also work from adb:
 * adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd hide_icon
 */
class MainActivity : ComponentActivity() {

    private lateinit var stock: StockIconController
    private var healing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        stock = StockIconController(this)
        handle(intent)
        setContent {
            AnimeBatteryTheme { AppShell() }
        }
    }

    override fun onResume() {
        super.onResume()
        // The widget can't update while the app is force-stopped; catch it up on open.
        lifecycleScope.launch { runCatching { BatteryBuddyWidget.refresh(this@MainActivity) } }
        healOverlay()
    }

    /**
     * If a crash took the overlay down, Android won't bring it back by itself. Give the
     * system a moment to bind it normally, then restart it if it's still missing.
     */
    private fun healOverlay() {
        if (healing || !OverlayHealth.canRestart(this)) return
        healing = true
        lifecycleScope.launch {
            delay(1500)
            if (OverlayHealth.status(this@MainActivity) == OverlayHealth.Status.Stopped) OverlayHealth.restart(this@MainActivity)
            healing = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra("cmd")?.let { command(it, intent) }
    }

    private fun command(cmd: String, intent: Intent? = null) {
        when (cmd) {
            "hide_icon" -> Log.i("AnimeBattery", "hide -> ${stock.hide()}")
            "restore_icon" -> Log.i("AnimeBattery", "restore -> ${stock.restore()}")
            "sheet" -> Characters.all.forEach { Log.i("AnimeBattery", "sheet -> ${ArtSheet.render(this, it)}") }
            "pose" -> {
                // adb ... --es cmd pose --es pose Hanging
                val settings = AppSettings(this)
                val pose = runCatching { Pose.valueOf(intent?.getStringExtra("pose") ?: "") }.getOrNull() ?: return
                settings.barLayout = settings.barLayout.copy(pose = pose)
                Log.i("AnimeBattery", "pose -> $pose")
            }
            else -> StatusOverlayService.instance?.run(cmd, intent ?: Intent())
                ?: Log.w("AnimeBattery", "service not running, can't run $cmd")
        }
    }
}
