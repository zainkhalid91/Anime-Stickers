package com.zainkhalid.animebattery.battery

/**
 * Maps battery snapshots to a [BatteryState].
 *
 * Order, first match wins:
 *   Hot > Charged > Charging > Critical > PowerSaver > level band.
 * Critical beats PowerSaver because Pixel turns the saver on by itself when the
 * battery is low, and "nearly dead" is the more useful thing to show.
 *
 * Hysteresis: a level band only changes once the level is [levelMargin] points past
 * the boundary, and Hot clears only below [hotExitC]. So 50/51/50/51 doesn't flicker.
 */
class BatteryStateMachine(
    private val levelMargin: Int = 2,
    private val hotEnterC: Float = 42f,
    private val hotExitC: Float = 40f,
) {
    private var band: BatteryState? = null
    private var hot = false

    var state: BatteryState = BatteryState.Mid
        private set

    fun update(s: BatterySnapshot): BatteryState {
        val level = s.level.coerceIn(0, 100)
        hot = if (hot) s.temperatureC >= hotExitC else s.temperatureC >= hotEnterC
        band = nextBand(band, level)
        state = when {
            hot -> BatteryState.Hot
            s.plugged && (s.full || level >= 100) -> BatteryState.Charged
            s.plugged -> BatteryState.Charging
            band == BatteryState.Critical -> BatteryState.Critical
            s.powerSave -> BatteryState.PowerSaver
            else -> band!!
        }
        return state
    }

    private fun nextBand(current: BatteryState?, level: Int): BatteryState {
        val raw = bandOf(level)
        if (current == null || raw == current) return raw
        val bands = BatteryState.LEVEL_BANDS
        val cur = bands.indexOf(current)
        // Only move as far as the level would reach with the margin taken off.
        return if (bands.indexOf(raw) > cur) {
            bands[maxOf(cur, bands.indexOf(bandOf(level - levelMargin)))]
        } else {
            bands[minOf(cur, bands.indexOf(bandOf(level + levelMargin)))]
        }
    }

    companion object {
        fun bandOf(level: Int): BatteryState = when {
            level <= 5 -> BatteryState.Critical
            level <= 20 -> BatteryState.Low
            level <= 50 -> BatteryState.Mid
            level <= 80 -> BatteryState.Good
            else -> BatteryState.Full
        }
    }
}
