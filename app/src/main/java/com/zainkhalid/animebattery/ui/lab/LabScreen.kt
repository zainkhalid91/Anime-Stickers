package com.zainkhalid.animebattery.ui.lab

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import com.zainkhalid.animebattery.R
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainkhalid.animebattery.ui.Tokens
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sample lab: three ideas with the sticker-style art, to judge the look before we
 * commit. Level and charging at the top drive all of them.
 */
@Composable
fun LabScreen(modifier: Modifier = Modifier) {
    var level by rememberSaveable { mutableFloatStateOf(85f) }
    var charging by rememberSaveable { mutableStateOf(false) }
    var aiArt by rememberSaveable { mutableStateOf(true) }
    val mood = moodFor(level.roundToInt(), charging)
    val art = if (aiArt) ImageBitmap.imageResource(R.drawable.sticker_naruto_happy) else null

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(Tokens.Gap)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${level.roundToInt()}%", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.width(72.dp))
                    Slider(value = level, onValueChange = { level = it }, valueRange = 0f..100f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Charging", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = charging, onCheckedChange = { charging = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AI sticker art", style = MaterialTheme.typography.bodyLarge)
                        Text("Off = hand-drawn vector", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = aiArt, onCheckedChange = { aiArt = it })
                }
            }
        }

        SampleTitle("1 · Battery widget", "Home screen widget idea. Liquid fill, big number, the character pops out.")
        BatteryWidgetSample(level.roundToInt(), charging, mood, art)

        SampleTitle("2 · Charging screen", "Shows for a few seconds when you plug in.")
        ChargingSample(level.roundToInt(), art)

        SampleTitle("3 · Screen pet", "Walks along the bottom of the screen, naps when the battery is low.")
        ScreenPetSample(level.roundToInt(), art)

        SampleTitle("Expressions", "Each battery state gets its own face.")
        MoodStrip()
    }
}

private fun moodFor(level: Int, charging: Boolean) = when {
    charging -> Mood.Charging
    level > 80 -> Mood.Sage
    level > 50 -> Mood.Happy
    level > 20 -> Mood.Calm
    level > 5 -> Mood.Worried
    else -> Mood.Sleepy
}

@Composable
private fun SampleTitle(title: String, hint: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Random blink every 3-6 s. */
@Composable
private fun rememberBlink(): Boolean {
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val r = Random(System.nanoTime())
        while (true) {
            delay(r.nextLong(3000, 6000))
            blink = true
            delay(130)
            blink = false
        }
    }
    return blink
}

/** Draws the sticker with its bottom-centre at [foot], [height] px tall. */
private fun DrawScope.sticker(foot: Offset, height: Float, mood: Mood, blink: Boolean, armUp: Boolean, squash: Float = 0f) {
    val k = height / StickerNaruto.H
    withTransform({
        translate(foot.x - StickerNaruto.W * k / 2f, foot.y - height)
        scale(k, k, Offset.Zero)
        scale(1f + squash * 0.6f, 1f - squash, Offset(StickerNaruto.W / 2f, StickerNaruto.H))
    }) {
        StickerNaruto.draw(this, mood, blink, armUp)
    }
}

/** AI sticker: right hand position as a fraction of the image (for the Rasengan). */
private val ImageHand = Offset(0.70f, 0.77f)

/**
 * Draws either the bitmap sticker or the vector one, bottom-centre at [foot].
 * Returns the hand point in canvas pixels, for effects.
 */
