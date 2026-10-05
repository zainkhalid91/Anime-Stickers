package com.zainkhalid.animebattery.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zainkhalid.animebattery.cast.Cast
import com.zainkhalid.animebattery.overlay.OverlayHealth
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.IconBadge
import com.zainkhalid.animebattery.ui.kit.PopButton
import com.zainkhalid.animebattery.ui.kit.PopCard
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.PopTag
import com.zainkhalid.animebattery.ui.kit.PopToggle
import com.zainkhalid.animebattery.ui.kit.SectionTitle

/** Settings, setup and the dev lab. */
@Composable
fun MeScreen(open: (Sub) -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var overlay by remember { mutableStateOf(OverlayHealth.status(context)) }
    val serviceOn = overlay != OverlayHealth.Status.Off
    var hideOnLock by remember { mutableStateOf(settings.hideOnLockScreen) }
    var showPercent by remember { mutableStateOf(settings.showPercent) }
    LifecycleResumeEffect(Unit) {
        overlay = OverlayHealth.status(context)
        onPauseOrDispose { }
    }
    val character = remember { Cast.byId(settings.characterId) }

    Text("Me", style = MaterialTheme.typography.displaySmall, color = Pop.Text)

    SectionTitle("Overlay")
    PopCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(PopIcons.Power, if (overlay == OverlayHealth.Status.Running) Pop.Lime else Pop.Coral)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (overlay) {
                        OverlayHealth.Status.Running -> "Overlay is on"
                        OverlayHealth.Status.Stopped -> "Overlay stopped (crash)"
                        OverlayHealth.Status.Off -> "Overlay is off"
                    },
                    style = MaterialTheme.typography.titleMedium, color = Pop.Text)
                Text("Runs as an accessibility service", style = MaterialTheme.typography.bodyMedium, color = Pop.TextDim)
            }
        }
        PopButton(
            if (serviceOn) "Accessibility settings" else "Turn it on",
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            color = if (serviceOn) Pop.SurfaceHigh else Pop.Pink, contentColor = if (serviceOn) Pop.Text else Pop.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    SectionTitle("Preferences")
    PopCard {
        PopToggle("Hide on lock screen", "Only show once you've unlocked", hideOnLock) { hideOnLock = it; settings.hideOnLockScreen = it }
        PopToggle("Percent in badge mode", "Number on the battery badge", showPercent) { showPercent = it; settings.showPercent = it }
    }

    SectionTitle("Nerd stuff")
    PopCard(onClick = { open(Sub.Lab) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(PopIcons.Flask, Pop.Sky)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Lab", style = MaterialTheme.typography.titleMedium, color = Pop.Text)
                Text("Experiments and the badge preview", style = MaterialTheme.typography.bodyMedium, color = Pop.TextDim)
            }
            IconBadge(PopIcons.Chevron, Pop.SurfaceHigh, size = 36.dp)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        PopTag("personal build", Pop.Lilac)
        Spacer(Modifier.width(8.dp))
        Text(
            if (character.series == "Original") "${character.name} is an original character." else "${character.name} © ${character.rights}. Fan art.",
            style = MaterialTheme.typography.bodySmall, color = Pop.TextDim,
        )
    }
}
