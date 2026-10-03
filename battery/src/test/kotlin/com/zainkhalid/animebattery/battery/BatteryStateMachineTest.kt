package com.zainkhalid.animebattery.battery

import com.zainkhalid.animebattery.battery.BatteryState.Charged
import com.zainkhalid.animebattery.battery.BatteryState.Charging
import com.zainkhalid.animebattery.battery.BatteryState.Critical
import com.zainkhalid.animebattery.battery.BatteryState.Full
import com.zainkhalid.animebattery.battery.BatteryState.Good
import com.zainkhalid.animebattery.battery.BatteryState.Hot
import com.zainkhalid.animebattery.battery.BatteryState.Low
import com.zainkhalid.animebattery.battery.BatteryState.Mid
import com.zainkhalid.animebattery.battery.BatteryState.PowerSaver
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStateMachineTest {

    private fun level(l: Int) = BatterySnapshot(level = l, plugged = false)
    private fun fresh(s: BatterySnapshot) = BatteryStateMachine().update(s)

    @Test fun `band edges match the spec`() {
        mapOf(
            0 to Critical, 5 to Critical, 6 to Low, 20 to Low, 21 to Mid, 50 to Mid,
            51 to Good, 80 to Good, 81 to Full, 100 to Full,
        ).forEach { (l, expected) -> assertEquals("level $l", expected, fresh(level(l))) }
    }

    @Test fun `out of range levels are clamped`() {
        assertEquals(Critical, fresh(level(-3)))
        assertEquals(Full, fresh(level(140)))
    }

    @Test fun `plugged below 100 is charging, at 100 is charged`() {
        assertEquals(Charging, fresh(BatterySnapshot(3, plugged = true)))
        assertEquals(Charging, fresh(BatterySnapshot(99, plugged = true)))
        assertEquals(Charged, fresh(BatterySnapshot(100, plugged = true)))
        assertEquals(Charged, fresh(BatterySnapshot(98, plugged = true, full = true)))
    }

    @Test fun `power saver shows over level bands but not over critical`() {
        assertEquals(PowerSaver, fresh(BatterySnapshot(60, plugged = false, powerSave = true)))
        assertEquals(PowerSaver, fresh(BatterySnapshot(15, plugged = false, powerSave = true)))
        assertEquals(Critical, fresh(BatterySnapshot(4, plugged = false, powerSave = true)))
    }

    @Test fun `hot wins over everything`() {
        assertEquals(Hot, fresh(BatterySnapshot(50, plugged = true, temperatureC = 42f)))
        assertEquals(Hot, fresh(BatterySnapshot(3, plugged = false, temperatureC = 45f, powerSave = true)))
        assertEquals(Mid, fresh(BatterySnapshot(50, plugged = false, temperatureC = 41.9f)))
    }

    @Test fun `hot clears only below 40`() {
        val m = BatteryStateMachine()
        assertEquals(Hot, m.update(BatterySnapshot(50, false, temperatureC = 42.5f)))
        assertEquals(Hot, m.update(BatterySnapshot(50, false, temperatureC = 41f)))
        assertEquals(Hot, m.update(BatterySnapshot(50, false, temperatureC = 40f)))
        assertEquals(Mid, m.update(BatterySnapshot(50, false, temperatureC = 39.9f)))
        assertEquals(Mid, m.update(BatterySnapshot(50, false, temperatureC = 41.9f)))
    }

    @Test fun `bouncing across a boundary does not flicker`() {
        val m = BatteryStateMachine()
        assertEquals(Mid, m.update(level(50)))
        listOf(51, 50, 52, 51, 50, 49).forEach { assertEquals("level $it", Mid, m.update(level(it))) }
        assertEquals(Good, m.update(level(53)))
        listOf(52, 51, 50, 49).forEach { assertEquals("level $it", Good, m.update(level(it))) }
        assertEquals(Mid, m.update(level(48)))
    }

    @Test fun `big jumps skip straight to the right band`() {
        val m = BatteryStateMachine()
        assertEquals(Critical, m.update(level(2)))
        assertEquals(Full, m.update(level(95)))
        assertEquals(Low, m.update(level(10)))
    }

    @Test fun `band memory survives charging`() {
        // Unplugging at 51 after being Good keeps Good, not a fresh Mid/Good decision.
        val m = BatteryStateMachine()
        assertEquals(Good, m.update(level(60)))
        assertEquals(Charging, m.update(BatterySnapshot(50, plugged = true)))
        assertEquals(Good, m.update(level(50)))
    }

    @Test fun `critical and low share a boundary with hysteresis`() {
        val m = BatteryStateMachine()
        assertEquals(Low, m.update(level(7)))
        assertEquals(Low, m.update(level(5)))
        assertEquals(Low, m.update(level(4)))
        assertEquals(Critical, m.update(level(3)))
        assertEquals(Critical, m.update(level(7)))
        assertEquals(Low, m.update(level(8)))
    }
}
