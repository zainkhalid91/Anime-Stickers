package com.zainkhalid.animebattery.ui.lab

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.zainkhalid.animebattery.characters.ellipse
import com.zainkhalid.animebattery.characters.mirrorX
import com.zainkhalid.animebattery.characters.part

/** Expressions the sample sticker can make. */
enum class Mood { Happy, Sage, Calm, Worried, Sleepy, Charging, Excited }

/**
 * Sticker-style chibi Naruto (fan art), drawn for big sizes: gradient hair and eyes,
 * glossy highlights, blush, a thick white sticker border and a soft drop shadow.
 * 100 x 120 viewport. Parts are built once.
 */
object StickerNaruto {
    const val W = 100f
    const val H = 120f

    private val Ink = Color(0xFF2B1B17)
    private val Skin = Color(0xFFFFE6CF)
    private val SkinShade = Color(0xFFF6C9A4)
    private val Orange = Color(0xFFFF8A2A)
    private val Black = Color(0xFF24242C)

    private val HairBrush = Brush.verticalGradient(listOf(Color(0xFFFFEE7A), Color(0xFFFFC233), Color(0xFFF29A1C)), 0f, 60f)
    private val OrangeBrush = Brush.verticalGradient(listOf(Color(0xFFFFA24A), Color(0xFFF2680F)), 76f, 104f)
    private val MetalBrush = Brush.verticalGradient(listOf(Color(0xFFF4F7FB), Color(0xFFB7C2D3)), 29f, 40f)
    private val BlueIris = Brush.verticalGradient(listOf(Color(0xFF173A8C), Color(0xFF3F8CFF), Color(0xFF8CD0FF)), 46f, 64f)
    private val SageIris = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFFDE68A)), 46f, 64f)
    private val InkBrush = SolidColor(Ink)

    // ── Silhouette parts (get the sticker border) ───────────────────────
    private val legs = part(W, H, "legs") {
        shape("M37,101 L36,112 L46.5,112 L47,102 Z M53,102 L53.5,112 L64,112 L63,101 Z", OrangeBrush, InkBrush, 2f)
        shape("M34.5,110.5 L47.5,110.5 Q48.5,117 44,117 L36,117 Q33,116.5 34.5,110.5 Z", Color(0xFF2E3F7A), Ink, 2f)
        shape(mirrorX("M34.5,110.5 L47.5,110.5 Q48.5,117 44,117 L36,117 Q33,116.5 34.5,110.5 Z", 50f), Color(0xFF2E3F7A), Ink, 2f)
    }
    private val torso = part(W, H, "torso") {
        shape("M31,80 C34,75 42,73.5 50,73.5 C58,73.5 66,75 69,80 L71,101 C62,105 38,105 29,101 Z", OrangeBrush, InkBrush, 2.2f)
        shape("M31,80 C34,75 42,73.5 50,73.5 C58,73.5 66,75 69,80 L70,87 C61,82.5 39,82.5 30,87 Z", Black, Ink, 1.8f)
        shape("M60,84 L70,87 L71,101 C68,102.4 64,103.3 60.5,103.8 Z", Color(0xFFE85E0B))
        line("M50,85 L50,103.5", Black, 1.6f)
        shape("M46.5,86 L53.5,86 L53.5,89.5 L46.5,89.5 Z", Color(0xFFFFFFFF), Ink, 1f)
    }
    private val armsDown = part(W, H, "armsDown") {
        val a = "M31.5,81 C23,85 20,93 21,99 L28.5,100 C28.5,94 30.5,89.5 34,87.5 Z"
        shape(a, OrangeBrush, InkBrush, 2f)
        shape(mirrorX(a, 50f), OrangeBrush, InkBrush, 2f)
        shape(ellipse(24.8f, 100.5f, 4.3f), Skin, Ink, 1.8f)
        shape(ellipse(75.2f, 100.5f, 4.3f), Skin, Ink, 1.8f)
    }
    /** Right arm up and out, palm holding the Rasengan. */
    private val armsUp = part(W, H, "armsUp") {
        shape("M31.5,81 C23,85 20,93 21,99 L28.5,100 C28.5,94 30.5,89.5 34,87.5 Z", OrangeBrush, InkBrush, 2f)
        shape(ellipse(24.8f, 100.5f, 4.3f), Skin, Ink, 1.8f)
        shape("M68,81 C76,80 84,76 87,70 L81.5,66.5 C79,71 74,74.5 66.5,76 Z", OrangeBrush, InkBrush, 2f)
        shape(ellipse(85.5f, 66f, 4.6f), Skin, Ink, 1.8f)
    }
    private val hairBack = part(W, H, "hairBack") {
        shape(
            "M18,60 L5,52 L16,45 L3,33 L19,31 L11,15 L28,20 L28,4 L41,15 L50,0 L59,15 L72,4 L72,20 " +
                "L89,15 L81,31 L97,33 L84,45 L95,52 L82,60 Z",
            HairBrush, InkBrush, 2.4f,
        )
    }
    private val head = part(W, H, "head") {
        shape("M16,50 C16,28 32,16 50,16 C68,16 84,28 84,50 C84,68 70,80 50,80 C30,80 16,68 16,50 Z", Skin, Ink, 2.4f)
    }

    // ── Details (no border) ─────────────────────────────────────────────
    private val headDetail = part(W, H, "headDetail") {
        shape("M70,72 C79,66 83.5,58 83.8,50 C81,60 75,67 64,75.5 Z", SkinShade)
        // Hair over the top, above the band.
        shape("M19,41 C21,24 34,13 50,13 C66,13 79,24 81,41 C71,33 61,30.5 50,30.5 C39,30.5 29,33 19,41 Z", HairBrush, InkBrush, 2f)
        // Side locks.
        val lock = "M18.5,44 L11,66 L20.5,59 L23.5,46 Z"
        shape(lock, HairBrush, InkBrush, 1.8f)
        shape(mirrorX(lock, 50f), HairBrush, InkBrush, 1.8f)
        // Headband and plate.
        shape("M17.5,45 C23,35 35,30.5 50,30.5 C65,30.5 77,35 82.5,45 L82.5,49.5 C75,41.5 63,38 50,38 C37,38 25,41.5 17.5,49.5 Z", Color(0xFF2B3A67), Ink, 1.8f)
        shape("M38,29 L62,29 Q65,29 65,32 L65,37.5 Q65,40.5 62,40.5 L38,40.5 Q35,40.5 35,37.5 L35,32 Q35,29 38,29 Z", MetalBrush, InkBrush, 1.6f)
        line("M53,38 C46,38.8 44.5,32.2 50,31.8 C53.6,31.6 53.7,35.6 50.6,35.6", Color(0xFF4A5468), 1.4f)
        line("M38.5,31.6 L61.5,31.6", Color(0xFFFFFFFF), 1.1f, alpha = 0.8f)
        // Hair shine.
        line("M30,22 Q38,16.5 47,16.2", Color(0xFFFFFBD6), 2.2f, alpha = 0.9f)
        // Whiskers and blush.
        val wh = "M19.5,62.5 L28,63.6 M19,66.4 L28,66.6 M20,70.3 L28,69.6"
        line(wh, Ink, 1.3f)
        line(mirrorX(wh, 50f), Ink, 1.3f)
        shape(ellipse(27f, 67.5f, 5.2f, 2.6f), Color(0xFFFF8FA3), alpha = 0.55f)
        shape(ellipse(73f, 67.5f, 5.2f, 2.6f), Color(0xFFFF8FA3), alpha = 0.55f)
    }

    // ── Eyes ─────────────────────────────────────────────────────────────
    private fun eyes(name: String, iris: Brush, pupilBar: Boolean) = part(W, H, name) {
        for (cx in floatArrayOf(36f, 64f)) {
            shape(ellipse(cx, 55.5f, 7.2f, 8.8f), iris, InkBrush, 1.4f)
            if (pupilBar) shape("M${cx - 4.6f},55 L${cx + 4.6f},55 L${cx + 4.6f},57.4 L${cx - 4.6f},57.4 Z", Ink)
            else shape(ellipse(cx, 57f, 3.5f, 4.4f), Color(0xFF0E1A3C))
            shape(ellipse(cx - 2.6f, 51.6f, 2.6f, 2.8f), Color.White)
            shape(ellipse(cx + 2.4f, 60.4f, 1.2f), Color.White, alpha = 0.9f)
            line("M${cx - 8.4f},51.5 Q$cx,43.6 ${cx + 8.4f},51.5", Ink, 2.8f)
        }
    }
    private val eyesBlue = eyes("eyesBlue", BlueIris, pupilBar = false)
    private val eyesSage = part(W, H, "sagePigment") {
        val p = "M26.5,50 Q36,43 45.5,50 L44.5,61.5 Q36,66 27.5,61.5 Z"
        shape(p, Color(0xFFF07A2E), alpha = 0.9f)
        shape(mirrorX(p, 50f), Color(0xFFF07A2E), alpha = 0.9f)
    }
    private val eyesGold = eyes("eyesGold", SageIris, pupilBar = true)
    private val eyesClosed = part(W, H, "eyesClosed") {
        line("M28.5,56 Q36,61.5 43.5,56 M56.5,56 Q64,61.5 71.5,56", Ink, 2.6f)
    }
    private val eyesHappy = part(W, H, "eyesHappy") {
        line("M28.5,58.5 Q36,48.5 43.5,58.5 M56.5,58.5 Q64,48.5 71.5,58.5", Ink, 2.8f)
    }
    private val browsWorried = part(W, H, "browsWorried") {
        line("M29,47.5 L41,44.5 M71,47.5 L59,44.5", Ink, 2f)
    }

    // ── Mouths ───────────────────────────────────────────────────────────
    private val mouthGrin = part(W, H, "mouthGrin") {
        shape("M42,66.5 Q50,78 58,66.5 Q50,68.8 42,66.5 Z", Color(0xFF9F1D35), Ink, 1.6f)
        shape("M45.5,71.5 Q50,75.2 54.5,71.5 Q50,70.2 45.5,71.5 Z", Color(0xFFFF7A8A))
    }
    private val mouthSmile = part(W, H, "mouthSmile") { line("M44,68 Q50,73 56,68", Ink, 1.8f) }
    private val mouthO = part(W, H, "mouthO") { shape(ellipse(50f, 70f, 3.2f, 3.8f), Color(0xFF9F1D35), Ink, 1.5f) }
    private val mouthSmall = part(W, H, "mouthSmall") { shape(ellipse(50f, 70f, 1.8f, 1.4f), Color(0xFF9F1D35), Ink, 1.2f) }
    private val sweat = part(W, H, "sweat") {
        shape("M85,36 Q91.5,46 85,49.5 Q78.5,46 85,36 Z", Brush.verticalGradient(listOf(Color(0xFFE0F4FF), Color(0xFF7CC8FF)), 36f, 50f), InkBrush, 1.5f)
    }

    private val silhouetteParts = listOf(legs, torso, hairBack, head)
    private val shadowBrush = SolidColor(Color(0x33000000))
    private val whiteBrush = SolidColor(Color.White)
    private val borderStroke = Stroke(width = 7f, join = StrokeJoin.Round, cap = StrokeCap.Round)

    /**
     * Draws the sticker into a 100 x 120 box (caller scales). [armUp] swaps in the raised
     * arm for charging. [blink] closes the eyes for one frame.
     */
    fun draw(s: DrawScope, mood: Mood, blink: Boolean = false, armUp: Boolean = false) {
        val arms = if (armUp) armsUp else armsDown
        // Soft shadow, then the white sticker border, both from the silhouette.
        s.translate(0f, 3.2f) {
            for (p in silhouetteParts) p.drawSilhouette(this, shadowBrush, borderStroke, fill = true)
            arms.drawSilhouette(this, shadowBrush, borderStroke, fill = true)
        }
        for (p in silhouetteParts) p.drawSilhouette(s, whiteBrush, borderStroke, fill = true)
        arms.drawSilhouette(s, whiteBrush, borderStroke, fill = true)

        legs.draw(s)
        torso.draw(s)
        arms.draw(s)
        hairBack.draw(s)
        head.draw(s)
        headDetail.draw(s)

        when {
            blink && mood != Mood.Sleepy && mood != Mood.Excited -> eyesClosed.draw(s)
            mood == Mood.Sage -> { eyesSage.draw(s); eyesGold.draw(s) }
            mood == Mood.Sleepy -> eyesClosed.draw(s)
            mood == Mood.Excited -> eyesHappy.draw(s)
            else -> eyesBlue.draw(s)
        }
        if (mood == Mood.Worried) { browsWorried.draw(s); sweat.draw(s) }
        when (mood) {
            Mood.Happy, Mood.Sage, Mood.Excited, Mood.Charging -> mouthGrin.draw(s)
            Mood.Calm -> mouthSmile.draw(s)
            Mood.Worried -> mouthO.draw(s)
            Mood.Sleepy -> mouthSmall.draw(s)
        }
    }

    /** Where the raised hand is, for effects like the Rasengan. */
    val HandUp = Offset(86f, 56f)

    /** A spinning Rasengan centred on [c], radius [r], turned by [turns] (0..1). */
    fun drawRasengan(s: DrawScope, c: Offset, r: Float, turns: Float, glow: Float) {
        s.drawCircle(Brush.radialGradient(listOf(Color(0x9960C8FF), Color(0x0060C8FF)), c, r * (1.9f + 0.4f * glow)), r * (1.9f + 0.4f * glow), c)
        s.drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFA8E4FF), Color(0xFF3A9BFF)), c, r), r, c)
        s.rotate(turns * 360f, c) {
            for (i in 0 until 3) {
                rotate(i * 120f, c) {
                    drawArc(
                        Color.White, startAngle = 0f, sweepAngle = 110f, useCenter = false,
                        topLeft = Offset(c.x - r * 0.72f, c.y - r * 0.72f),
                        size = androidx.compose.ui.geometry.Size(r * 1.44f, r * 1.44f),
                        style = Stroke(width = r * 0.16f, cap = StrokeCap.Round), alpha = 0.85f,
                    )
                }
            }
        }
        s.drawCircle(Color(0xFF1D5FB0), r, c, style = Stroke(r * 0.08f))
    }
}
