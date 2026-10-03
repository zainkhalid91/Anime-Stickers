package com.zainkhalid.animebattery.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.battery.BatterySnapshot
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.battery.BatteryStateMachine
import com.zainkhalid.animebattery.characters.CharacterAnimator
import com.zainkhalid.animebattery.characters.CharacterArt
import com.zainkhalid.animebattery.render.StatusBarMock
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A fake Pixel status bar over a fake app, with our badge drawn exactly the way the
 * overlay draws it. Slider and toggles drive the same state machine as the real
 * battery. 8x zoom shows the real pixels, unsmoothed.
 */
@Composable
fun PreviewScreen(art: CharacterArt, showPercent: Boolean, size: Float, modifier: Modifier = Modifier) {
    var level by rememberSaveable { mutableFloatStateOf(64f) }
    var charging by rememberSaveable { mutableStateOf(false) }
    var saver by rememberSaveable { mutableStateOf(false) }
    var hot by rememberSaveable { mutableStateOf(false) }
    var light by rememberSaveable { mutableStateOf(false) }
    var zoom by rememberSaveable { mutableIntStateOf(1) }

    val machine = remember { BatteryStateMachine() }
    val animator = remember { CharacterAnimator(seed = 42) }
    val state = remember(level.roundToInt(), charging, saver, hot) {
        machine.update(
            BatterySnapshot(
                level = level.roundToInt(), plugged = charging,
                temperatureC = if (hot) 44f else 31f, powerSave = saver,
            ),
        )
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        PhoneMock(art, animator, state, level.roundToInt(), charging, light, zoom, showPercent, size)

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(1, 8).forEachIndexed { i, z ->
                SegmentedButton(
                    selected = zoom == z,
                    onClick = { zoom = z },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(if (z == 1) "1× real pixels" else "8× zoom") }
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Battery", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StateChip(state)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${level.roundToInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.width(72.dp),
                    )
                    Slider(
                        value = level, onValueChange = { level = it }, valueRange = 0f..100f,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Battery level" },
                    )
                }
                ToggleRow("Charging", "Plugged in", charging) { charging = it }
                ToggleRow("Power saver", "Calm pose, no animation", saver) { saver = it }
                ToggleRow("Hot", "42 °C or above", hot) { hot = it }
                ToggleRow("Light status bar", "Check it on a light app too", light) { light = it }
            }
        }
    }
}

@Composable
private fun PhoneMock(
    art: CharacterArt, animator: CharacterAnimator, state: BatteryState, level: Int,
    charging: Boolean, light: Boolean, zoom: Int, showPercent: Boolean, size: Float,
) {
    val density = LocalDensity.current.density
    val context = androidx.compose.ui.platform.LocalContext.current
    val mock = remember(density) { StatusBarMock(context, density) }
    var version by remember { mutableIntStateOf(0) }

    LaunchedEffect(state, level) {
        animator.setLevel(level)
        animator.setState(state, SystemClock.uptimeMillis())
    }

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = if (light) Color(0xFFF6F2F7) else Color(0xFF0E0E12)),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val widthPx = constraints.maxWidth
            val heightPx = (59 * density).roundToInt()
            val bitmap = remember(widthPx, heightPx) { Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888) }
            val image = remember(bitmap) { bitmap.asImageBitmap() }

            // Frame clock: at most 12 fps, redraw only when the animator says so.
            LaunchedEffect(bitmap, light, charging, showPercent, size, art) {
                animator.pxPerUnit = 2f
                var first = true
                while (true) {
                    val now = SystemClock.uptimeMillis()
                    if (animator.advance(now) || first) {
                        mock.draw(bitmap, light, level, charging, art, animator.frame, showPercent, size)
                        animator.pxPerUnit = mock.layout.unit
                        version++
                        first = false
                    }
                    val d = animator.nextDelayMs()
                    delay(if (d < 0) 120 else d)
                }
            }

            Column {
                val barHeight = with(LocalDensity.current) { heightPx.toDp() }
                Canvas(
                    Modifier.fillMaxWidth().height(if (zoom == 1) barHeight else barHeight * 4)
                        .semantics { contentDescription = "Status bar preview, ${state.name}, $level percent" },
                ) {
                    version
                    if (zoom == 1) {
                        drawImage(image, filterQuality = FilterQuality.None)
                    } else {
                        // Show the right-hand end of the bar, 8x, nearest neighbour.
                        val srcW = (this.size.width / zoom).roundToInt()
                        val srcH = (this.size.height / zoom).roundToInt().coerceAtMost(heightPx)
                        val srcX = (widthPx - srcW - (12 * density).roundToInt()).coerceAtLeast(0)
                        val srcY = ((heightPx - srcH) / 2).coerceAtLeast(0)
                        drawImage(
                            image, srcOffset = IntOffset(srcX, srcY), srcSize = IntSize(srcW, srcH),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(srcW * zoom, srcH * zoom),
                            filterQuality = FilterQuality.None,
                        )
                    }
                }
                FakeApp(light)
            }
        }
    }
}

/** A plain messaging-style app under the status bar, so the badge is seen in context. */
@Composable
private fun FakeApp(light: Boolean) {
    val fg = if (light) Color(0xFF1C1B1F) else Color(0xFFE6E1E6)
    val faint = fg.copy(alpha = 0.10f)
    val mid = fg.copy(alpha = 0.22f)
    val accents = listOf(Color(0xFFF97316), Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFDB2777))
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(faint))
        accents.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(a.copy(alpha = 0.85f)))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(120.dp).height(10.dp).clip(CircleShape).background(mid))
                    Box(Modifier.width(190.dp).height(8.dp).clip(CircleShape).background(faint))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StateChip(state: BatteryState) {
    val label = when (state) {
        BatteryState.PowerSaver -> "Power saver"
        else -> state.name
    }
    Box(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ToggleRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
