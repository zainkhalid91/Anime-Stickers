package com.zainkhalid.animebattery.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.decor.BarLayout
import com.zainkhalid.animebattery.overlay.FullBarView
import kotlin.math.roundToInt

/**
 * The real bar view inside Compose, so previews can never drift from what the
 * overlay draws. The camera sits in the middle, the size of a Pixel lens.
 */
@Composable
fun BarPreview(
    layout: BarLayout,
    modifier: Modifier = Modifier,
    background: Int = 0xFF1B1F3B.toInt(),
    level: Int = 76,
    state: BatteryState = BatteryState.Good,
    sparkles: Boolean = true,
    animations: Boolean = true,
    characterId: String = "naruto",
) {
    val d = LocalDensity.current.density
    AndroidView(
        factory = { ctx -> FullBarView(ctx) },
        modifier = modifier,
        update = { v ->
            v.barHeight = (52 * d).roundToInt()
            v.startPad = 24 * d
            v.endPad = 24 * d
            v.characterId = characterId
            v.sparklesAroundCamera = sparkles
            v.wifiLevel = 3
            v.cellLevel = 3
            v.animator.animationsEnabled = animations
            v.setBackgroundSample(background)
            v.post {
                // The view's width is only known after layout.
                val cx = v.width / 2f
                val half = 21.5f * d
                if (v.cutout.isEmpty || v.cutout.centerX() != cx) {
                    v.cutout.set(cx - half, 0f, cx + half, v.barHeight.toFloat())
                    v.invalidate()
                }
            }
            if (v.layout != layout) v.layout = layout
            v.show(state, level)
        },
    )
}
