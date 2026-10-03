package com.zainkhalid.animebattery.battery

/** What the character shows. Level bands come last; the special states win over them. */
enum class BatteryState {
    Full, Good, Mid, Low, Critical, Charging, Charged, PowerSaver, Hot;

    val isLevelBand: Boolean get() = this in LEVEL_BANDS

    companion object {
        /** Lowest first, so the index grows with the level. */
        val LEVEL_BANDS = listOf(Critical, Low, Mid, Good, Full)
    }
}

/** Raw numbers from ACTION_BATTERY_CHANGED and PowerManager, nothing Android-specific. */
data class BatterySnapshot(
    val level: Int,
    val plugged: Boolean,
    val full: Boolean = false,
    val temperatureC: Float = 30f,
    val powerSave: Boolean = false,
)
