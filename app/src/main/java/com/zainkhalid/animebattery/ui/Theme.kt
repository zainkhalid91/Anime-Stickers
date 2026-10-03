package com.zainkhalid.animebattery.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tokens from the ui-ux-pro-max design system for this app: playful orange, a
// trust blue accent, deep navy. Dark first. System font (Google Sans Flex on Pixel)
// instead of downloading Fredoka/Nunito, to keep the app free of outside assets.
object Tokens {
    val Primary = Color(0xFFF97316)
    val OnPrimary = Color(0xFF0F172A)
    val Secondary = Color(0xFFFB923C)
    val Accent = Color(0xFF2563EB)
    val Background = Color(0xFF0B1020)
    val Card = Color(0xFF131A2C)
    val CardHigh = Color(0xFF1B2338)
    val Muted = Color(0xFF1E293B)
    val MutedText = Color(0xFFCBD5E1)
    val Border = Color(0xFF334155)
    val Foreground = Color(0xFFF8FAFC)
    val Danger = Color(0xFFDC2626)

    val Gap = 16.dp
    val GapSmall = 8.dp
    val Radius = 24.dp
    val RadiusSmall = 16.dp
    val Outline = 2.dp
}

private val Scheme = darkColorScheme(
    primary = Tokens.Primary,
    onPrimary = Tokens.OnPrimary,
    primaryContainer = Color(0xFF3A1D08),
    onPrimaryContainer = Color(0xFFFFD8BF),
    secondary = Tokens.Secondary,
    onSecondary = Tokens.OnPrimary,
    tertiary = Tokens.Accent,
    onTertiary = Color.White,
    background = Tokens.Background,
    onBackground = Tokens.Foreground,
    surface = Tokens.Card,
    onSurface = Tokens.Foreground,
    surfaceVariant = Tokens.Muted,
    onSurfaceVariant = Tokens.MutedText,
    surfaceContainer = Tokens.Card,
    surfaceContainerHigh = Tokens.CardHigh,
    surfaceContainerHighest = Tokens.CardHigh,
    outline = Tokens.Border,
    outlineVariant = Tokens.Border,
    error = Tokens.Danger,
)

private val Sans = FontFamily.SansSerif

private val Type = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp),
)

private val ShapeSet = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Tokens.RadiusSmall),
    large = RoundedCornerShape(Tokens.Radius),
)

@Composable
fun AnimeBatteryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = ShapeSet, content = content)
}
