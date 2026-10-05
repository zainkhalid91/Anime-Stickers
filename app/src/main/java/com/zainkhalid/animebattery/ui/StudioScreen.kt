package com.zainkhalid.animebattery.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.decor.Decor
import com.zainkhalid.animebattery.decor.DecorPainter
import com.zainkhalid.animebattery.decor.DecorType
import com.zainkhalid.animebattery.decor.Pose
import com.zainkhalid.animebattery.cast.Look
import com.zainkhalid.animebattery.ui.kit.stickerPainter
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.PopButton
import com.zainkhalid.animebattery.ui.kit.PopCard
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.PopSegmented
import com.zainkhalid.animebattery.ui.kit.PopTag
import com.zainkhalid.animebattery.ui.kit.PopTile
import com.zainkhalid.animebattery.ui.kit.PopToggle
import com.zainkhalid.animebattery.ui.kit.SectionTitle
import com.zainkhalid.animebattery.ui.kit.hardShadow
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Bar canvas is shown at this zoom so small stickers are easy to grab. */
private const val ZOOM = 2f

/**
 * Build your bar: drag stickers round, pinch to resize, pick the pose and size.
 * Saves by itself a moment after you stop, and the overlay follows straight away.
 */
@Composable
fun StudioScreen() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var layout by remember { mutableStateOf(settings.barLayout) }
    var selected by remember { mutableIntStateOf(-1) }
    var mode by remember { mutableStateOf(settings.barMode) }
    var sparkles by remember { mutableStateOf(settings.sparkles) }
    var saved by remember { mutableStateOf(true) }
    val density = LocalDensity.current.density
    val characterId = remember { settings.characterId }

    LaunchedEffect(layout) {
        if (layout == settings.barLayout) return@LaunchedEffect
        saved = false
        delay(400)
        settings.barLayout = layout
        settings.themeId = null
        if (mode != AppSettings.MODE_FULL) { mode = AppSettings.MODE_FULL; settings.barMode = mode }
        saved = true
    }
    fun edit(new: BarLayout) { layout = new }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Studio", style = MaterialTheme.typography.displaySmall, color = Pop.Text, modifier = Modifier.weight(1f))
        PopTag(if (saved) "saved" else "saving…", if (saved) Pop.Lime else Pop.SurfaceHigh, icon = if (saved) PopIcons.Check else null)
    }

    // ── Canvas ────────────────────────────────────────────────────────
    val shape = RoundedCornerShape(Pop.RadiusCard)
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(end = 3.dp, bottom = Pop.ShadowCard)
            .hardShadow(Pop.RadiusCard, dx = 3.dp, dy = Pop.ShadowCard)
            .height(230.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1426), Color(0xFF3A2357))))
            .border(Pop.Outline, Pop.Ink, shape),
    ) {
        val cw = constraints.maxWidth.toFloat()
        val latest by rememberUpdatedState(layout)
        val sel by rememberUpdatedState(selected)
        // Bar coordinates <-> canvas coordinates (zoomed round the top centre).
        fun toBar(o: Offset) = Offset((o.x - cw / 2f) / ZOOM + cw / 2f, o.y / ZOOM)
        fun decorAt(p: Offset): Int {
            val base = 16f * density
            return latest.decor.indexOfFirst { dd ->
                !dd.type.attached && hypot(dd.x * cw - p.x, dd.y * density - p.y) < base * dd.scale + 10f * density
            }
        }

        BarPreview(
            layout = layout, background = 0xFF1A1426.toInt(), characterId = characterId, sparkles = sparkles,
            modifier = Modifier.requiredWidth(maxWidth).height(115.dp)
                .graphicsLayer(scaleX = ZOOM, scaleY = ZOOM, transformOrigin = TransformOrigin(0.5f, 0f)),
        )
        // Selection ring + gestures on top.
        Canvas(
            Modifier.fillMaxSize()
                .semantics { contentDescription = "Status bar editor. Tap a sticker to select it, drag to move, pinch to resize." }
                .pointerInput(Unit) { detectTapGestures { o -> selected = decorAt(toBar(o)) } }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val i = sel.takeIf { it in latest.decor.indices } ?: decorAt(toBar(centroid)).also { selected = it }
                        if (i < 0) {
                            // Pinch on empty space resizes the character.
                            if (zoom != 1f) edit(latest.copy(characterSizeDp = (latest.characterSizeDp * zoom).coerceIn(24f, 72f)))
                            return@detectTransformGestures
                        }
                        val dd = latest.decor[i]
                        val moved = dd.copy(
                            x = (dd.x + pan.x / ZOOM / cw).coerceIn(0f, 1f),
                            y = (dd.y + pan.y / ZOOM / density).coerceIn(-4f, 110f),
                            scale = (dd.scale * zoom).coerceIn(0.5f, 3f),
                        )
                        edit(latest.copy(decor = latest.decor.toMutableList().also { it[i] = moved }))
                    }
                },
        ) {
            val i = selected
            if (i >= 0 && i < layout.decor.size) {
                val dd = layout.decor[i]
                val c = Offset((dd.x * cw - cw / 2f) * ZOOM + cw / 2f, dd.y * density * ZOOM)
                drawCircle(
                    Pop.Lime, radius = (16f * density * dd.scale + 8f * density) * ZOOM, center = c,
                    style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
        }
        PopTag("tap · drag · pinch", Pop.Surface.copy(alpha = 0.9f), Modifier.align(Alignment.BottomStart).padding(12.dp))
    }

    // ── Selected sticker ──────────────────────────────────────────────
    if (selected in layout.decor.indices) {
        val dd = layout.decor[selected]
        PopCard(color = Pop.SurfaceHigh) {
            Text("${dd.type.label} selected", style = MaterialTheme.typography.titleMedium, color = Pop.Text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size", style = MaterialTheme.typography.labelLarge, color = Pop.TextDim, modifier = Modifier.width(48.dp))
                PopSlider(dd.scale, 0.5f..3f, Modifier.weight(1f)) { v ->
                    edit(layout.copy(decor = layout.decor.toMutableList().also { it[selected] = dd.copy(scale = v) }))
                }
            }
            PopButton("Remove sticker", onClick = {
                edit(layout.copy(decor = layout.decor.filterIndexed { idx, _ -> idx != selected }))
                selected = -1
            }, color = Pop.Coral, modifier = Modifier.fillMaxWidth())
        }
    }

    // ── Pose ──────────────────────────────────────────────────────────
    SectionTitle("Pose")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pose.entries.forEach { p ->
            PopTile(layout.pose == p, onClick = { edit(layout.copy(pose = p)) }, Modifier.weight(1f)) {
                PoseIcon(p, characterId, Modifier.size(40.dp, 44.dp))
                Spacer(Modifier.height(6.dp))
                Text(poseLabel(p), style = MaterialTheme.typography.labelMedium, color = if (layout.pose == p) Pop.Text else Pop.TextDim)
            }
        }
    }
    if (layout.pose != Pose.Eyes) {
        PopCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Character size", style = MaterialTheme.typography.titleSmall, color = Pop.Text, modifier = Modifier.weight(1f))
                PopTag("${layout.characterSizeDp.roundToInt()}", Pop.Pink)
            }
            PopSlider(layout.characterSizeDp, 24f..72f) { edit(layout.copy(characterSizeDp = it)) }
            if (layout.pose == Pose.Hanging) {
                PopToggle("Sway", "Swings gently on the thread", layout.sway) { edit(layout.copy(sway = it)) }
            }
        }
    }

    // ── Sticker tray ──────────────────────────────────────────────────
    SectionTitle("Stickers") {
        if (layout.decor.isNotEmpty()) {
            Text(
                "Clear all", style = MaterialTheme.typography.labelLarge, color = Pop.Coral,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .clickable { edit(layout.copy(decor = emptyList())); selected = -1 }
                    .padding(8.dp),
            )
        }
    }
    val icons = remember(density) { DecorType.entries.associateWith { decorIcon(it, density) } }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DecorType.entries.forEach { t ->
            val has = t.attached && layout.decor.any { it.type == t }
            PopTile(has, onClick = {
                if (t.attached) {
                    // Worn things toggle on and off.
                    edit(if (has) layout.copy(decor = layout.decor.filter { it.type != t }) else layout.copy(decor = layout.decor + Decor(t, 0f, 0f)))
                    selected = -1
                } else {
                    // New stickers land near the camera, just under the bar.
                    val n = layout.decor.size
                    edit(layout.copy(decor = layout.decor + Decor(t, 0.38f + (n % 5) * 0.06f, 14f + (n % 3) * 6f)))
                    // edit() has already added it, so it's the last one.
                    selected = layout.decor.lastIndex
                }
            }, Modifier.width(78.dp), accent = Pop.Lime) {
                Image(icons.getValue(t), contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = Pop.TextDim, maxLines = 1)
            }
        }
    }

    // ── Bar ───────────────────────────────────────────────────────────
    SectionTitle("Bar")
    PopCard {
        PopSegmented(
            listOf(AppSettings.MODE_FULL to "Custom bar", AppSettings.MODE_BADGE to "Battery badge"), mode,
            { mode = it; settings.barMode = it },
        )
        PopToggle("Sparkles", "Little stars round the camera", sparkles) { sparkles = it; settings.sparkles = it }
    }
}

