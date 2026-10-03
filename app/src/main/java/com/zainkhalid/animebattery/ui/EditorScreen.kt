package com.zainkhalid.animebattery.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.decor.Decor
import com.zainkhalid.animebattery.decor.DecorPainter
import com.zainkhalid.animebattery.decor.DecorType
import com.zainkhalid.animebattery.decor.Pose
import com.zainkhalid.animebattery.settings.AppSettings
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Bar canvas is shown at this zoom so small stickers are easy to grab. */
private const val ZOOM = 2f

/**
 * Drag stickers round the status bar, pinch to resize, pick the pose. Works on a copy
 * of the layout; Apply saves it and the overlay picks it up straight away.
 */
@Composable
fun EditorScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var layout by remember { mutableStateOf(settings.barLayout) }
    var selected by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current.density

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Back") }
            Text("Edit status bar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Button(
                onClick = { settings.barLayout = layout; settings.themeId = null; onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("Apply") }
        }

        // ── Canvas ────────────────────────────────────────────────────────
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1B1F3B), Color(0xFF3B2A5C))))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
        ) {
            val cw = constraints.maxWidth.toFloat()
            val latest by rememberUpdatedState(layout)
            val sel by rememberUpdatedState(selected)
            // Bar coordinates <-> canvas coordinates (zoomed round the top centre).
            fun toBar(o: Offset) = Offset((o.x - cw / 2f) / ZOOM + cw / 2f, o.y / ZOOM)
            fun decorAt(p: Offset): Int {
                val base = 16f * density
                return latest.decor.indexOfFirst { dd ->
                    dd.type != DecorType.Wings && hypot(dd.x * cw - p.x, dd.y * density - p.y) < base * dd.scale + 10f * density
                }
            }

            BarPreview(
                layout = layout,
                modifier = Modifier.requiredWidth(maxWidth).height(115.dp)
                    .graphicsLayer(scaleX = ZOOM, scaleY = ZOOM, transformOrigin = TransformOrigin(0.5f, 0f)),
            )
            // Selection ring + gestures on top.
            Canvas(
                Modifier.fillMaxSize()
                    .semantics { contentDescription = "Status bar editor. Tap a sticker to select it, drag to move, pinch to resize." }
                    .pointerInput(Unit) {
                        detectTapGestures { o -> selected = decorAt(toBar(o)) }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val i = sel.takeIf { it >= 0 } ?: decorAt(toBar(centroid)).also { selected = it }
                            if (i < 0) {
                                // Pinch on empty space resizes the character.
                                if (zoom != 1f) layout = layout.copy(characterSizeDp = (layout.characterSizeDp * zoom).coerceIn(24f, 72f))
                                return@detectTransformGestures
                            }
                            val dd = layout.decor[i]
                            val moved = dd.copy(
                                x = (dd.x + pan.x / ZOOM / cw).coerceIn(0f, 1f),
                                y = (dd.y + pan.y / ZOOM / density).coerceIn(-4f, 110f),
                                scale = (dd.scale * zoom).coerceIn(0.5f, 3f),
                            )
                            layout = layout.copy(decor = layout.decor.toMutableList().also { it[i] = moved })
                        }
                    },
            ) {
                val i = selected
                if (i >= 0 && i < layout.decor.size) {
                    val dd = layout.decor[i]
                    val bx = dd.x * cw
                    val by = dd.y * density
                    val c = Offset((bx - cw / 2f) * ZOOM + cw / 2f, by * ZOOM)
                    drawCircle(
                        Color(0xFF60A5FA), radius = (16f * density * dd.scale + 8f * density) * ZOOM, center = c,
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                    )
                }
            }
            Text(
                "Tap · drag · pinch", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            )
        }

        // ── Selected sticker ──────────────────────────────────────────────
        if (selected in layout.decor.indices) {
            val dd = layout.decor[selected]
            EditorCard("Selected · ${dd.type.label}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", modifier = Modifier.width(56.dp))
                    Slider(
                        value = dd.scale, valueRange = 0.5f..3f, modifier = Modifier.weight(1f),
                        onValueChange = { v -> layout = layout.copy(decor = layout.decor.toMutableList().also { it[selected] = dd.copy(scale = v) }) },
                    )
                }
                OutlinedButton(onClick = {
                    layout = layout.copy(decor = layout.decor.filterIndexed { idx, _ -> idx != selected })
                    selected = -1
                }) { Text("Remove sticker") }
            }
        }

        // ── Pose and character ────────────────────────────────────────────
        EditorCard("Character") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pose.entries.forEach { p ->
                    FilterChip(
                        selected = layout.pose == p, onClick = { layout = layout.copy(pose = p) },
                        label = { Text(poseLabel(p)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
            if (layout.pose != Pose.Eyes) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", modifier = Modifier.width(56.dp))
                    Slider(
                        value = layout.characterSizeDp, valueRange = 24f..72f, modifier = Modifier.weight(1f),
                        onValueChange = { layout = layout.copy(characterSizeDp = it) },
                    )
                    Text("${layout.characterSizeDp.roundToInt()}dp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (layout.pose == Pose.Hanging) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sway", style = MaterialTheme.typography.bodyLarge)
                        Text("Swings gently on the thread", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = layout.sway, onCheckedChange = { layout = layout.copy(sway = it) })
                }
            }
        }

        // ── Sticker tray ──────────────────────────────────────────────────
        EditorCard("Add stickers") {
            val icons = remember(density) { DecorType.entries.associateWith { decorIcon(it, density) } }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecorType.entries.forEach { t ->
                    Column(
                        Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                            .clickable {
                                if (t == DecorType.Wings && layout.decor.any { it.type == DecorType.Wings }) return@clickable
                                // New stickers land near the camera, just under the bar.
                                val n = layout.decor.size
                                layout = layout.copy(decor = layout.decor + Decor(t, 0.38f + (n % 5) * 0.06f, 14f + (n % 3) * 6f))
                                selected = if (t == DecorType.Wings) -1 else layout.decor.lastIndex
                            }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(icons.getValue(t), contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(t.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (layout.decor.isNotEmpty()) {
                TextButton(onClick = { layout = layout.copy(decor = emptyList()); selected = -1 }) { Text("Clear all stickers") }
            }
        }
        Spacer(Modifier.height(Tokens.Gap))
    }
}

fun poseLabel(p: Pose) = when (p) {
    Pose.Hanging -> "Hanging"
    Pose.Battery -> "By battery"
    Pose.Camera -> "By camera"
    Pose.Eyes -> "Eyes"
}

@Composable
private fun EditorCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

/** Small icon for the tray, drawn by the same painter the bar uses. */
private fun decorIcon(t: DecorType, d: Float): androidx.compose.ui.graphics.ImageBitmap {
    val px = (40 * d).roundToInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val painter = DecorPainter(d)
    when (t) {
        DecorType.Wings -> {
            val b = android.graphics.RectF(px / 2f - 9 * d, px / 2f - 5 * d, px / 2f + 9 * d, px / 2f + 5 * d)
            painter.wings(c, b, 0.5f)
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5AE68C.toInt() }
            c.drawRoundRect(b, 3 * d, 3 * d, p)
        }
        DecorType.Web -> painter.draw(c, t, 0f, 0f, 0.8f, 0, 0.25f)
        DecorType.Spider -> painter.draw(c, t, px / 2f, px * 0.62f, 1.3f, 0, 0.25f, barTop = 0f)
        else -> painter.draw(c, t, px / 2f, px / 2f, 1.6f, 0, 0.25f)
    }
    return bmp.asImageBitmap()
}
