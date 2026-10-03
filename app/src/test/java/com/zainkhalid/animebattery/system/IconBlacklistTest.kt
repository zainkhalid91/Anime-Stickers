package com.zainkhalid.animebattery.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconBlacklistTest {

    @Test fun `adds battery to an unset value`() {
        assertEquals("battery", IconBlacklist.withSlot(null))
        assertEquals("battery", IconBlacklist.withSlot(""))
    }

    @Test fun `keeps existing entries and their order`() {
        assertEquals("rotate,headset,battery", IconBlacklist.withSlot("rotate,headset"))
    }

    @Test fun `does not add battery twice`() {
        assertEquals("rotate,battery", IconBlacklist.withSlot("rotate,battery"))
    }

    @Test fun `tolerates spaces and empty items`() {
        assertEquals(listOf("rotate", "battery"), IconBlacklist.parse(" rotate, ,battery ,"))
        assertTrue(IconBlacklist.contains("rotate , battery"))
        assertFalse(IconBlacklist.contains("batteryx"))
    }

    @Test fun `removing the last entry goes back to unset`() {
        assertNull(IconBlacklist.withoutSlot("battery"))
    }

    @Test fun `restore drops only our entry`() {
        assertEquals("rotate,headset", IconBlacklist.restored("rotate,battery,headset", originalHadBattery = false))
    }

    @Test fun `restore keeps entries the user added while we were on`() {
        // Original was "rotate"; user hid "zen" meanwhile.
        assertEquals("rotate,zen", IconBlacklist.restored("rotate,battery,zen", originalHadBattery = false))
    }

    @Test fun `restore leaves battery hidden if the user had hidden it before us`() {
        assertEquals("battery,rotate", IconBlacklist.restored("battery,rotate", originalHadBattery = true))
    }

    @Test fun `restore from just battery returns null`() {
        assertNull(IconBlacklist.restored("battery", originalHadBattery = false))
    }
}
