package com.zainkhalid.animebattery.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zainkhalid.animebattery.R
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.decor.Pose
import com.zainkhalid.animebattery.decor.ThemeSets
import com.zainkhalid.animebattery.overlay.StatusOverlayService
import com.zainkhalid.animebattery.settings.AppSettings
import kotlin.math.roundToInt

/** Enabled in Accessibility settings (it may still be reconnecting, e.g. right after an update). */
fun overlayEnabled(context: android.content.Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val me = android.content.ComponentName(context, StatusOverlayService::class.java)
    return enabled.split(':').any { android.content.ComponentName.unflattenFromString(it) == me }
}

/** Where the home screen can send you. */
enum class Dest { Home, Editor, Wallpapers, Widgets, Ideas }

/**
 * Main screen, following the Figma design: live preview of your real bar, a big
 * "customize" button, poses, collections, and quick settings. Everything saves
 * straight away and the overlay updates live.
 */
@Composable
fun HomeScreen(go: (Dest) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var layout by remember { mutableStateOf(settings.barLayout) }
    var mode by remember { mutableStateOf(settings.barMode) }
    var themeId by remember { mutableStateOf(settings.themeId) }
    var sparkles by remember { mutableStateOf(settings.sparkles) }
    var animations by remember { mutableStateOf(settings.animations) }
    var size by remember { mutableFloatStateOf(layout.characterSizeDp) }
    var serviceOn by remember { mutableStateOf(overlayEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        serviceOn = overlayEnabled(context)
        layout = settings.barLayout
        themeId = settings.themeId
        size = layout.characterSizeDp
        onPauseOrDispose { }
    }
    fun save(newLayout: BarLayout) {
        layout = newLayout
        settings.barLayout = newLayout
    }
    fun useFullBar() {
        if (mode != AppSettings.MODE_FULL) { mode = AppSettings.MODE_FULL; settings.barMode = mode }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        // Header.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Anime Battery", style = MaterialTheme.typography.displaySmall)
                Text("Your status bar, with a friend in it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Image(painterResource(R.drawable.sticker_naruto_happy), contentDescription = null, modifier = Modifier.size(64.dp))
        }
        if (!serviceOn) SetupCard { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

        // Live preview.
        Card(
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                Modifier.fillMaxWidth().height(210.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B1F3B), Color(0xFF3B2A5C), Color(0xFFE07A9A)))),
            ) {
                if (mode == AppSettings.MODE_FULL) {
                    BarPreview(layout, Modifier.fillMaxWidth().height(200.dp), sparkles = sparkles, animations = animations)
                } else {
                    Text(
                        "Battery badge mode: the character sits on the stock battery icon.",
                        color = Color.White, modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                Text(
                    "LIVE PREVIEW", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("${poseLabel(layout.pose)} · ${layout.characterSizeDp.roundToInt()}dp", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // Customize button.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFFF6FA0))))
                .clickable(onClickLabel = "Open the status bar editor") { go(Dest.Editor) }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Customize your status bar", style = MaterialTheme.typography.titleMedium, color = Tokens.OnPrimary)
                Text("Drag stickers, resize, pick poses", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3A1D08))
            }
            Text("→", style = MaterialTheme.typography.headlineSmall, color = Tokens.OnPrimary)
        }

        // Poses.
        Section("Pose") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pose.entries.forEach { p ->
                    val sel = layout.pose == p
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.2.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .clickable { save(layout.copy(pose = p)); useFullBar() }
                            .semantics { role = Role.RadioButton; selected = sel }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PoseIcon(p, Modifier.size(40.dp, 44.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(poseLabel(p), style = MaterialTheme.typography.labelMedium, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Collections.
        Section("Collections") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeSets.all.forEach { t ->
                    val applied = themeId == t.id
                    Column(
                        Modifier.width(150.dp).clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(t.colors.first), Color(t.colors.second))))
                            .border(if (applied) 2.5.dp else 0.dp, if (applied) Color.White else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable(onClickLabel = "Apply ${t.name}") {
                                save(t.layout); themeId = t.id; settings.themeId = t.id; useFullBar()
                            }
                            .padding(14.dp),
                    ) {
                        Image(painterResource(R.drawable.sticker_naruto_happy), contentDescription = null, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(t.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(t.blurb, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 3)
                        if (applied) Text("✓ Applied", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        // More.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoreCard("Wallpapers", "Camera-hole art", Color(0xFF0E1A3A), Color(0xFFF97316), Modifier.weight(1f)) { go(Dest.Wallpapers) }
            MoreCard("Widgets", "Home screen", Color(0xFF3B2A5C), Color(0xFFE07A9A), Modifier.weight(1f)) { go(Dest.Widgets) }
            MoreCard("Ideas", "Samples lab", Color(0xFF134E4A), Color(0xFF2ECC8F), Modifier.weight(1f)) { go(Dest.Ideas) }
        }

        // Quick settings.
        Section("Quick settings") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(AppSettings.MODE_FULL to "Custom bar", AppSettings.MODE_BADGE to "Battery badge")
                options.forEachIndexed { i, (key, label) ->
                    SegmentedButton(
                        selected = mode == key, onClick = { mode = key; settings.barMode = key },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Character size", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${size.roundToInt()} dp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = size, valueRange = 24f..72f,
                    onValueChange = { size = it; layout = layout.copy(characterSizeDp = it) },
                    onValueChangeFinished = { save(layout.copy(characterSizeDp = size)) },
                )
            }
            ToggleRow("Sparkles", "Little stars round the camera", sparkles) { sparkles = it; settings.sparkles = it }
            ToggleRow("Animations", "Blink, sway and effects. Off saves a little battery.", animations) { animations = it; settings.animations = it }
        }
        Text(
            "Naruto © Masashi Kishimoto / Shueisha. Fan art, not affiliated.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Tokens.Gap))
    }
}

@Composable
private fun PoseIcon(p: Pose, modifier: Modifier) {
    when (p) {
        Pose.Eyes -> Canvas(modifier) {
            val r = size.minDimension * 0.24f
            for (side in listOf(-1, 1)) {
                val c = Offset(size.width / 2f + side * r * 1.1f, size.height / 2f)
                drawOval(Color.White, Offset(c.x - r * 0.8f, c.y - r), Size(r * 1.6f, r * 2f))
                drawCircle(Color(0xFF2E7FE6), r * 0.6f, c.copy(y = c.y + r * 0.15f))
                drawCircle(Color(0xFF0E1A3C), r * 0.3f, c.copy(y = c.y + r * 0.2f))
            }
        }
        else -> Box(modifier, contentAlignment = Alignment.TopCenter) {
            if (p == Pose.Hanging) Canvas(Modifier.size(2.dp, 14.dp)) { drawRect(Color.White.copy(alpha = 0.7f)) }
            Image(
                painterResource(R.drawable.sticker_naruto_happy), contentDescription = null,
                modifier = Modifier.padding(top = if (p == Pose.Hanging) 12.dp else 4.dp).size(if (p == Pose.Hanging) 30.dp else 36.dp),
            )
        }
    }
}

@Composable
private fun MoreCard(title: String, sub: String, a: Color, b: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(a, b)))
            .clickable(onClickLabel = "Open $title", onClick = onClick).heightIn(min = 88.dp).padding(14.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
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

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
        }
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