private fun DrawScope.anySticker(
    img: ImageBitmap?, foot: Offset, height: Float, mood: Mood, blink: Boolean, armUp: Boolean, squash: Float = 0f,
): Offset {
    if (img == null) {
        sticker(foot, height, mood, blink, armUp, squash)
        val k = height / StickerNaruto.H
        return Offset(foot.x - StickerNaruto.W * k / 2f + StickerNaruto.HandUp.x * k, foot.y - height + StickerNaruto.HandUp.y * k)
    }
    val w = height * img.width / img.height
    withTransform({
        translate(foot.x - w / 2f, foot.y - height)
        scale(1f + squash * 0.6f, 1f - squash, Offset(w / 2f, height))
    }) {
        drawImage(
            img, dstSize = IntSize(w.roundToInt(), height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
    }
    return Offset(foot.x - w / 2f + ImageHand.x * w, foot.y - height + ImageHand.y * height)
}

// ── 1 · Battery widget ───────────────────────────────────────────────────

@Composable
private fun BatteryWidgetSample(level: Int, charging: Boolean, mood: Mood, img: ImageBitmap?) {
    val t = rememberInfiniteTransition(label = "widget")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "time")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val fill by animateFloatAsState(level / 100f, spring(dampingRatio = 0.6f, stiffness = 80f), label = "fill")
    var lastMood by remember { mutableStateOf(mood) }
    val pop = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(mood) {
        if (mood != lastMood) {
            lastMood = mood
            pop.snapTo(1f)
            pop.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f))
        }
    }
    val blink = rememberBlink()
    val text = rememberTextMeasurer()
    val wave = remember { Path() }
    val body = remember { Path() }

    val fillColor = when {
        level <= 20 -> listOf(Color(0xFFFF8A8A), Color(0xFFFF4D6D))
        level <= 50 -> listOf(Color(0xFFFFE08A), Color(0xFFFFB020))
        else -> listOf(Color(0xFF9BF2C9), Color(0xFF2ECC8F))
    }

    Canvas(Modifier.fillMaxWidth().aspectRatio(1.9f)) {
        val w = size.width
        val h = size.height
        val r = h * 0.16f
        // Background: soft mint card with slowly turning sun rays.
        drawRoundRect(Brush.radialGradient(listOf(Color(0xFFE6FFF4), Color(0xFFA8EBD3)), Offset(w * 0.35f, h * 0.5f), w * 0.8f), cornerRadius = CornerRadius(r))
        val c = Offset(w * 0.3f, h * 0.55f)
        clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, CornerRadius(r))) }) {
            rotate(time * 30f, c) {
                for (i in 0 until 12) rotate(i * 30f, c) {
                    val ray = Path().apply { moveTo(c.x, c.y); lineTo(c.x + w, c.y - w * 0.12f); lineTo(c.x + w, c.y + w * 0.12f); close() }
                    drawPath(ray, Color.White, alpha = 0.22f)
                }
            }
        }

        // Battery body.
        val bl = w * 0.30f
        val bt = h * 0.22f
        val bw = w * 0.60f
        val bh = h * 0.58f
        val br = bh * 0.22f
        drawRoundRect(Color.White.copy(alpha = 0.75f), Offset(bl, bt), Size(bw, bh), CornerRadius(br))
        drawRoundRect(Color(0xFF34C08E), Offset(bl + bw + 4.dp.toPx(), bt + bh * 0.3f), Size(7.dp.toPx(), bh * 0.4f), CornerRadius(4.dp.toPx()))

        // Liquid fill with a wavy edge.
        val inset = 6.dp.toPx()
        body.reset()
        body.addRoundRect(androidx.compose.ui.geometry.RoundRect(bl + inset, bt + inset, bl + bw - inset, bt + bh - inset, CornerRadius(br - inset)))
        val edge = bl + inset + (bw - 2 * inset) * fill
        val amp = if (charging) 6.dp.toPx() else 3.dp.toPx()
        wave.reset()
        wave.moveTo(bl, bt)
        var y = bt
        while (y <= bt + bh) {
            val x = edge + amp * sin((y / bh * 2.4f + time * if (charging) 6f else 2f) * 2 * PI).toFloat()
            wave.lineTo(x, y)
            y += 3f
        }
        wave.lineTo(bl, bt + bh)
        wave.close()
        clipPath(body) {
            clipPath(wave) {
                drawRect(Brush.verticalGradient(fillColor, bt, bt + bh), Offset(bl, bt), Size(bw, bh))
                // Bubbles.
                for (i in 0 until 6) {
                    val px = bl + inset + (bw - 2 * inset) * fill * ((i * 0.17f + 0.08f) % 1f)
                    val py = bt + bh - ((time * (0.6f + i * 0.13f) + i * 0.21f) % 1f) * bh
                    drawCircle(Color.White, (2.5f + i % 3).dp.toPx(), Offset(px, py), alpha = 0.45f)
                }
            }
        }
        drawRoundRect(Color(0xFF34C08E), Offset(bl, bt), Size(bw, bh), CornerRadius(br), style = Stroke(4.dp.toPx()))

        // Big number.
        val label = text.measure(
            "$level%",
            TextStyle(fontSize = (h * 0.22f).toSp(), fontWeight = FontWeight.Black, color = Color(0xFF0E4D3A), textAlign = TextAlign.Center),
        )
        drawText(label, topLeft = Offset(bl + bw * 0.58f - label.size.width / 2f, bt + bh / 2f - label.size.height / 2f))

        // The character pops out of the left, bobbing; squash-pop on mood change.
        val foot = Offset(w * 0.2f, h * 0.97f - bob * 4.dp.toPx())
        val hand = anySticker(img, foot, h * 1.0f, mood, blink, armUp = charging, squash = pop.value * 0.18f)
        if (charging) StickerNaruto.drawRasengan(this, hand, h * 0.1f, time * 3f, bob)

        // Sparkles twinkling around.
        val spots = listOf(Offset(0.92f, 0.12f), Offset(0.47f, 0.1f), Offset(0.06f, 0.2f), Offset(0.95f, 0.88f))
        spots.forEachIndexed { i, p ->
            val tw = ((sin((time * 2 + i * 0.27f) * 2 * PI) + 1) / 2).toFloat()
            drawSparkle(Offset(p.x * w, p.y * h), (5 + 6 * tw).dp.toPx(), Color.White, 0.5f + 0.5f * tw)
        }
        // Hearts floating up from the top of the battery.
        for (i in 0 until 3) {
            val p = (time * 1.5f + i / 3f) % 1f
            drawHeart(Offset(bl + bw * (0.3f + i * 0.25f), bt - p * h * 0.25f), 7.dp.toPx(), Color(0xFFFF6FA0), 1f - p)
        }
    }
}

private fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color, alpha: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color, alpha = alpha)
}

private fun DrawScope.drawHeart(c: Offset, r: Float, color: Color, alpha: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y + r)
        cubicTo(c.x - r * 1.6f, c.y - r * 0.2f, c.x - r * 0.7f, c.y - r * 1.4f, c.x, c.y - r * 0.5f)
        cubicTo(c.x + r * 0.7f, c.y - r * 1.4f, c.x + r * 1.6f, c.y - r * 0.2f, c.x, c.y + r)
        close()
    }
    drawPath(p, color, alpha = alpha)
}

// ── 2 · Charging screen ──────────────────────────────────────────────────

@Composable
private fun ChargingSample(level: Int, img: ImageBitmap?) {
    val t = rememberInfiniteTransition(label = "charge")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "time")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "pulse")
    val shown by animateFloatAsState(level.toFloat(), tween(1200), label = "count")
    val blink = rememberBlink()
    val text = rememberTextMeasurer()
    val seeds = remember { List(26) { Random(it).let { r -> Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }

    Canvas(Modifier.fillMaxWidth().aspectRatio(0.78f)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0B1230), Color(0xFF162463), Color(0xFF0B1230))), cornerRadius = CornerRadius(28.dp.toPx()))
        val c = Offset(w / 2f, h * 0.42f)

        // Rings pulsing outward.
        for (i in 0 until 3) {
            val p = (pulse + i / 3f) % 1f
            drawCircle(Color(0xFF6FC8FF), w * (0.22f + p * 0.4f), c, alpha = (1f - p) * 0.45f, style = Stroke(3.dp.toPx()))
        }
        // Energy particles drifting up.
        seeds.forEach { (a, b, s) ->
            val p = (time * (1f + s) + b) % 1f
            val x = w * (0.1f + a * 0.8f) + sin((p * 3 + a * 6) * PI).toFloat() * 8.dp.toPx()
            drawCircle(if (s > 0.6f) Color.White else Color(0xFF7FD3FF), (1.5f + s * 3f).dp.toPx(), Offset(x, h * (1f - p)), alpha = sin(p * PI).toFloat() * 0.9f)
        }
        // Level arc around the character.
        val ar = w * 0.36f
        drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, Offset(c.x - ar, c.y - ar), Size(ar * 2, ar * 2), style = Stroke(10.dp.toPx()))
        drawArc(
            Brush.sweepGradient(listOf(Color(0xFF3A9BFF), Color(0xFF7CF5FF), Color(0xFF3A9BFF)), c),
            -90f, 360f * shown / 100f, false, Offset(c.x - ar, c.y - ar), Size(ar * 2, ar * 2),
            style = Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )

        val sh = w * 0.62f
        val foot = Offset(c.x, c.y + sh * 0.5f)
        val hand = anySticker(img, foot, sh, Mood.Charging, blink, armUp = true)
        StickerNaruto.drawRasengan(this, hand, sh * 0.12f + pulse * 2.dp.toPx(), time * 8f, pulse)

        val big = text.measure("${shown.roundToInt()}%", TextStyle(fontSize = (w * 0.15f).toSp(), fontWeight = FontWeight.Black, color = Color.White))
        drawText(big, topLeft = Offset(c.x - big.size.width / 2f, h * 0.8f))
        val small = text.measure("⚡ Charging", TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9FD8FF)))
        drawText(small, topLeft = Offset(c.x - small.size.width / 2f, h * 0.8f - small.size.height - 2.dp.toPx()))
    }
}