@Composable
fun PopSlider(value: Float, range: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
    Slider(
        value = value, valueRange = range, onValueChange = onChange, modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Pop.Pink, activeTrackColor = Pop.Pink, inactiveTrackColor = Pop.SurfaceHigh,
            activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
        ),
    )
}

fun poseLabel(p: Pose) = when (p) {
    Pose.Hanging -> "Hanging"
    Pose.Battery -> "Battery"
    Pose.Camera -> "Camera"
    Pose.Eyes -> "Eyes"
}

@Composable
private fun PoseIcon(p: Pose, characterId: String, modifier: Modifier) {
    when (p) {
        Pose.Eyes -> Canvas(modifier) {
            val r = size.minDimension * 0.24f
            for (side in listOf(-1, 1)) {
                val c = Offset(size.width / 2f + side * r * 1.1f, size.height / 2f)
                drawOval(Color.White, Offset(c.x - r * 0.8f, c.y - r), Size(r * 1.6f, r * 2f))
                drawCircle(Color(0xFF2E7FE6), r * 0.6f, c.copy(y = c.y + r * 0.15f))
                drawCircle(Pop.Ink, r * 0.3f, c.copy(y = c.y + r * 0.2f))
            }
        }
        else -> Box(modifier, contentAlignment = Alignment.TopCenter) {
            if (p == Pose.Hanging) Canvas(Modifier.size(2.dp, 14.dp)) { drawRect(Color.White.copy(alpha = 0.7f)) }
            Image(
                stickerPainter(characterId, if (p == Pose.Hanging) Look.Hang else Look.Idle), contentDescription = null,
                modifier = Modifier.padding(top = if (p == Pose.Hanging) 12.dp else 4.dp).size(if (p == Pose.Hanging) 30.dp else 36.dp),
            )
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
        DecorType.WitchHat -> painter.witchHat(c, px * 0.1f, px * 0.62f, px * 0.8f, px * 1.0f)
        DecorType.Web -> painter.draw(c, t, 0f, 0f, 0.8f, 0, 0.25f)
        DecorType.Spider -> painter.draw(c, t, px / 2f, px * 0.62f, 1.3f, 0, 0.25f, barTop = 0f)
        else -> painter.draw(c, t, px / 2f, px / 2f, 1.6f, 0, 0.25f)
    }
    return bmp.asImageBitmap()
}
