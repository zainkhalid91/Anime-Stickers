package com.zainkhalid.animebattery.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.ui.Pop

/** Springy, a little bouncy: the default feel for anything that moves on touch. */
fun <T> popSpring() = spring<T>(dampingRatio = 0.5f, stiffness = 700f)

/** A flat ink shadow offset down/right, drawn behind whatever comes next. */
fun Modifier.hardShadow(radius: Dp, dx: Dp = 0.dp, dy: Dp, color: Color = Pop.Ink): Modifier = drawBehind {
    drawRoundRect(color, topLeft = Offset(dx.toPx(), dy.toPx()), size = size, cornerRadius = CornerRadius(radius.toPx()))
}

/**
 * Chunky Duolingo-style button: an ink outline and a flat shadow it sinks into when
 * pressed. Text and icon are ink on the pop colour.
 */
@Composable
fun PopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Pop.Pink,
    contentColor: Color = Pop.Ink,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val depth = Pop.ShadowButton
    val sink by animateDpAsState(if (pressed && enabled) depth else 0.dp, popSpring(), label = "sink")
    val shape = RoundedCornerShape(Pop.RadiusTile)
    val fill = if (enabled) color else Pop.SurfaceHigh
    Box(
        modifier.padding(bottom = depth).hardShadow(Pop.RadiusTile, dy = depth)
            .semantics { role = Role.Button },
    ) {
        Row(
            Modifier.fillMaxWidth().offset(y = sink).clip(shape).background(fill)
                .border(Pop.Outline, Pop.Ink, shape)
                .clickable(interaction, indication = null, enabled = enabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                }
                .heightIn(min = 52.dp).padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ink = if (enabled) contentColor else Pop.TextDim
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = ink, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Card with a hard shadow. Dark cards get a thin [Pop.Line] outline; pop-coloured
 * ones ([sticker] = true) get the full ink outline so they read as stickers.
 */
@Composable
fun PopCard(
    modifier: Modifier = Modifier,
    color: Color = Pop.Surface,
    sticker: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Pop.Gap,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Pop.RadiusCard)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 3.dp else 0.dp, popSpring(), label = "cardSink")
    Column(
        modifier.padding(end = 3.dp, bottom = Pop.ShadowCard)
            .hardShadow(Pop.RadiusCard, dx = 3.dp, dy = Pop.ShadowCard)
            .offset(x = sink * 0.6f, y = sink)
            .clip(shape).background(color)
            .border(if (sticker) Pop.Outline else Pop.OutlineThin, if (sticker) Pop.Ink else Pop.Line, shape)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Pop.GapSmall + 2.dp),
        content = content,
    )
}

/** A selectable square-ish tile that pops when chosen. */
@Composable
fun PopTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Pop.Pink,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Pop.RadiusTile)
    val scale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            scale.snapTo(0.92f)
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
    }
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.22f) else Pop.SurfaceHigh, label = "tileBg")
    Column(
        modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .then(if (selected) Modifier.hardShadow(Pop.RadiusTile, dy = 3.dp) else Modifier)
            .clip(shape).background(bg)
            .border(if (selected) Pop.Outline else Pop.OutlineThin, if (selected) accent else Pop.Line, shape)
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton; this.selected = selected }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Pill chip. Selected: filled with [accent], ink text and outline. */
@Composable
fun PopChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Pop.Pink,
    dot: Color? = null,
) {
    val bg by animateColorAsState(if (selected) accent else Pop.SurfaceHigh, label = "chipBg")
    Row(
        modifier.clip(CircleShape).background(bg)
            .border(if (selected) 2.dp else Pop.OutlineThin, if (selected) Pop.Ink else Pop.Line, CircleShape)
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton; this.selected = selected }
            .heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot).border(1.5.dp, Pop.Ink, CircleShape))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Pop.Ink else Pop.Text)
    }
}

/** A line with a title, a hint and a switch. The whole row toggles. */
@Composable
fun PopToggle(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Switch) { onChange(!checked) }
            .heightIn(min = 56.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Pop.Text)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = Pop.TextDim)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Pop.Ink, checkedTrackColor = Pop.Lime, checkedBorderColor = Pop.Ink,
                uncheckedThumbColor = Pop.TextDim, uncheckedTrackColor = Pop.SurfaceHigh, uncheckedBorderColor = Pop.Line,
            ),
        )
    }
}

/** Segmented choice in one pill. */
@Composable
fun <T> PopSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(Pop.SurfaceHigh).border(Pop.OutlineThin, Pop.Line, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            val sel = key == selected
            val bg by animateColorAsState(if (sel) Pop.Pink else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).clip(CircleShape).background(bg)
                    .then(if (sel) Modifier.border(2.dp, Pop.Ink, CircleShape) else Modifier)
                    .clickable { onSelect(key) }
                    .semantics { role = Role.RadioButton; this.selected = sel }
                    .heightIn(min = 44.dp).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) Pop.Ink else Pop.TextDim)
            }
        }
    }
}

/** Section title in Nunito, sentence case, with an optional trailing slot. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Pop.Text, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Small pill tag, e.g. "limited" or "applied". */
@Composable
fun PopTag(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(CircleShape).background(color).border(1.5.dp, Pop.Ink, CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Pop.Ink, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = Pop.Ink)
    }
}

/** Header for a sub-page: a round back button and the title. */
@Composable
fun BackHeader(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Pop.SurfaceHigh).border(Pop.OutlineThin, Pop.Line, CircleShape)
                .clickable(onClickLabel = "Back", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(PopIcons.Back, contentDescription = "Back", tint = Pop.Text) }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Pop.Text, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** A round icon in a coloured sticker circle. */
@Composable
fun IconBadge(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(color).border(2.dp, Pop.Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Pop.Ink, modifier = Modifier.size(size * 0.5f)) }
}
