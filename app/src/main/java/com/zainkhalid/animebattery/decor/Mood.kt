package com.zainkhalid.animebattery.decor

import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.cast.Cast
import com.zainkhalid.animebattery.cast.Event
import kotlin.random.Random

/**
 * How the character feels, one per [BatteryState]. The overlay draws the effect
 * (sweat, zzz, steam...) round the sticker; the app shows the word and colour.
 */
enum class Mood(val word: String, val color: Long, val tiltDeg: Float) {
    Hyped("hyped", 0xFFFFD23F, 0f),
    Chill("chill", 0xFF4CC9FF, 0f),
    Meh("meh", 0xFFB69CFF, 4f),
    Tired("tired", 0xFFFF9F5A, 7f),
    Fainting("fainting", 0xFFFF6B5A, 14f),
    PoweringUp("powering up", 0xFFC6FF3D, 0f),
    FullPower("full power", 0xFFFFD23F, 0f),
    Sleeping("sleeping", 0xFFB69CFF, 10f),
    Overheating("overheating", 0xFFFF6B5A, 0f),
    /** Not a battery state: shown for a moment after the head hits the camera. */
    Hurt("ouch", 0xFFFF6B5A, -8f);

    /** Only a reaction to something you did, never from the battery. */
    val reactionOnly: Boolean get() = this == Hurt

    companion object {
        fun of(state: BatteryState): Mood = when (state) {
            BatteryState.Full -> Hyped
            BatteryState.Good -> Chill
            BatteryState.Mid -> Meh
            BatteryState.Low -> Tired
            BatteryState.Critical -> Fainting
            BatteryState.Charging -> PoweringUp
            BatteryState.Charged -> FullPower
            BatteryState.PowerSaver -> Sleeping
            BatteryState.Hot -> Overheating
        }

        /** A state that shows each mood, for previews. */
        fun sample(m: Mood): BatteryState = BatteryState.entries.firstOrNull { of(it) == m } ?: BatteryState.Mid
    }
}

/**
 * Speech bubble lines. Built-in characters ([Cast]) have their own voice; anyone else
 * (e.g. a character you made) gets these generic ones.
 */
object Lines {
    private val pools = mapOf(
        Mood.Hyped to listOf("full send today", "we're so back", "main character hours", "can't stop won't stop"),
        Mood.Chill to listOf("vibing", "all good here", "just chilling", "no thoughts, just vibes"),
        Mood.Meh to listOf("halfway there ig", "it's giving mid", "lowkey fine", "still got it (kinda)"),
        Mood.Tired to listOf("kinda tired ngl", "low key need a charger", "running on snacks", "nap soon?"),
        Mood.Fainting to listOf("i'm running on vibes rn", "plug me in pls", "this is my villain arc", "tell my snacks i loved them"),
        Mood.PoweringUp to listOf("power up!!", "ok ok i'm healing", "eating good rn", "charging…"),
        Mood.FullPower to listOf("100%. main character energy", "fully charged, fully slay", "unplug me, i'm ready"),
        Mood.Sleeping to listOf("zzz… saver mode", "shh i'm resting", "low power, low effort"),
        Mood.Overheating to listOf("it's giving sauna", "why am i so toasty", "too hot to handle fr"),
        Mood.Hurt to listOf("ow!!", "that hurt", "ouch"),
    )
    private val generic = mapOf(
        Event.Poke to listOf("hey!", "that tickles", "boop", "what's up?", "hehe", "rude!!"),
        Event.Grab to listOf("woah woah woah", "put me down!", "where are we going", "careful!!"),
        Event.Fling to listOf("wheeee!", "yeet!!", "i can fly", "aaaaa"),
        Event.Dizzy to listOf("everything's spinning…", "i see stars", "never again", "too many rounds"),
        Event.Cheer to listOf("yay!!", "let's gooo", "double boop!"),
        Event.Hide to listOf("brb", "you can't see me", "hiding in the camera"),
        Event.Peek to listOf("peekaboo!", "missed me?", "i'm back"),
        Event.Hurt to listOf("OW! the camera!", "my head…", "that's a bonk", "ouch ouch ouch"),
    )

    private val random = Random(System.nanoTime())
    private var last: String? = null

    fun forMood(m: Mood, who: String?): String =
        pick(Cast.find(who)?.moods?.get(m) ?: pools.getValue(m))

    fun on(e: Event, who: String?): String =
        pick(Cast.find(who)?.events?.get(e) ?: generic.getValue(e))

    private fun pick(pool: List<String>): String {
        var s = pool[random.nextInt(pool.size)]
        if (s == last && pool.size > 1) s = pool[(pool.indexOf(s) + 1) % pool.size]
        last = s
        return s
    }
}
