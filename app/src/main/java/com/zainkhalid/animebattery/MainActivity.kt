package com.zainkhalid.animebattery

import android.content.Intent
import android.os.Bundle
import com.zainkhalid.animebattery.ui.HomeScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import com.zainkhalid.animebattery.ui.lab.LabScreen
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.PreviewScreen
import com.zainkhalid.animebattery.ui.AnimeBatteryTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.overlay.StatusOverlayService
import com.zainkhalid.animebattery.render.ArtSheet
import com.zainkhalid.animebattery.system.StockIconController

/**
 * Phase 0 spike screen. Every button also works from adb:
 * adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd hide_icon
 */
class MainActivity : ComponentActivity() {

    private lateinit var stock: StockIconController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        stock = StockIconController(this)
        handle(intent)
        setContent {
            AnimeBatteryTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier.windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        var explore by rememberSaveable { mutableStateOf(false) }
                        BackHandler(enabled = explore) { explore = false }
                        if (!explore) {
                            HomeScreen(onOpenSamples = { explore = true })
                        } else {
                            TextButton(onClick = { explore = false }) { Text("‹ Back") }
                            var tab by rememberSaveable { mutableIntStateOf(0) }
                            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                                Tab(tab == 0, { tab = 0 }, text = { Text("Ideas") })
                                Tab(tab == 1, { tab = 1 }, text = { Text("Badge preview") })
                            }
                            if (tab == 0) LabScreen()
                            else PreviewScreen(Characters.byId(AppSettings(this@MainActivity).characterId), showPercent = true, size = 1f)
                        }
                    }
                }
            }
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
            else -> StatusOverlayService.instance?.run(cmd, intent ?: Intent())
                ?: Log.w("AnimeBattery", "service not running, can't run $cmd")
        }
    }
}
