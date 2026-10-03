package com.zainkhalid.animebattery.characters.naruto

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.battery.BatteryState.Charged
import com.zainkhalid.animebattery.battery.BatteryState.Charging
import com.zainkhalid.animebattery.battery.BatteryState.Critical
import com.zainkhalid.animebattery.battery.BatteryState.Full
import com.zainkhalid.animebattery.battery.BatteryState.Good
import com.zainkhalid.animebattery.battery.BatteryState.Hot
import com.zainkhalid.animebattery.battery.BatteryState.Low
import com.zainkhalid.animebattery.battery.BatteryState.Mid
import com.zainkhalid.animebattery.battery.BatteryState.PowerSaver
import com.zainkhalid.animebattery.characters.CharacterArt
import com.zainkhalid.animebattery.characters.Frame
import com.zainkhalid.animebattery.characters.GaugeStyle
import com.zainkhalid.animebattery.characters.Part
import com.zainkhalid.animebattery.characters.ellipse
import com.zainkhalid.animebattery.characters.mirrorX
import com.zainkhalid.animebattery.characters.part

/**
 * Chibi Naruto, fan art. Battery idea: the gauge is his chakra; at full he's in
 * Sage Mode; charging, he spins up a Rasengan in his right hand.
 *
 * Art box is 20 x 27.6 units, head about 58% of the height.
 */
object Naruto : CharacterArt() {
    override val id = "naruto"
    override val name = "Naruto Uzumaki"
    override val series = "Naruto"
    override val rightsHolder = "Masashi Kishimoto / Shueisha, TV Tokyo, Pierrot"

    private val Ink = Color(0xFF1B1622)
    private val Skin = Color(0xFFFFDDB8)
    private val SkinShade = Color(0xFFF2B68A)
    private val Hair = Color(0xFFFFC83A)
    private val HairShade = Color(0xFFE39A1A)
    private val Band = Color(0xFF27305A)
    private val Metal = Color(0xFFCDD5E0)
    private val MetalShade = Color(0xFF96A1B4)
    private val Iris = Color(0xFF2E7FE6)
    private val SageIris = Color(0xFFF7CB45)
    private val SagePigment = Color(0xFFE5702A)
    private val MouthIn = Color(0xFF7A2430)
    private val Orange = Color(0xFFFF7B1C)
    private val OrangeShade = Color(0xFFDB5A10)
    private val Jacket = Color(0xFF2A2A33)
    private val Sandal = Color(0xFF2C3B70)
    private val White = Color(0xFFFFFFFF)

    override val gauge = GaugeStyle(
        track = Color(0xFF1D2238),
        outline = Ink,
        fill = Color(0xFFFF8A1F),
        fillShade = Color(0xFFE0620E),
        critical = Color(0xFFFF4136),
    )

    // ── Body ─────────────────────────────────────────────────────────────
    private val legs = part(20f, 27.6f, "legs") {
        shape("M6.6,22.4 L9.6,22.4 L9.5,26.4 L6.8,26.4 Z M10.4,22.4 L13.4,22.4 L13.2,26.4 L10.5,26.4 Z", Orange, Ink, 1.0f)
        shape("M6.3,26.1 L9.8,26.1 Q10.0,27.5 8.7,27.5 L6.7,27.5 Q5.9,27.4 6.3,26.1 Z", Sandal, Ink, 0.9f)
        shape(mirrorX("M6.3,26.1 L9.8,26.1 Q10.0,27.5 8.7,27.5 L6.7,27.5 Q5.9,27.4 6.3,26.1 Z", 10f), Sandal, Ink, 0.9f)
    }

    private val torso = part(20f, 27.6f, "torso") {
        shape("M5.6,16.0 Q10,14.6 14.4,16.0 L14.2,23.2 Q10,23.9 5.8,23.2 Z", Orange, Ink, 1.1f)
        shape("M12.7,15.6 Q13.7,15.7 14.4,16.0 L14.2,23.2 Q13.4,23.4 12.7,23.5 Z", OrangeShade)
        shape("M5.6,16.0 Q10,14.6 14.4,16.0 L14.35,18.3 Q10,17.3 5.65,18.3 Z", Jacket, Ink, 0.7f)
        line("M10,18.0 L10,23.3", Jacket, 0.45f)
    }

    private val armsDown = part(20f, 27.6f, "armsDown") {
        val sleeve = "M5.9,16.4 Q3.8,17.6 3.6,20.4 L5.4,20.8 Q5.6,18.8 6.4,18.1 Z"
        shape(sleeve, Orange, Ink, 0.9f)
        shape(mirrorX(sleeve, 10f), Orange, Ink, 0.9f)
        shape(ellipse(4.5f, 21.1f, 1.0f), Skin, Ink, 0.7f)
        shape(ellipse(15.5f, 21.1f, 1.0f), Skin, Ink, 0.7f)
    }

