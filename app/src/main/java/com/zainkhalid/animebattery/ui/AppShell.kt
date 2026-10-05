package com.zainkhalid.animebattery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.zainkhalid.animebattery.characters.Characters
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.BackHeader
import com.zainkhalid.animebattery.ui.kit.PopSegmented
import com.zainkhalid.animebattery.ui.lab.LabScreen
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.hardShadow

/** The four tabs (docs/ROADMAP.md §2.7). */
enum class Tab(val label: String, val icon: ImageVector) {
    Buddy("Buddy", PopIcons.Buddy),
    Studio("Studio", PopIcons.Studio),
    Drops("Drops", PopIcons.Drops),
    Me("Me", PopIcons.Me),
}

/** Pages opened from inside a tab. */
enum class Sub { None, Wallpapers, Widgets, Lab, Make }

/**
 * The whole app: a tab at a time, plus an optional sub-page, with the floating
 * bottom bar on top. Back closes a sub-page, then returns to Buddy.
 */
@Composable
fun AppShell() {
    var tab by rememberSaveable { mutableStateOf(Tab.Buddy) }
    var sub by rememberSaveable { mutableStateOf(Sub.None) }
    BackHandler(enabled = sub != Sub.None || tab != Tab.Buddy) {
        if (sub != Sub.None) sub = Sub.None else tab = Tab.Buddy
    }
    val go: (Tab) -> Unit = { tab = it; sub = Sub.None }
    val open: (Sub) -> Unit = { sub = it }
    val close = { sub = Sub.None }

    Box(Modifier.fillMaxSize().background(Pop.Bg)) {
        AnimatedContent(
            targetState = tab to sub,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 }).togetherWith(fadeOut(tween(140)))
            },
            label = "page",
        ) { (t, s) ->
            Page {
                when {
                    s == Sub.Wallpapers -> WallpaperScreen(onClose = close)
                    s == Sub.Widgets -> WidgetsScreen(onClose = close)
                    s == Sub.Lab -> LabPage(onClose = close)
                    s == Sub.Make -> MakeScreen(onClose = close)
                    t == Tab.Buddy -> BuddyScreen(go = go, open = open)
                    t == Tab.Studio -> StudioScreen()
                    t == Tab.Drops -> DropsScreen(open = open)
                    t == Tab.Me -> MeScreen(open = open)
                }
            }
        }
        PopBottomBar(tab, onSelect = go, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Scrolling page with the gutters, room under the status bar, and room for the bottom bar. */
@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Pop.Gutter).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Pop.Gap),
    ) {
        content()
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(112.dp))
    }
}

@Composable
private fun PopBottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier.windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
            .hardShadow(28.dp, dy = 4.dp)
            .clip(shape).background(Pop.Surface).border(Pop.Outline, Pop.Ink, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { t ->
            val sel = t == selected
            val bg by animateColorAsState(if (sel) Pop.Pink else Pop.Surface, label = "tabBg")
            val fg by animateColorAsState(if (sel) Pop.Ink else Pop.TextDim, label = "tabFg")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(bg)
                    .then(if (sel) Modifier.border(2.dp, Pop.Ink, RoundedCornerShape(22.dp)) else Modifier)
                    .clickable { onSelect(t) }
                    .semantics { role = Role.Tab; this.selected = sel }
                    .heightIn(min = 56.dp).padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(t.icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = fg)
            }
        }
    }
}

/** Dev tools from the old home screen, kept under Me. */
@Composable
private fun LabPage(onClose: () -> Unit) {
    val context = LocalContext.current
    var labTab by rememberSaveable { mutableStateOf(0) }
    BackHeader("Lab", onClose)
    PopSegmented(listOf(0 to "Ideas", 1 to "Badge preview"), labTab, { labTab = it })
    if (labTab == 0) LabScreen()
    else PreviewScreen(Characters.byId(AppSettings(context).characterId), showPercent = true, size = 1f)
}
