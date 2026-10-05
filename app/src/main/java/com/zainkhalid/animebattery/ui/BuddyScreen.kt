package com.zainkhalid.animebattery.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.zainkhalid.animebattery.cast.Cast
import com.zainkhalid.animebattery.cast.CustomCast
import com.zainkhalid.animebattery.cast.castName
import com.zainkhalid.animebattery.render.StickerCache
import com.zainkhalid.animebattery.cast.Event
import com.zainkhalid.animebattery.cast.Look
import com.zainkhalid.animebattery.cast.look
import com.zainkhalid.animebattery.ui.kit.PopTile
import com.zainkhalid.animebattery.ui.kit.stickerPainter
import com.zainkhalid.animebattery.decor.DecorType
import com.zainkhalid.animebattery.decor.Lines
import com.zainkhalid.animebattery.decor.Mood
import com.zainkhalid.animebattery.overlay.OverlayHealth
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.CharacterStage
import com.zainkhalid.animebattery.ui.kit.IconBadge
import com.zainkhalid.animebattery.ui.kit.PopButton
import com.zainkhalid.animebattery.ui.kit.PopCard
import com.zainkhalid.animebattery.ui.kit.PopChip
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.PopToggle
import com.zainkhalid.animebattery.ui.kit.SectionTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home. The character, big and alive, reacting to the real battery. Poke it, try
 * the other moods, see your status bar, flip the main switches.
 */