    /** Left arm down, right arm out holding the Rasengan. */
    private val armsRasengan = part(20f, 27.6f, "armsRasengan") {
        shape("M5.9,16.4 Q3.8,17.6 3.6,20.4 L5.4,20.8 Q5.6,18.8 6.4,18.1 Z", Orange, Ink, 0.9f)
        shape(ellipse(4.5f, 21.1f, 1.0f), Skin, Ink, 0.7f)
        shape("M14.1,16.4 Q16.6,16.9 17.4,19.0 L15.9,19.8 Q15.4,18.4 13.6,18.2 Z", Orange, Ink, 0.9f)
        shape(ellipse(16.9f, 20.0f, 1.05f), Skin, Ink, 0.7f)
    }

    // ── Head ─────────────────────────────────────────────────────────────
    private val hairBack = part(20f, 27.6f, "hairBack") {
        shape(
            "M3.2,11.2 L0.8,8.6 L3.2,7.6 L1.2,4.6 L4.8,4.9 L4.4,1.6 L7.6,3.4 L9.6,0.4 L11.3,3.2 " +
                "L14.4,1.2 L14.6,4.6 L18.6,4.2 L16.9,7.4 L19.3,8.6 L16.8,11.2 Z",
            Hair, Ink, 1.3f,
        )
        shape("M14.6,4.6 L18.6,4.2 L16.9,7.4 L19.3,8.6 L16.8,11.2 L15.6,8.2 Z", HairShade)
    }

    private val face = part(20f, 27.6f, "face") {
        shape(
            "M3.9,9.2 C3.9,5.6 6.6,3.9 10,3.9 C13.4,3.9 16.1,5.6 16.1,9.2 " +
                "C16.1,12.6 13.6,15.7 10,15.7 C6.4,15.7 3.9,12.6 3.9,9.2 Z",
            Skin, Ink, 1.3f,
        )
        shape("M13.4,14.6 C15.2,13.4 16.0,11.5 16.0,9.4 C15.4,11.8 14.3,13.4 12.1,15.0 Z", SkinShade)
    }

    private val hairFrontAndBand = part(20f, 27.6f, "band") {
        // Hair over the top of the head, above the headband.
        shape("M4.5,6.4 C5.0,3.4 7.8,2.5 10,2.5 C12.4,2.5 15.0,3.4 15.5,6.4 C13.8,5.4 12,5.1 10,5.1 C8,5.1 6.2,5.4 4.5,6.4 Z", Hair, Ink, 0.7f)
        // Cloth band, following the forehead.
        shape(
            "M3.95,8.1 C4.4,6.0 6.5,5.0 10,5.0 C13.5,5.0 15.6,6.0 16.05,8.1 L16.0,8.9 " +
                "C14.6,7.9 12.6,7.5 10,7.5 C7.4,7.5 5.4,7.9 4.0,8.9 Z",
            Band, Ink, 0.7f,
        )
        // Metal plate and its shade.
        shape("M6.7,4.9 L13.3,4.9 Q13.9,4.9 13.9,5.5 L13.9,7.4 Q13.9,8.0 13.3,8.0 L6.7,8.0 Q6.1,8.0 6.1,7.4 L6.1,5.5 Q6.1,4.9 6.7,4.9 Z", Metal, Ink, 0.6f)
        shape("M6.25,7.0 L13.75,7.0 L13.75,7.4 Q13.75,7.85 13.3,7.85 L6.7,7.85 Q6.25,7.85 6.25,7.4 Z", MetalShade)
        // Our own little swirl mark, not the real symbol.
        line("M11.0,6.8 C9.3,7.0 8.9,5.5 10.1,5.4 C10.9,5.4 10.9,6.3 10.2,6.3", Color(0xFF3B4252), 0.42f)
        // Side bangs hanging past the band.
        val bang = "M4.1,8.5 L3.3,12.0 L5.0,10.3 L5.5,8.1 Z"
        shape(bang, Hair, Ink, 0.6f)
        shape(mirrorX(bang, 10f), Hair, Ink, 0.6f)
    }

    private val whiskers = part(20f, 27.6f, "whiskers") {
        val w = "M4.6,11.4 L6.0,11.7 M4.5,12.3 L6.0,12.4 M4.7,13.2 L6.0,13.1"
        line(w, Ink, 0.42f)
        line(mirrorX(w, 10f), Ink, 0.42f)
    }

