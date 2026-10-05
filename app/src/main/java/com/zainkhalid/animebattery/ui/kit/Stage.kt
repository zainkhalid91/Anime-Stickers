package com.zainkhalid.animebattery.ui.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainkhalid.animebattery.decor.DecorPainter
import com.zainkhalid.animebattery.decor.Mood
import com.zainkhalid.animebattery.decor.MoodPainter
import com.zainkhalid.animebattery.render.PopType
import com.zainkhalid.animebattery.ui.Nunito
import com.zainkhalid.animebattery.ui.Pop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The hero of the Buddy screen: the character big and alive on a manga backdrop that
 * takes the mood's colour. Breathes, leans with the mood, shows the same mood effects
 * as the status bar (same painter), and squashes when poked.
 */
@Composable
fun CharacterStage(
    characterId: String,
    look: com.zainkhalid.animebattery.cast.Look,
    mood: Mood,
    level: Int,
    bubble: String?,
    onPoke: () -> Unit,
    modifier: Modifier = Modifier,
    witchHat: Boolean = false,
    animate: Boolean = true,
    characterHeight: Dp = 176.dp,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val moodColor by animateColorAsState(Color(mood.color), tween(450), label = "moodColor")
    val tilt by animateFloatAsState(mood.tiltDeg, spring(dampingRatio = 0.45f, stiffness = 120f), label = "tilt")

    val loop = rememberInfiniteTransition(label = "stage")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "t")
    val breathe by loop.animateFloat(1f, 1.025f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "breathe")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "spin")
    val phase = if (animate) t else 0.25f
    val step = (phase * 5.2f).toInt()

    val squash = remember { Animatable(1f) }
    val wobble = remember { Animatable(0f) }
    // Effects drawn at the stage's scale: the overlay draws a 40dp character.
    val scale = characterHeight.value / 40f
    val moods = remember(density, scale) { MoodPainter(density * scale, PopType.nunito(context.resources, 800)) }
    val decor = remember(density, scale) { DecorPainter(density * scale) }
    val painter = stickerPainter(characterId, look)
    val aspect = painter.intrinsicSize.let { if (it.height > 0f) it.width / it.height else 0.8f }
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier.fillMaxWidth().height(characterHeight + 150.dp)
            .padding(end = 3.dp, bottom = Pop.ShadowCard)
            .hardShadow(28.dp, dx = 3.dp, dy = Pop.ShadowCard)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(moodColor.copy(alpha = 0.85f), moodColor.copy(alpha = 0.35f), Pop.Surface)))
            .drawWithCache {
                // Manga focus lines turning slowly round the character, then halftone dots.
                // Built once per size; each frame only rotates the finished path.
                val c = Offset(size.width / 2f, size.height * 0.52f)
                val r = size.maxDimension
                val burst = Path()
                for (i in 0 until 28) {
                    val a = i * (2 * PI / 28).toFloat()
                    val w = if (i % 3 == 0) 0.05f else 0.025f
                    burst.moveTo(c.x + cos(a) * r * 0.28f, c.y + sin(a) * r * 0.28f)
                    burst.lineTo(c.x + cos(a - w) * r, c.y + sin(a - w) * r)
                    burst.lineTo(c.x + cos(a + w) * r, c.y + sin(a + w) * r)
                    burst.close()
                }
                val dots = Path()
                val gap = 14.dp.toPx()
                var y = size.height * 0.55f
                while (y < size.height) {
                    val k = (y - size.height * 0.55f) / (size.height * 0.45f)
                    val rad = 0.6.dp.toPx() + k * 2.4.dp.toPx()
                    var x = if (((y / gap).toInt()) % 2 == 0) 0f else gap / 2f
                    while (x < size.width) {
                        dots.addOval(Rect(Offset(x, y), rad))
                        x += gap
                    }
                    y += gap
                }
                onDrawBehind {
                    rotate(if (animate) spin else 0f, c) { drawPath(burst, Color.White.copy(alpha = 0.10f)) }
                    drawPath(dots, Pop.Ink.copy(alpha = 0.35f))
                }
            }
            .border(Pop.Outline, Pop.Ink, shape),
    ) {
        // Speech bubble over the head.
        AnimatedContent(
            targetState = bubble,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f), initialScale = 0.6f, transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn())
                    .togetherWith(scaleOut(tween(140), targetScale = 0.8f, transformOrigin = TransformOrigin(0.5f, 1f)) + fadeOut(tween(140)))
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp, start = 16.dp, end = 16.dp),
            label = "bubble",
        ) { text ->
            if (text != null) SpeechBubble(text) else Spacer(Modifier.height(44.dp))
        }

        // The character.
        Image(
            painter, contentDescription = null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp)
                .height(characterHeight).aspectRatio(aspect)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    val b = if (animate) breathe else 1f
                    scaleY = b * squash.value
                    scaleX = (2f - squash.value) * (2f - b)
                    rotationZ = tilt + wobble.value
                }
                .drawWithContent {
                    val w = size.width
                    val h = size.height
                    drawIntoCanvas { moods.behind(it.nativeCanvas, mood, 0f, 0f, w, h, step) }
                    drawContent()
                    drawIntoCanvas {
                        if (witchHat) decor.witchHat(it.nativeCanvas, 0f, 0f, w, h)
                        moods.front(it.nativeCanvas, mood, 0f, 0f, w, h, phase, step)
                    }
                }
                .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Poke") {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPoke()
                    scope.launch {
                        squash.snapTo(0.82f)
                        squash.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f))
                    }
                    scope.launch {
                        wobble.snapTo(if (System.nanoTime() % 2 == 0L) -9f else 9f)
                        wobble.animateTo(0f, spring(dampingRatio = 0.25f, stiffness = 300f))
                    }
                }
                .semantics { contentDescription = "Your buddy, feeling ${mood.word}. Tap to poke." },
        )

        // Battery level in big outlined manga numbers, and the mood.
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedNumber("$level%", Modifier.weight(1f))
            MoodBadge(mood)
        }
    }
}

