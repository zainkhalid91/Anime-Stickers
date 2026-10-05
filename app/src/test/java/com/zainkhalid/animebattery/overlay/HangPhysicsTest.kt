package com.zainkhalid.animebattery.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HangPhysicsTest {

    private fun settle(p: HangPhysics, maxSeconds: Float = 20f): Float {
        var t = 0f
        while (p.active && t < maxSeconds) { p.step(1 / 60f); t += 1 / 60f }
        return t
    }

    @Test fun startsAtRest() {
        assertFalse(HangPhysics().active)
    }

    @Test fun pokeSwingsThenSettles() {
        val p = HangPhysics().apply { restLength = 120f }
        p.poke(1f)
        assertTrue(p.active)
        p.step(0.1f)
        assertTrue("swings right", p.theta > 0f)
        val t = settle(p)
        assertFalse("settled within ${t}s", p.active)
        assertEquals(0f, p.theta, 0f)
        assertEquals(0f, p.stretch, 0f)
    }

    @Test fun releaseKeepsTheFlick() {
        val p = HangPhysics().apply { restLength = 120f }
        p.grab()
        // Drag from straight down to the right over 50 ms.
        for (i in 0..5) p.drag(i * 20f, 140f, 20f, i * 0.01f)
        p.release(0.05f)
        assertTrue("moving right after release", p.omega > 1f)
    }

    @Test fun pullingStretchesTheRopeButNotForever() {
        val p = HangPhysics().apply { restLength = 100f }
        p.grab()
        p.drag(0f, 10_000f, 0f, 0f)
        assertTrue(p.stretch > 100f)
        assertTrue("rubber band caps the stretch", p.stretch < 350f)
        p.release(0.1f)
        settle(p)
        assertEquals(0f, p.stretch, 0f)
    }

    @Test fun bigFlickLoopsRoundTheCameraAndCountsSpins() {
        val p = HangPhysics().apply { restLength = 80f }
        p.grab()
        for (i in 0..5) p.drag(i * 60f, 90f - i * 10f, 0f, i * 0.008f)
        p.release(0.04f)
        var maxSpins = 0f
        var t = 0f
        while (p.active && t < 20f) { p.step(1 / 60f); t += 1 / 60f; maxSpins = maxOf(maxSpins, abs(p.spins)) }
        assertTrue("went over the top at least once ($maxSpins)", maxSpins >= 1f)
    }

    @Test fun pullAndLetGoBonksTheCamera() {
        val p = HangPhysics().apply { restLength = 100f; cameraLength = 15f }
        // A long slingshot pull, then let go.
        p.grab()
        p.drag(0f, 500f, 0f, 0f)
        p.release(0.2f)
        var hit = 0f
        var t = 0f
        while (p.active && t < 5f) { p.step(1 / 60f); t += 1 / 60f; hit = maxOf(hit, p.takeHit()) }
        assertTrue("slammed into the lens ($hit)", hit > 0f)
        assertTrue("never went through it", p.length >= 15f - 0.01f)
    }

    @Test fun gentleBoopDoesNotHurt() {
        val p = HangPhysics().apply { restLength = 100f; cameraLength = 15f }
        p.poke(1f)
        var hit = 0f
        var t = 0f
        while (p.active && t < 5f) { p.step(1 / 60f); t += 1 / 60f; hit = maxOf(hit, p.takeHit()) }
        assertEquals(0f, hit, 0f)
    }

    @Test fun pushingIntoTheCameraBonksOncePerPush() {
        val p = HangPhysics().apply { restLength = 100f; cameraLength = 15f }
        p.grab()
        p.drag(0f, 5f, 0f, 0f)
        assertTrue(p.takeHit() > 0f)
        p.drag(0f, 4f, 0f, 0.02f)
        assertEquals("still pressed in, no second bonk", 0f, p.takeHit(), 0f)
        assertTrue(p.length >= 15f - 0.01f)
    }

    @Test fun hidingIsNotABonk() {
        val p = HangPhysics().apply { restLength = 100f; cameraLength = 15f; collide = false; hidden = true }
        var hit = 0f
        p.step(0.016f)
        while (p.active) { p.step(1 / 60f); hit = maxOf(hit, p.takeHit()) }
        assertEquals(0f, hit, 0f)
    }

    @Test fun hidingGoesIdleInsideTheCamera() {
        val p = HangPhysics().apply { restLength = 100f }
        p.hidden = true
        p.step(0.016f)
        assertTrue(p.active)
        settle(p)
        assertFalse("doesn't keep ticking while hidden", p.active)
        assertTrue("rope pulled up", p.length < 20f)
        p.hidden = false
        p.step(0.016f)
        assertTrue(p.active)
        settle(p)
        assertEquals(100f, p.length, 0.01f)
    }
}
