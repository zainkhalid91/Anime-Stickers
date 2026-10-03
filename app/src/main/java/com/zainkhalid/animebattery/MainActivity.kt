package com.zainkhalid.animebattery

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.overlay.StatusOverlayService
import com.zainkhalid.animebattery.system.StockIconController

/**
 * Phase 0 spike screen. Every button also works from adb:
 * adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd hide_icon
 */
class MainActivity : ComponentActivity() {

    private lateinit var stock: StockIconController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stock = StockIconController(this)
        handle(intent)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { SpikeScreen() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra("cmd")?.let(::command)
    }

    private fun command(cmd: String) {
        when (cmd) {
            "hide_icon" -> Log.i("AnimeBattery", "hide -> ${stock.hide()}")
            "restore_icon" -> Log.i("AnimeBattery", "restore -> ${stock.restore()}")
            else -> StatusOverlayService.instance?.run(cmd)
                ?: Log.w("AnimeBattery", "service not running, can't run $cmd")
        }
    }

    @Composable
    private fun SpikeScreen() {
        // Bumped after each action so the status lines re-read.
        var tick by remember { mutableIntStateOf(0) }
        fun act(cmd: String) { command(cmd); tick++ }
        Column(
            Modifier.padding(24.dp).padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Anime Battery · Phase 0 spike", style = MaterialTheme.typography.titleLarge)
            key(tick) {
                Text("WRITE_SECURE_SETTINGS: ${stock.canWrite()}")
                Text("Overlay service running: ${StatusOverlayService.instance != null}")
                Text("icon_blacklist: ${stock.current()}")
            }
            Button(onClick = { act("measure") }) { Text("1. Measure stock battery (icon visible)") }
            Button(onClick = { act("hide_icon") }) { Text("2. Hide stock battery") }
            Button(onClick = { act("restore_icon") }) { Text("Restore stock battery") }
            Button(onClick = { act("dump") }) { Text("Dump windows + status bar to logcat") }
            Button(onClick = {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) { Text("Open Accessibility settings") }
        }
    }
}