/** Text with a thick ink outline, like manga sound effects. */
@Composable
fun OutlinedNumber(text: String, modifier: Modifier = Modifier, size: Int = 40) {
    val style = TextStyle(fontFamily = Nunito, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, fontSize = size.sp, lineHeight = size.sp)
    Box(modifier) {
        Text(text, style = style.copy(drawStyle = Stroke(width = with(LocalDensity.current) { 6.dp.toPx() })), color = Pop.Ink)
        Text(text, style = style, color = Pop.Text)
    }
}

/** The mood as a little sticker pill. */
@Composable
fun MoodBadge(mood: Mood, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(Color(mood.color), label = "badge")
    Row(
        modifier.hardShadow(50.dp, dy = 3.dp).clip(CircleShape).background(bg).border(2.dp, Pop.Ink, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(8.dp)) { drawCircle(Pop.Ink) }
        Spacer(Modifier.width(6.dp))
        Text(mood.word, style = MaterialTheme.typography.labelLarge, color = Pop.Ink)
    }
}

/** A white manga speech bubble with an ink outline and a tail pointing down. */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-2).dp)) {
        Box(
            Modifier.widthIn(max = 280.dp).hardShadow(20.dp, dx = 2.dp, dy = 3.dp).clip(shape).background(Color.White)
                .border(2.dp, Pop.Ink, shape).padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = Pop.Ink)
        }
        Canvas(Modifier.size(16.dp, 10.dp)) {
            val p = Path().apply { moveTo(0f, 0f); lineTo(size.width / 2f, size.height); lineTo(size.width, 0f); close() }
            drawPath(p, Color.White)
            drawLine(Pop.Ink, Offset(0f, 0f), Offset(size.width / 2f, size.height), 2.dp.toPx())
            drawLine(Pop.Ink, Offset(size.width, 0f), Offset(size.width / 2f, size.height), 2.dp.toPx())
        }
    }
}
