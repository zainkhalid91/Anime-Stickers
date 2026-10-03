package com.zainkhalid.animebattery.characters

import kotlin.math.max
import kotlin.math.min

/**
 * Where everything goes, in pixels, in the badge's own coordinates (0,0 = top left
 * of the overlay window).
 *
 * The plate is an opaque pill that fully covers the stock battery icon (we can't
 * hide it on Android 17, so we sit on top of it). The figure stands on the plate's
 * right end. The part of the plate left of the figure is the gauge.
 */
class BadgeLayout {
    var width = 0f
    var height = 0f

    var plateLeft = 0f
    var plateTop = 0f
    var plateRight = 0f
    var plateBottom = 0f

    var gaugeLeft = 0f
    var gaugeRight = 0f

    var figureLeft = 0f
    var figureTop = 0f
    /** Pixels per art unit. */
    var unit = 1f

    /** Window position on screen. */
    var windowLeft = 0
    var windowTop = 0

    val plateHeight get() = plateBottom - plateTop

    /**
     * Fills this layout from the measured stock icon.
     *
     * @param stockLeft..stockBottom stock battery bounds on screen, px
     * @param minLeft first free x after the icons to the left (Wi-Fi etc), px; NaN if unknown
     * @param density px per dp
     * @param size 0.9..1.15 user size setting
     * @param nudgePx horizontal nudge for the figure only, so the plate still covers the stock icon
     */
    fun place(
        stockLeft: Float, stockTop: Float, stockRight: Float, stockBottom: Float,
        minLeft: Float, statusBarHeight: Float, screenWidth: Float,
        density: Float, size: Float = 1f, nudgePx: Float = 0f,
    ): BadgeLayout {
        unit = FIGURE_HEIGHT_DP * density * size / ART_HEIGHT

        // Plate: stock bounds plus a little margin, never reaching back into the icon on the left.
        val margin = max(2f, density)
        val sTop = stockTop - margin
        val sBottom = stockBottom + margin
        val sRight = stockRight + margin
        var sLeft = stockLeft - margin
        if (!minLeft.isNaN()) sLeft = min(stockLeft - 1f, max(sLeft, minLeft))

        val figW = ART_WIDTH * unit
        val figH = ART_HEIGHT * unit
        val figLeftScreen = sRight - FIGURE_OVERLAP * unit + nudgePx
        val cy = (sTop + sBottom) / 2f
        val figTopScreen = (cy - figH / 2f + 0.6f * unit).coerceIn(0f, max(0f, statusBarHeight - figH))

        // Window wraps plate + figure, with room for effects either side.
        val left = min(sLeft, figLeftScreen) - 2f * unit
        val right = min(screenWidth, max(sRight, figLeftScreen + figW) + 1.5f * unit)
        windowLeft = left.toInt()
        windowTop = 0
        width = right - windowLeft
        height = statusBarHeight

        plateLeft = sLeft - windowLeft
        plateRight = sRight - windowLeft
        plateTop = sTop
        plateBottom = sBottom
        figureLeft = figLeftScreen - windowLeft
        figureTop = figTopScreen

        gaugeLeft = plateLeft + plateHeight * 0.18f
        gaugeRight = min(plateRight - plateHeight * 0.18f, figureLeft + GAUGE_UNDER_FIGURE * unit)
        return this
    }

    companion object {
        /** Art is drawn in a 20 x 27.6 unit box; the figure stands 24dp tall at 100%. */
        const val ART_WIDTH = 20f
        const val ART_HEIGHT = 27.6f
        const val FIGURE_HEIGHT_DP = 24f
        /** How many units of the figure sit over the plate's right end. */
        const val FIGURE_OVERLAP = 9f
        /** The gauge runs a little way behind the figure's left edge. */
        const val GAUGE_UNDER_FIGURE = 3f
    }
}
