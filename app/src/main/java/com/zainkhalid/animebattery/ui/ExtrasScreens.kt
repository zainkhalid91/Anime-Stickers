package com.zainkhalid.animebattery.ui

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.settings.AppSettings
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

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Back") }
            Text("Camera-hole wallpapers", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Drawn round your camera, so the lens becomes part of the art.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            WallpaperArt.Design.entries.forEach { d ->
                val sel = d == chosen
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .clickable { chosen = d }.padding(12.dp),
                ) {
                    Text(d.title, style = MaterialTheme.typography.titleMedium)
                    Text(d.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Box(
            Modifier.fillMaxWidth(0.72f).align(Alignment.CenterHorizontally)
                .aspectRatio(bounds.width().toFloat() / bounds.height())
                .clip(RoundedCornerShape(28.dp)).border(3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                .background(Color(0xFF0B1020)),
            contentAlignment = Alignment.Center,
        ) {
            val p = preview
            if (p == null) CircularProgressIndicator()
            else Image(p.asImageBitmap(), contentDescription = "${chosen.title} preview", contentScale = ContentScale.Crop)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            Button(onClick = { set(WallpaperManager.FLAG_SYSTEM, "home screen") }, enabled = preview != null && !busy, modifier = Modifier.weight(1f)) { Text("Home screen") }
            OutlinedButton(onClick = { set(WallpaperManager.FLAG_LOCK, "lock screen") }, enabled = preview != null && !busy, modifier = Modifier.weight(1f)) { Text("Lock screen") }
        }
        OutlinedButton(
            onClick = { set(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "both screens") },
            enabled = preview != null && !busy, modifier = Modifier.fillMaxWidth(),
        ) { Text("Both") }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(Tokens.Gap))
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
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Image(bmp.asImageBitmap(), contentDescription = "$label widget preview", modifier = Modifier.size(wDp.dp, hDp.dp))
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Back") }
            Text("Home screen widget", style = MaterialTheme.typography.headlineSmall)
        }
        Text("One widget, three looks: resize it on your home screen to switch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2A5C)),
            border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
                Preview("4×2 · Battery buddy", 320, 150)
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
                    Preview("2×2 · Mood", 150, 150)
                    Preview("2×1 · Pill", 150, 70)
                }
            }
        }
        Button(
            onClick = {
                val mgr = AppWidgetManager.getInstance(context)
                val ok = mgr.isRequestPinAppWidgetSupported &&
                    mgr.requestPinAppWidget(ComponentName(context, BatteryBuddyReceiver::class.java), null, null)
                note = if (ok) "Check your home screen to place it." else "Long-press your home screen → Widgets → Anime Battery."
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Add to home screen") }
        note?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(Tokens.Gap))
    }
}
