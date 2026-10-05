package com.zainkhalid.animebattery.ui

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.BackHeader
import com.zainkhalid.animebattery.ui.kit.PopButton
import com.zainkhalid.animebattery.ui.kit.PopCard
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.PopTile
import com.zainkhalid.animebattery.ui.kit.hardShadow
import com.zainkhalid.animebattery.wallpaper.WallpaperArt
import com.zainkhalid.animebattery.widget.BatteryBuddyReceiver
import com.zainkhalid.animebattery.widget.BatteryBuddyWidget
import com.zainkhalid.animebattery.widget.WidgetArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ── Wallpapers ────────────────────────────────────────────────────────────

@Composable
fun WallpaperScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf(WallpaperArt.Design.RasenganLens) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Real screen size and lens position, so the art lines up with the camera.
    val bounds = remember { context.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds }
    val cut = remember(view) { view.rootWindowInsets?.displayCutout?.boundingRectTop }
    val camX = cut?.takeUnless { it.isEmpty }?.exactCenterX() ?: (bounds.width() / 2f)
    val camY = cut?.takeUnless { it.isEmpty }?.let { it.top + it.width() / 2f } ?: (24f * context.resources.displayMetrics.density)
    val camR = cut?.takeUnless { it.isEmpty }?.let { it.width() * 0.32f } ?: (12f * context.resources.displayMetrics.density)
    val characterId = remember { AppSettings(context).characterId }

    LaunchedEffect(chosen) {
        preview = null
        preview = withContext(Dispatchers.Default) {
            WallpaperArt.render(context, chosen, bounds.width(), bounds.height(), camX, camY, camR, characterId)
        }
    }

    fun set(which: Int, label: String) {
        val bmp = preview ?: return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { WallpaperManager.getInstance(context).setBitmap(bmp, null, true, which) }.isSuccess
            }
            busy = false
            status = if (ok) "Set on $label" else "Couldn't set the wallpaper"
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Pop.Gap)) {
        BackHeader("Wallpapers", onClose)
        Text(
            "Drawn round your camera, so the lens becomes part of the art.",
            style = MaterialTheme.typography.bodyLarge, color = Pop.TextDim,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Pop.GapSmall)) {
            WallpaperArt.Design.entries.forEach { d ->
                PopTile(d == chosen, onClick = { chosen = d }, Modifier.weight(1f), accent = Pop.Sky) {
                    Text(d.title, style = MaterialTheme.typography.titleSmall, color = Pop.Text)
                    Text(d.blurb, style = MaterialTheme.typography.bodySmall, color = Pop.TextDim)
                }
            }
        }
        val shape = RoundedCornerShape(32.dp)
        Box(
            Modifier.fillMaxWidth(0.72f).align(Alignment.CenterHorizontally)
                .aspectRatio(bounds.width().toFloat() / bounds.height())
                .hardShadow(32.dp, dx = 3.dp, dy = Pop.ShadowCard)
                .clip(shape).background(Pop.Surface).border(Pop.Outline, Pop.Ink, shape),
            contentAlignment = Alignment.Center,
        ) {
            val p = preview
            if (p == null) CircularProgressIndicator(color = Pop.Pink)
            else Image(p.asImageBitmap(), contentDescription = "${chosen.title} preview", contentScale = ContentScale.Crop)
        }
        val ready = preview != null && !busy
        Row(horizontalArrangement = Arrangement.spacedBy(Pop.GapSmall)) {
            PopButton("Home", onClick = { set(WallpaperManager.FLAG_SYSTEM, "home screen") }, enabled = ready, modifier = Modifier.weight(1f))
            PopButton("Lock", onClick = { set(WallpaperManager.FLAG_LOCK, "lock screen") }, enabled = ready, color = Pop.Sky, modifier = Modifier.weight(1f))
        }
        PopButton(
            "Both screens", onClick = { set(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "both screens") },
            enabled = ready, color = Pop.Sun, icon = PopIcons.Sparkle, modifier = Modifier.fillMaxWidth(),
        )
        status?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = Pop.Lime) }
    }
}

// ── Widgets ───────────────────────────────────────────────────────────────

@Composable
fun WidgetsScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val d = context.resources.displayMetrics.density
    val characterId = remember { AppSettings(context).characterId }
    val (level, state) = remember { BatteryBuddyWidget.readBattery(context) }
    var note by remember { mutableStateOf<String?>(null) }

    @Composable
    fun Preview(label: String, wDp: Int, hDp: Int) {
        val bmp = remember(wDp, hDp) { WidgetArt.render(context, characterId, (wDp * d).roundToInt(), (hDp * d).roundToInt(), d, level, state) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Pop.TextDim)
            Image(bmp.asImageBitmap(), contentDescription = "$label widget preview", modifier = Modifier.size(wDp.dp, hDp.dp))
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Pop.Gap)) {
        BackHeader("Widget", onClose)
        Text("One widget, three looks: resize it on your home screen to switch.", style = MaterialTheme.typography.bodyLarge, color = Pop.TextDim)
        PopCard(color = Pop.SurfaceHigh) {
            Preview("4×2 · battery buddy", 320, 150)
            Row(horizontalArrangement = Arrangement.spacedBy(Pop.Gap)) {
                Preview("2×2 · mood", 150, 150)
                Preview("2×1 · pill", 150, 70)
            }
        }
        PopButton(
            "Add to home screen",
            onClick = {
                val mgr = AppWidgetManager.getInstance(context)
                val ok = mgr.isRequestPinAppWidgetSupported &&
                    mgr.requestPinAppWidget(ComponentName(context, BatteryBuddyReceiver::class.java), null, null)
                note = if (ok) "Check your home screen to place it." else "Long-press your home screen → Widgets → Anime Battery."
            },
            icon = PopIcons.Grid, modifier = Modifier.fillMaxWidth(),
        )
        note?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = Pop.Lime) }
    }
}