// ── 3 · Screen pet ───────────────────────────────────────────────────────

@Composable
private fun ScreenPetSample(level: Int, img: ImageBitmap?) {
    val t = rememberInfiniteTransition(label = "pet")
    val walk by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "walk")
    val step by t.animateFloat(0f, 1f, infiniteRepeatable(tween(420, easing = LinearEasing)), label = "step")
    val sleeping = level <= 20
    val blink = rememberBlink()
    var lastWalk by remember { mutableFloatStateOf(0f) }
    val facingRight = walk >= lastWalk
    LaunchedEffect(walk) { lastWalk = walk }

    Box(Modifier.fillMaxWidth().height(150.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFF1E6), Color(0xFFFFD9C2))), cornerRadius = CornerRadius(24.dp.toPx()))
            drawRect(Color(0xFFF5B994), Offset(0f, h * 0.86f), Size(w, h * 0.14f))
            val sh = h * 0.78f
            val x = if (sleeping) w * 0.5f else w * (0.12f + walk * 0.76f)
            val hop = if (sleeping) 0f else kotlin.math.abs(sin(step * PI).toFloat()) * 5.dp.toPx()
            val foot = Offset(x, h * 0.9f - hop)
            withTransform({ scale(if (facingRight || sleeping) 1f else -1f, 1f, Offset(x, foot.y)) }) {
                // Lean into the step a little.
                rotate(if (sleeping) 0f else sin(step * 2 * PI).toFloat() * 4f, foot) {
                    anySticker(img, foot, sh, if (sleeping) Mood.Sleepy else Mood.Happy, blink && !sleeping, armUp = false)
                }
            }
            if (sleeping) {
                val zt = (walk * 6f) % 1f
                for (i in 0 until 3) {
                    val p = (zt + i / 3f) % 1f
                    drawZ(Offset(x + sh * 0.35f + p * 30.dp.toPx(), h * 0.3f - p * 40.dp.toPx()), (8 + i * 3).dp.toPx(), 1f - p)
                }
            }
        }
    }
}

private fun DrawScope.drawZ(c: Offset, s: Float, alpha: Float) {
    val p = Path().apply { moveTo(c.x - s / 2, c.y - s / 2); lineTo(c.x + s / 2, c.y - s / 2); lineTo(c.x - s / 2, c.y + s / 2); lineTo(c.x + s / 2, c.y + s / 2) }
    drawPath(p, Color(0xFF7C8CFF), alpha = alpha, style = Stroke(s * 0.18f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

@Composable
private fun MoodStrip() {
    val blink = rememberBlink()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(Mood.entries) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE7F0))) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(96.dp, 115.dp)) {
                        sticker(Offset(size.width / 2, size.height - 4.dp.toPx()), size.height - 8.dp.toPx(), m, blink, armUp = m == Mood.Charging)
                    }
                    Text(m.name, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B2140))
                }
            }
        }
    }
}