    // ── Eyes ─────────────────────────────────────────────────────────────
    private val eyesOpen = part(20f, 27.6f, "eyesOpen") {
        shape(ellipse(7.5f, 10.4f, 1.05f, 1.4f), Iris, Ink, 0.55f)
        shape(ellipse(12.5f, 10.4f, 1.05f, 1.4f), Iris, Ink, 0.55f)
        shape(ellipse(7.2f, 9.9f, 0.4f), White)
        shape(ellipse(12.2f, 9.9f, 0.4f), White)
    }

    /** Sage Mode: orange pigment round the eyes, yellow iris, flat toad pupil. */
    private val eyesSage = part(20f, 27.6f, "eyesSage") {
        val pigment = "M5.6,9.5 Q7.5,8.3 9.4,9.5 L9.1,11.7 Q7.5,12.5 5.9,11.7 Z"
        shape(pigment, SagePigment)
        shape(mirrorX(pigment, 10f), SagePigment)
        shape(ellipse(7.5f, 10.4f, 1.05f, 1.3f), SageIris, Ink, 0.55f)
        shape(ellipse(12.5f, 10.4f, 1.05f, 1.3f), SageIris, Ink, 0.55f)
        shape("M6.75,10.15 L8.25,10.15 L8.25,10.7 L6.75,10.7 Z M11.75,10.15 L13.25,10.15 L13.25,10.7 L11.75,10.7 Z", Ink)
    }

    private val eyesClosed = part(20f, 27.6f, "eyesClosed") {
        line("M6.4,10.7 Q7.5,11.4 8.6,10.7 M11.4,10.7 Q12.5,11.4 13.6,10.7", Ink, 0.6f)
    }

    private val eyesHappy = part(20f, 27.6f, "eyesHappy") {
        line("M6.4,11.0 Q7.5,9.3 8.6,11.0 M11.4,11.0 Q12.5,9.3 13.6,11.0", Ink, 0.65f)
    }

    private val eyesTired = part(20f, 27.6f, "eyesTired") {
        shape("M6.45,10.5 A1.05,1.05 0 0,0 8.55,10.5 Z M11.45,10.5 A1.05,1.05 0 0,0 13.55,10.5 Z", Iris, Ink, 0.5f)
        line("M6.1,10.5 L8.9,10.5 M11.1,10.5 L13.9,10.5", Ink, 0.7f)
    }

    // ── Mouths ───────────────────────────────────────────────────────────
    private val mouthGrin = part(20f, 27.6f, "mouthGrin") {
        shape("M8.0,12.9 Q10,15.2 12.0,12.9 Z", MouthIn, Ink, 0.5f)
    }
    private val mouthTeeth = part(20f, 27.6f, "mouthTeeth") {
        shape("M8.1,12.8 L11.9,12.8 Q10,15.0 8.1,12.8 Z", MouthIn, Ink, 0.5f)
        shape("M8.5,12.95 L11.5,12.95 Q11.3,13.5 10,13.5 Q8.7,13.5 8.5,12.95 Z", White)
    }
    private val mouthSmile = part(20f, 27.6f, "mouthSmile") {
        line("M8.6,13.2 Q10,14.4 11.4,13.2", Ink, 0.55f)
    }
    private val mouthWorried = part(20f, 27.6f, "mouthWorried") {
        line("M8.8,14.0 Q10,13.2 11.2,14.0", Ink, 0.55f)
    }
    private val mouthGasp = part(20f, 27.6f, "mouthGasp") {
        shape(ellipse(10f, 13.7f, 0.7f, 0.85f), MouthIn, Ink, 0.5f)
    }
    private val mouthWavy = part(20f, 27.6f, "mouthWavy") {
        line("M8.3,13.6 Q8.85,13.0 9.4,13.6 Q9.95,14.2 10.5,13.6 Q11.05,13.0 11.7,13.6", Ink, 0.5f)
    }
    private val mouthSleep = part(20f, 27.6f, "mouthSleep") {
        shape(ellipse(10f, 13.6f, 0.45f, 0.35f), MouthIn, Ink, 0.4f)
    }

