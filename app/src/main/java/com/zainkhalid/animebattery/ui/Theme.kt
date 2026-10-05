package com.zainkhalid.animebattery.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainkhalid.animebattery.R

/**
 * "Sticker Pop" design system (docs/ROADMAP.md §2): ink outlines, flat offset shadows,
 * saturated pop colours on a deep grape-black, Nunito for display and DM Sans for UI.
 * Dark only. Screens use these tokens and the ui/kit components, never raw hex.
 */
object Pop {
    val Ink = Color(0xFF0D0A14)
    val Bg = Color(0xFF16111F)
    val Surface = Color(0xFF221A30)
    val SurfaceHigh = Color(0xFF2E2440)
    val Line = Color(0xFF3D3152)
    val Text = Color(0xFFFFF8F0)
    val TextDim = Color(0xFFBDB2CF)

    /** Primary: you, your character, main actions. */
    val Pink = Color(0xFFFF4FA3)
    /** Power, charging, progress. */
    val Lime = Color(0xFFC6FF3D)
    /** Info. */
    val Sky = Color(0xFF4CC9FF)
    /** Rewards, drops, highlights. */
    val Sun = Color(0xFFFFD23F)
    /** Collections, secondary accent. */
    val Lilac = Color(0xFFB69CFF)
    /** Low battery, danger. */
    val Coral = Color(0xFFFF6B5A)

    val Outline = 2.5.dp
    val OutlineThin = 1.5.dp
    val ShadowButton = 4.dp
    val ShadowCard = 5.dp
    val RadiusCard = 24.dp
    val RadiusTile = 18.dp

    val Gap = 16.dp
    val GapSmall = 8.dp
    val Gutter = 20.dp
}

/** Spacing names the older screens (lab, badge preview) still use. */
object Tokens {
    val Gap = Pop.Gap
    val GapSmall = Pop.GapSmall
    val Radius = Pop.RadiusCard
    val RadiusSmall = Pop.RadiusTile
    val Outline = Pop.OutlineThin
}

@OptIn(ExperimentalTextApi::class)
private fun weighted(res: Int, w: Int) =
    Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Nunito = FontFamily(weighted(R.font.nunito, 700), weighted(R.font.nunito, 800), weighted(R.font.nunito, 900))
val DmSans = FontFamily(weighted(R.font.dm_sans, 400), weighted(R.font.dm_sans, 500), weighted(R.font.dm_sans, 700))

private val Scheme = darkColorScheme(
    primary = Pop.Pink,
    onPrimary = Pop.Ink,
    primaryContainer = Color(0xFF4A1D3A),
    onPrimaryContainer = Color(0xFFFFD6EA),
    secondary = Pop.Lime,
    onSecondary = Pop.Ink,
    tertiary = Pop.Sky,
    onTertiary = Pop.Ink,
    background = Pop.Bg,
    onBackground = Pop.Text,
    surface = Pop.Surface,
    onSurface = Pop.Text,
    surfaceVariant = Pop.SurfaceHigh,
    onSurfaceVariant = Pop.TextDim,
    surfaceContainer = Pop.Surface,
    surfaceContainerHigh = Pop.SurfaceHigh,
    surfaceContainerHighest = Pop.SurfaceHigh,
    outline = Pop.Line,
    outlineVariant = Pop.Line,
    error = Pop.Coral,
    onError = Pop.Ink,
)

private val Type = Typography(
    displayLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp),
)

private val ShapeSet = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Pop.RadiusTile),
    large = RoundedCornerShape(Pop.RadiusCard),
)

@Composable
fun AnimeBatteryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = ShapeSet, content = content)
}
