package com.zainkhalid.animebattery.characters

import com.zainkhalid.animebattery.battery.BatteryState

/**
 * Everything a character needs to draw one frame. One instance is reused for
 * every frame, so nothing here allocates while animating.
 */
class Frame {
    /** The state being shown, and the one we're leaving during a transition. */
    var look: BatteryState = BatteryState.Mid
    var previous: BatteryState = BatteryState.Mid

    /** 0 at the start of a state change, 1 once it has settled. */
    var transition: Float = 1f

    /** Battery level 0..100, already smoothed for the gauge. */
    var level: Float = 50f

    /** 1 while the eyes are shut for a blink. */
    var blink: Boolean = false

    /** Vertical bob in units, about ±0.5. */
    var bob: Float = 0f

    /** Charging loop frame, 0 until [CHARGE_FRAMES]-1. */
    var chargeFrame: Int = 0

    /** 0..1 slow pulse for Critical, 0 otherwise. */
    var pulse: Float = 0f

    /** Free-running time, for anything else a character wants to loop. */
    var timeMs: Long = 0L

    companion object {
        const val CHARGE_FRAMES = 10
    }
}
