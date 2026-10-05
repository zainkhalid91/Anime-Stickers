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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.zainkhalid.animebattery.battery.BatterySnapshot
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.battery.BatteryStateMachine
import com.zainkhalid.animebattery.overlay.OverlayHealth
import com.zainkhalid.animebattery.overlay.StatusOverlayService
import com.zainkhalid.animebattery.ui.kit.MoodBadge
import kotlin.math.roundToInt
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
    // Battery test: a pretend battery shown here and on the real status bar.
    var test by remember { mutableStateOf(StatusOverlayService.instance?.testing) }
    val testState = test?.let { BatteryStateMachine().update(it) }
    val mood = Mood.of(testState ?: live.state)
    val level = test?.level ?: live.level
    fun setTest(t: BatterySnapshot?) {
        test = t
        StatusOverlayService.instance?.testBattery(t)
    }
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

    MasterSwitch(on = !paused) {
        paused = !it
        settings.paused = !it
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
        level = level,
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

    // Try every mood: pretend battery, on the stage and the real status bar.
    SectionTitle("Battery test") {
        Text(if (test == null) "real battery" else "testing", style = MaterialTheme.typography.labelMedium, color = if (test == null) Pop.TextDim else Pop.Sun)
    }
    PopCard {
        // Start from the real battery, so the chips match what's going on.
        val t = test ?: BatterySnapshot(
            live.level,
            plugged = live.state == BatteryState.Charging || live.state == BatteryState.Charged,
            temperatureC = if (live.state == BatteryState.Hot) 44f else 30f,
            powerSave = live.state == BatteryState.PowerSaver,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${t.level}%", style = MaterialTheme.typography.headlineMedium, color = Pop.Text, modifier = Modifier.weight(1f))
            MoodBadge(mood)
        }
        PopSlider(t.level.toFloat(), 0f..100f) { setTest(t.copy(level = it.roundToInt())) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PopChip("Charging", t.plugged, { setTest(t.copy(plugged = !t.plugged, full = false)) }, accent = Pop.Lime, dot = Pop.Lime)
            PopChip("Hot", t.temperatureC >= 42f, { setTest(t.copy(temperatureC = if (t.temperatureC >= 42f) 30f else 44f)) }, accent = Pop.Coral, dot = Pop.Coral)
            PopChip("Power saver", t.powerSave, { setTest(t.copy(powerSave = !t.powerSave)) }, accent = Pop.Lilac, dot = Pop.Lilac)
        }
        Text(
            if (StatusOverlayService.instance == null) "Turn the overlay on to see this in your status bar too."
            else "Your status bar shows this too. It goes back to the real battery by itself after 10 minutes.",
            style = MaterialTheme.typography.bodySmall, color = Pop.TextDim,
        )
        if (test != null) {
            PopButton("Back to real battery", onClick = { setTest(null) }, color = Pop.SurfaceHigh, contentColor = Pop.Text, modifier = Modifier.fillMaxWidth())
        }
    }

    // The real bar, same view as the overlay.
    SectionTitle("Your status bar")
    PopCard(contentPadding = 0.dp, onClick = { go(Tab.Studio) }) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            BarPreview(
                layout, Modifier.fillMaxWidth().height(150.dp),
                background = 0xFF1A1426.toInt(),
                level = level, state = testState ?: live.state,
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

/** The big on/off for everything the app shows. Also on the Quick Settings tile. */
@Composable
private fun MasterSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    PopCard(color = if (on) Pop.Lime else Pop.Coral, sticker = true, onClick = { onChange(!on) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (on) "Anime Battery is on" else "Anime Battery is off", style = MaterialTheme.typography.titleLarge, color = Pop.Ink)
                Text(
                    if (on) "Tap to hide everything right away. Tip: add the Quick Settings tile to do it from anywhere."
                    else "Nothing is showing. Tap to bring your buddy back.",
                    style = MaterialTheme.typography.bodySmall, color = Pop.Ink,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = on, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Pop.Lime, checkedTrackColor = Pop.Ink, checkedBorderColor = Pop.Ink,
                    uncheckedThumbColor = Pop.Coral, uncheckedTrackColor = Pop.Ink, uncheckedBorderColor = Pop.Ink,
                ),
            )
        }
    }
}