    // ── Effects ──────────────────────────────────────────────────────────
    private val sweatOne = part(20f, 27.6f, "sweat") {
        shape("M16.9,7.4 Q18.8,10.1 16.9,11.0 Q15.0,10.1 16.9,7.4 Z", Color(0xFF8FD3FF), Ink, 0.5f)
    }
    private val sweatTwo = part(20f, 27.6f, "sweat2") {
        shape("M17.0,8.4 Q18.3,10.3 17.0,10.9 Q15.7,10.3 17.0,8.4 Z", Color(0xFF8FD3FF), Ink, 0.45f)
        shape("M2.9,9.9 Q3.9,11.4 2.9,11.9 Q1.9,11.4 2.9,9.9 Z", Color(0xFF8FD3FF), Ink, 0.4f)
    }
    private val blush = part(20f, 27.6f, "blush") {
        shape(ellipse(5.6f, 12.3f, 1.35f, 0.7f), Color(0xFFFF5A4E), alpha = 0.9f)
        shape(ellipse(14.4f, 12.3f, 1.35f, 0.7f), Color(0xFFFF5A4E), alpha = 0.9f)
    }
    /** Heat shimmer rising off his head. */
    private val heat = part(20f, 27.6f, "heat") {
        line("M3.0,3.4 Q2.0,2.3 3.0,1.2 Q4.0,0.1 3.0,-1.0", Color(0xFFFF5A3C), 0.75f)
        line("M17.2,3.0 Q16.2,1.9 17.2,0.8 Q18.2,-0.3 17.2,-1.4", Color(0xFFFF5A3C), 0.75f)
    }
    private val zzz = part(20f, 27.6f, "zzz") {
        line("M15.8,-1.2 L19.4,-1.2 L15.8,2.4 L19.4,2.4", Color(0xFF8EA8FF), 0.85f)
        line("M13.4,2.2 L15.2,2.2 L13.4,4.0 L15.2,4.0", Color(0xFF8EA8FF), 0.6f)
    }
    private val sparkles = part(20f, 27.6f, "sparkles") {
        shape(star(18.3f, 2.6f, 1.5f), Color(0xFFFFF0A0), Ink, 0.4f)
        shape(star(1.7f, 3.6f, 1.1f), Color(0xFFFFF0A0), Ink, 0.35f)
    }

    // Rasengan: a glowing orb with two swirls that turn one step per charge frame.
    private val RasenganCenter = Offset(17.2f, 17.6f)
    private val rasenganOrb = part(20f, 27.6f, "rasenganOrb") {
        shape(ellipse(17.2f, 17.6f, 3.3f), Color(0xFF7FD0FF), Color(0xFF1D5FB0), 0.6f)
        shape(ellipse(17.2f, 17.6f, 1.6f), Color(0xFFEFFCFF))
    }
    private val rasenganSwirl = part(20f, 27.6f, "rasenganSwirl") {
        line("M14.6,17.6 Q14.9,15.0 17.2,14.6 M19.8,17.6 Q19.5,20.2 17.2,20.6", Color(0xFF1F7FE0), 0.65f)
        line("M15.5,15.9 Q17.2,16.4 17.2,17.6", Color(0xFFFFFFFF), 0.5f)
    }

    override fun drawFigure(scope: DrawScope, f: Frame) {
        legs.draw(scope)
        torso.draw(scope)
        crossfade(f) { look, a -> (if (look == Charging) armsRasengan else armsDown).draw(scope, a) }

        hairBack.draw(scope)
        face.draw(scope)
        hairFrontAndBand.draw(scope)
        whiskers.draw(scope)
        crossfade(f) { look, a -> drawExpression(scope, look, a, f.blink) }

        crossfade(f) { look, a ->
            if (look == Charging) {
                rasenganOrb.draw(scope, a)
                scope.rotate(f.chargeFrame * 36f, RasenganCenter) { rasenganSwirl.draw(this, a) }
            }
        }
    }

    private fun drawExpression(s: DrawScope, look: BatteryState, a: Float, blink: Boolean) {
        val eyes: Part = when (look) {
            Full -> eyesSage
            Charged -> eyesHappy
            PowerSaver -> eyesClosed
            Critical -> eyesTired
            else -> eyesOpen
        }
        val canBlink = eyes === eyesOpen || eyes === eyesSage || eyes === eyesTired
        (if (blink && canBlink) eyesClosed else eyes).draw(s, a)

        val mouth: Part = when (look) {
            Full, Good, Charged -> mouthGrin
            Mid -> mouthSmile
            Low -> mouthWorried
            Critical -> mouthGasp
            Charging -> mouthTeeth
            PowerSaver -> mouthSleep
            Hot -> mouthWavy
        }
        mouth.draw(s, a)

        when (look) {
            Low -> sweatOne.draw(s, a)
            Hot -> { blush.draw(s, a); sweatTwo.draw(s, a); heat.draw(s, a) }
            PowerSaver -> zzz.draw(s, a)
            Charged -> sparkles.draw(s, a)
            else -> Unit
        }
    }

    private fun star(x: Float, y: Float, r: Float) =
        "M$x,${y - r} Q$x,$y ${x + r},$y Q$x,$y $x,${y + r} Q$x,$y ${x - r},$y Q$x,$y $x,${y - r} Z"
}