@Composable
fun BuddyScreen(go: (Tab) -> Unit, open: (Sub) -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var characterId by remember { mutableStateOf(settings.characterId) }
    val name = castName(context, characterId)
    val mine = remember { CustomCast.list(context) }
    // Brief pose after a poke on the stage.
    var pokeLook by remember { mutableStateOf<Look?>(null) }
    val live = rememberLiveBattery()
    var preview by remember { mutableStateOf<Mood?>(null) }
    val mood = preview ?: Mood.of(live.state)
    var layout by remember { mutableStateOf(settings.barLayout) }
    var overlay by remember { mutableStateOf(OverlayHealth.status(context)) }
    val scope = rememberCoroutineScope()
    var restarting by remember { mutableStateOf(false) }
    var speech by remember { mutableStateOf(settings.speech) }
    var animations by remember { mutableStateOf(settings.animations) }
    var paused by remember { mutableStateOf(settings.paused) }
    var touch by remember { mutableStateOf(settings.touchBuddy) }
    LifecycleResumeEffect(Unit) {
        overlay = OverlayHealth.status(context)
        layout = settings.barLayout
        onPauseOrDispose { }
    }

    // A line whenever the mood changes, and on every poke; each one fades after a bit.
    var bubble by remember { mutableStateOf<String?>(null) }
    var said by remember { mutableIntStateOf(0) }
    fun say(text: String) { bubble = text; said++ }
    LaunchedEffect(mood, characterId) { say(Lines.forMood(mood, characterId)) }
    LaunchedEffect(pokeLook) {
        if (pokeLook != null) { delay(900); pokeLook = null }
    }
    LaunchedEffect(said) {
        delay(3500)
        bubble = null
    }

    // Header.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("your buddy", style = MaterialTheme.typography.labelLarge, color = Pop.Pink)
            Text(name, style = MaterialTheme.typography.displaySmall, color = Pop.Text)
        }
        IconBadge(
            PopIcons.Pencil, Pop.Sun,
            Modifier.clip(CircleShape).clickable(onClickLabel = "Customise in Studio") { go(Tab.Studio) },
            size = 48.dp,
        )
    }

    if (overlay == OverlayHealth.Status.Stopped) {
        PopCard(color = Pop.Coral, sticker = true) {
            Text("$name fell out of your status bar", style = MaterialTheme.typography.titleLarge, color = Pop.Ink)
            Text(
                if (OverlayHealth.canRestart(context)) "The app crashed and Android stopped the overlay. It's still switched on, but Android won't run it again until it's restarted."
                else "The app crashed and Android stopped the overlay. In Accessibility, open Anime Battery overlay, switch it off, then on again.",
                style = MaterialTheme.typography.bodyMedium, color = Pop.Ink,
            )
            if (OverlayHealth.canRestart(context)) {
                PopButton(
                    if (restarting) "Restarting…" else "Restart overlay", enabled = !restarting,
                    onClick = {
                        restarting = true
                        scope.launch {
                            OverlayHealth.restart(context)
                            overlay = OverlayHealth.status(context)
                            restarting = false
                        }
                    },
                    color = Pop.Ink, contentColor = Pop.Coral, icon = PopIcons.Power, modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PopButton(
                    "Open Accessibility", onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    color = Pop.Ink, contentColor = Pop.Coral, icon = PopIcons.Power, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (overlay == OverlayHealth.Status.Off) {
        PopCard(color = Pop.Sun, sticker = true) {
            Text("Let $name into your status bar", style = MaterialTheme.typography.titleLarge, color = Pop.Ink)
            Text(
                "Android only lets accessibility windows draw over the status bar. Open Accessibility, tap Anime Battery overlay and switch it on.",
                style = MaterialTheme.typography.bodyMedium, color = Pop.Ink,
            )
            PopButton(
                "Open Accessibility", onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                color = Pop.Ink, contentColor = Pop.Sun, icon = PopIcons.Power, modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (live.stuck) StuckBatteryCard(real = live.level, shown = live.systemLevel)

    CharacterStage(
        characterId = characterId,
        look = pokeLook ?: mood.look(hanging = false),
        mood = mood,
        level = live.level,
        bubble = bubble,
        onPoke = {
            say(Lines.on(Event.Poke, characterId))
            pokeLook = if (System.nanoTime() % 3 == 0L) Look.Hurt else Look.Grabbed
        },
        witchHat = layout.decor.any { it.type == DecorType.WitchHat },
        animate = animations,
    )

    // Who lives in your status bar.
    SectionTitle("Your character")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Only characters whose art has been made (tools/gen_cast.py) show up.
        Cast.all.filter { StickerCache.res(context, it.id) != 0 }.forEach { c ->
            PopTile(c.id == characterId, onClick = {
                characterId = c.id
                settings.characterId = c.id
            }, Modifier.width(92.dp), accent = Color(c.accent)) {
                Image(stickerPainter(c.id), contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(6.dp))
                Text(c.name, style = MaterialTheme.typography.labelLarge, color = Pop.Text, maxLines = 1)
                Text(c.series, style = MaterialTheme.typography.labelSmall, color = Pop.TextDim, maxLines = 1)
            }
        }
        mine.forEach { c ->
            PopTile(c.id == characterId, onClick = {
                characterId = c.id
                settings.characterId = c.id
            }, Modifier.width(92.dp), accent = Pop.Lilac) {
                Image(stickerPainter(c.id), contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(6.dp))
                Text(c.name, style = MaterialTheme.typography.labelLarge, color = Pop.Text, maxLines = 1)
                Text("yours", style = MaterialTheme.typography.labelSmall, color = Pop.TextDim, maxLines = 1)
            }
        }
        PopTile(false, onClick = { open(Sub.Make) }, Modifier.width(92.dp), accent = Pop.Lime) {
            IconBadge(PopIcons.Sparkle, Pop.Lime, size = 64.dp)
            Spacer(Modifier.height(6.dp))
            Text("Make", style = MaterialTheme.typography.labelLarge, color = Pop.Text, maxLines = 1)
            Text("from a photo", style = MaterialTheme.typography.labelSmall, color = Pop.TextDim, maxLines = 1)
        }
    }

    // Try the moods.
    SectionTitle("Moods") {
        Text(if (preview == null) "live" else "preview", style = MaterialTheme.typography.labelMedium, color = Pop.TextDim)
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PopChip("Live", selected = preview == null, onClick = { preview = null }, accent = Pop.Lime, dot = Pop.Lime)
        Mood.entries.filterNot { it.reactionOnly }.forEach { m ->
            PopChip(m.word, selected = preview == m, onClick = { preview = m }, accent = Color(m.color), dot = Color(m.color))
        }
    }

    // The real bar, same view as the overlay.
    SectionTitle("Your status bar")
    PopCard(contentPadding = 0.dp, onClick = { go(Tab.Studio) }) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            BarPreview(
                layout, Modifier.fillMaxWidth().height(150.dp),
                background = 0xFF1A1426.toInt(),
                level = live.level, state = preview?.let { Mood.sample(it) } ?: live.state,
                sparkles = settings.sparkles, animations = animations, characterId = characterId,
                speech = false,
            )
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Customise in Studio", style = MaterialTheme.typography.titleSmall, color = Pop.Text, modifier = Modifier.weight(1f))
            IconBadge(PopIcons.Chevron, Pop.Pink, size = 36.dp)
        }
    }

    // Main switches.
    PopCard {
        PopToggle("Show on status bar", if (paused) "Hidden for now" else "Your buddy is out", !paused) {
            paused = !it; settings.paused = !it
        }
        PopToggle("Speech bubbles", "Says something when the mood changes or you unlock", speech) {
            speech = it; settings.speech = it
        }
        PopToggle("Animations", "Off saves a little battery", animations) {
            animations = it; settings.animations = it
        }
        PopToggle("Play with your buddy", "Tap, drag, flick or hold the hanging character in the status bar", touch) {
            touch = it; settings.touchBuddy = it
        }
    }
}

/**
 * Android's battery reading is frozen (a `dumpsys battery` test was never reset), so the
 * phone's own status bar, low-battery warning and auto-shutdown use an old number.
 * Only adb (or a restart) can undo it; apps aren't allowed to.
 */
@Composable
private fun StuckBatteryCard(real: Int, shown: Int) {
    PopCard(color = Pop.Coral, sticker = true) {
        Text("Android's battery reading is stuck", style = MaterialTheme.typography.titleLarge, color = Pop.Ink)
        Text(
            "Your phone's own battery icon says $shown%, but the battery is really at $real%. " +
                "Low-battery warnings won't come either. This is left over from an adb battery test.",
            style = MaterialTheme.typography.bodyMedium, color = Pop.Ink,
        )
        Text("Fix it from your computer, or restart the phone:", style = MaterialTheme.typography.labelLarge, color = Pop.Ink)
        Text(
            "adb shell dumpsys battery reset",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            color = Pop.Text,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Pop.Ink).padding(12.dp),
        )
    }
}
