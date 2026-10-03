package com.zainkhalid.animebattery.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zainkhalid.animebattery.R
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.overlay.FullBarView
import com.zainkhalid.animebattery.overlay.StatusOverlayService
import com.zainkhalid.animebattery.settings.AppSettings
import kotlin.math.roundToInt

/**
 * Main screen: a live preview of the status bar (the real bar view, so it's exactly
 * what you'll get), then the controls. Every change is saved straight away and the
 * overlay service picks it up.
 */
@Composable
fun HomeScreen(onOpenSamples: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var mode by remember { mutableStateOf(settings.barMode) }
    var spot by remember { mutableStateOf(settings.characterSpot) }
    var size by remember { mutableFloatStateOf(settings.characterSizeDp) }
    var islandOn by remember { mutableStateOf(settings.islandOn) }
    var islandWidth by remember { mutableFloatStateOf(settings.islandWidthDp) }
    var sparkles by remember { mutableStateOf(settings.sparkles) }
    var animations by remember { mutableStateOf(settings.animations) }
    var serviceOn by remember { mutableStateOf(StatusOverlayService.instance != null) }
    LifecycleResumeEffect(Unit) {
        serviceOn = StatusOverlayService.instance != null
        onPauseOrDispose { }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        Header()
        if (!serviceOn) SetupCard { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

        LivePreview(
            full = mode == AppSettings.MODE_FULL, spot = spot, sizeDp = size,
            islandOn = islandOn, islandWidthDp = islandWidth, sparkles = sparkles, animations = animations,
        )

        Section("Style") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(AppSettings.MODE_FULL to "Custom bar", AppSettings.MODE_BADGE to "Battery badge")
                options.forEachIndexed { i, (key, label) ->
                    SegmentedButton(
                        selected = mode == key,
                        onClick = { mode = key; settings.barMode = key },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            Text(
                if (mode == AppSettings.MODE_FULL) "Draws its own status bar, so the character can be big and sit on the camera island."
                else "Covers just the stock battery icon. Smallest and most subtle.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (mode == AppSettings.MODE_FULL) {
            Section("Character") {
                Text("Where it sits", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
                    listOf(
                        AppSettings.SPOT_BATTERY to "By battery",
                        AppSettings.SPOT_LEAN to "On island",
                        AppSettings.SPOT_PEEK to "Peeking",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = spot == key,
                            onClick = {
                                spot = key; settings.characterSpot = key
                                if (key != AppSettings.SPOT_BATTERY && !islandOn) { islandOn = true; settings.islandOn = true }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                SliderRow("Size", "${size.roundToInt()} dp", size, 24f..56f, 15, { size = it }) { settings.characterSizeDp = size }
            }

            Section("Camera island") {
                ToggleRow("Island", "A pill round the camera, like the iPhone Dynamic Island", islandOn) {
                    islandOn = it; settings.islandOn = it
                }
                if (islandOn) {
                    SliderRow("Width", "${islandWidth.roundToInt()} dp", islandWidth, 64f..180f, 0, { islandWidth = it }) {
                        settings.islandWidthDp = islandWidth
                    }
                    ToggleRow("Sparkles", "Little stars twinkling round the island", sparkles) { sparkles = it; settings.sparkles = it }
                }
            }
        }

        Section("Motion") {
            ToggleRow("Animations", "Blink, bob and charging effects. Off saves a little battery.", animations) {
                animations = it; settings.animations = it
            }
        }

        CharacterCard()

        Button(
            onClick = onOpenSamples, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
        ) { Text("Explore ideas: widget, charging screen, screen pet") }
        Spacer(Modifier.height(Tokens.Gap))
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Anime Battery", style = MaterialTheme.typography.displaySmall)
            Text("Your status bar, with a friend in it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Image(
            painterResource(R.drawable.sticker_naruto_happy), contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun SetupCard(onOpen: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            Text("Turn on the overlay", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Android only lets accessibility windows draw over the status bar. Open Accessibility, tap Anime Battery overlay and switch it on.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onOpen) { Text("Open Accessibility settings") }
        }
    }
}

/** The real FullBarView over a fake wallpaper, so the preview can't drift from the real thing. */
@Composable
private fun LivePreview(
    full: Boolean, spot: String, sizeDp: Float, islandOn: Boolean, islandWidthDp: Float,
    sparkles: Boolean, animations: Boolean,
) {
    val density = LocalDensity.current.density
    val barPx = (59 * density).roundToInt()
    Card(
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier.fillMaxWidth().height(200.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B1F3B), Color(0xFF3B2A5C), Color(0xFFE07A9A))))
                .semantics { contentDescription = "Live status bar preview" },
        ) {
            if (full) {
                AndroidView(
                    factory = { ctx -> FullBarView(ctx) },
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    update = { v ->
                        v.barHeight = barPx
                        v.startPad = 28 * density
                        v.endPad = 28 * density
                        v.sizeDp = sizeDp
                        v.spot = when (spot) {
                            AppSettings.SPOT_LEAN -> FullBarView.Spot.IslandLean
                            AppSettings.SPOT_PEEK -> FullBarView.Spot.IslandPeek
                            else -> FullBarView.Spot.Battery
                        }
                        v.islandOn = islandOn
                        v.islandWidthDp = islandWidthDp
                        v.sparkles = sparkles
                        v.wifiLevel = 3
                        v.cellLevel = 3
                        v.animator.animationsEnabled = animations
                        v.setBackgroundSample(0xFF1B1F3B.toInt())
                        v.show(BatteryState.Good, 76)
                    },
                )
            } else {
                Text(
                    "Battery badge mode: see the Status bar preview under Explore ideas.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = Color.White,
                )
            }
            Text(
                "LIVE PREVIEW", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }
    }
}

@Composable
private fun CharacterCard() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1E6)),
    ) {
        Row(Modifier.padding(Tokens.Gap), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(Color(0xFFFFD9B8)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.sticker_naruto_happy), contentDescription = null, modifier = Modifier.size(74.dp))
            }
            Spacer(Modifier.width(Tokens.Gap))
            Column {
                Text("Naruto Uzumaki", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3A1D08))
                Text("Naruto · fan art", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A4A28))
                Text(
                    "Naruto © Masashi Kishimoto / Shueisha. Not affiliated. More characters coming.",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A4A28),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, steps: Int,
    onChange: (Float) -> Unit, onDone: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = v, onValueChange = onChange, valueRange = range, steps = steps, onValueChangeFinished = onDone)
    }
}

@Composable
private fun ToggleRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
