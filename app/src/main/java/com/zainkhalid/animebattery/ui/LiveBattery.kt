package com.zainkhalid.animebattery.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.battery.BatteryStateMachine
import com.zainkhalid.animebattery.system.BatteryReader
import kotlinx.coroutines.delay

@Immutable
data class LiveBattery(
    val level: Int,
    val state: BatteryState,
    /** Android's own reading is frozen; [systemLevel] is the wrong number it's showing. */
    val stuck: Boolean = false,
    val systemLevel: Int = level,
)

/**
 * The real battery, kept up to date while on screen. Same reader and state machine
 * as the overlay. Also re-reads once a minute, because a frozen battery broadcast
 * never fires (see [BatteryReader]).
 */
@Composable
fun rememberLiveBattery(): LiveBattery {
    val context = LocalContext.current
    val machine = remember { BatteryStateMachine() }
    var live by remember { mutableStateOf(LiveBattery(50, BatteryState.Mid)) }
    fun update(intent: Intent? = BatteryReader.sticky(context)) {
        val r = BatteryReader.read(context, intent)
        live = LiveBattery(r.snapshot.level, machine.update(r.snapshot), r.stuck, r.systemLevel)
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) =
                update(if (i.action == Intent.ACTION_BATTERY_CHANGED) i else BatteryReader.sticky(c))
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(receiver, filter)?.let { update(it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(context) {
        while (true) {
            delay(60_000)
            update()
        }
    }
    return live
}
