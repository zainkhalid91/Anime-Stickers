package com.zainkhalid.animebattery.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zainkhalid.animebattery.decor.ThemeSet
import com.zainkhalid.animebattery.decor.ThemeSets
import androidx.compose.ui.graphics.painter.Painter
import com.zainkhalid.animebattery.cast.Look
import com.zainkhalid.animebattery.ui.kit.stickerPainter
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.IconBadge
import com.zainkhalid.animebattery.ui.kit.PopButton
import com.zainkhalid.animebattery.ui.kit.PopCard
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.PopTag
import com.zainkhalid.animebattery.ui.kit.SectionTitle
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Ready-made looks (with seasonal drops on top), plus wallpapers and the widget. */
@Composable
fun DropsScreen(open: (Sub) -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var applied by remember { mutableStateOf(settings.themeId) }
    LifecycleResumeEffect(Unit) {
        applied = settings.themeId
        onPauseOrDispose { }
    }
    val today = remember { LocalDate.now() }
    val month = today.monthValue
    val sets = remember(month) { ThemeSets.ordered(month) }
    val sticker = stickerPainter(settings.characterId, Look.Cheer)
    fun apply(t: ThemeSet) {
        settings.barLayout = t.layout
        settings.themeId = t.id
        settings.barMode = AppSettings.MODE_FULL
        applied = t.id
    }

    Text("Drops", style = MaterialTheme.typography.displaySmall, color = Pop.Text)
    Text("New looks for your bar. Seasonal ones don't stick around.", style = MaterialTheme.typography.bodyLarge, color = Pop.TextDim)

    // Live seasonal drop, big.
    sets.firstOrNull { it.inSeason(month) }?.let { t ->
        PopCard(color = Color(t.colors.second), sticker = true, contentPadding = 0.dp) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 200.dp)
                    .background(Brush.linearGradient(listOf(Color(t.colors.first), Color(t.colors.second)))),
            ) {
                Image(
                    sticker, contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).size(150.dp),
                )
                Column(
                    Modifier.align(Alignment.TopStart).fillMaxWidth(0.62f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PopTag("limited · ${today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase()}", Pop.Sun, icon = PopIcons.Sparkle)
                    Text(t.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(t.blurb, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                PopButton(
                    if (applied == t.id) "On your bar" else "Get the ${t.name} look",
                    onClick = { apply(t) }, color = if (applied == t.id) Pop.Lime else Pop.Sun,
                    icon = if (applied == t.id) PopIcons.Check else PopIcons.Sparkle, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    SectionTitle("Collections")
    sets.filterNot { it.inSeason(month) }.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { t -> CollectionTile(t, sticker, applied == t.id, Modifier.weight(1f)) { apply(t) } }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    SectionTitle("More")
    ExtraRow("Camera-hole wallpapers", "The lens becomes part of the art", PopIcons.Image, Pop.Sky) { open(Sub.Wallpapers) }
    ExtraRow("Home screen widget", "Your buddy, three sizes", PopIcons.Grid, Pop.Lilac) { open(Sub.Widgets) }
}

@Composable
private fun CollectionTile(t: ThemeSet, sticker: Painter, applied: Boolean, modifier: Modifier, onApply: () -> Unit) {
    PopCard(modifier, color = Color(t.colors.second), sticker = true, onClick = onApply, contentPadding = 0.dp) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 190.dp)
                .background(Brush.linearGradient(listOf(Color(t.colors.first), Color(t.colors.second))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Image(sticker, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.weight(1f))
                if (applied) PopTag("on", Pop.Lime, icon = PopIcons.Check)
            }
            Spacer(Modifier.height(4.dp))
            Text(t.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(t.blurb, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.88f), maxLines = 3)
        }
    }
}

@Composable
private fun ExtraRow(title: String, sub: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    PopCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Pop.Text)
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = Pop.TextDim)
            }
            IconBadge(PopIcons.Chevron, Pop.SurfaceHigh, size = 36.dp)
        }
    }
}
