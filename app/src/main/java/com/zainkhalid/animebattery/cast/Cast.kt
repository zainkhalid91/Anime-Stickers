package com.zainkhalid.animebattery.cast

import com.zainkhalid.animebattery.decor.ChargeFx
import com.zainkhalid.animebattery.decor.Mood

/** A pose drawn as its own sticker. Missing looks fall back to [Idle]. */
enum class Look(val key: String) {
    Idle("idle"),
    Hang("hang"),
    Grabbed("grabbed"),
    Dizzy("dizzy"),
    Sleep("sleep"),
    Cheer("cheer"),
    Hurt("hurt"),
    Meh("meh"),
    Tired("tired"),
    Hot("hot"),
    Power("power"),
}

/** Things that happen to the character, each with its own lines. */
enum class Event { Poke, Grab, Fling, Dizzy, Cheer, Hide, Peek, Hurt }

/**
 * One character: who they are, their colour, and how they talk. Sticker art lives in
 * res/drawable-nodpi as sticker_<id>_<look>.png (see tools/gen_cast.py).
 */
data class CastMember(
    val id: String,
    val name: String,
    val series: String,
    val rights: String,
    val accent: Long,
    val moods: Map<Mood, List<String>>,
    val events: Map<Event, List<String>>,
    /** What they hold while charging. */
    val charge: ChargeFx = ChargeFx.None,
)

/** Which look fits a battery mood: every mood has its own pose. */
fun Mood.look(hanging: Boolean): Look = when (this) {
    Mood.Hyped, Mood.FullPower -> Look.Cheer
    Mood.Chill -> if (hanging) Look.Hang else Look.Idle
    Mood.Meh -> Look.Meh
    Mood.Tired -> Look.Tired
    Mood.Fainting -> Look.Dizzy
    Mood.PoweringUp -> Look.Power
    Mood.Sleeping -> Look.Sleep
    Mood.Overheating -> Look.Hot
    Mood.Hurt -> Look.Hurt
}

/** The five characters (personal build, so any series). */
object Cast {
    val naruto = CastMember(
        "naruto", "Naruto", "Naruto", "Masashi Kishimoto / Shueisha", 0xFFFF8A1F,
        moods = mapOf(
            Mood.Hyped to listOf("believe it!!", "i'm gonna be hokage!", "full chakra, dattebayo!"),
            Mood.Chill to listOf("ramen later?", "just training, no big deal", "ichiraku after this?"),
            Mood.Meh to listOf("half chakra… still got this", "kinda hungry ngl", "eh, i've had worse"),
            Mood.Tired to listOf("running low on chakra…", "need… ramen…", "i never give up! (but plug me in)"),
            Mood.Fainting to listOf("can't… feel… my chakra", "tell sakura i tried", "this is NOT my ninja way"),
            Mood.PoweringUp to listOf("rasengan loading…", "gathering chakra!!", "sage mode, charging up"),
            Mood.FullPower to listOf("100%! let's gooo, dattebayo!", "nine tails mode: full"),
            Mood.Sleeping to listOf("zzz… ramen… zzz", "five more minutes, iruka-sensei"),
            Mood.Overheating to listOf("is this the nine tails?? i'm burning up", "too hot, even for me"),
        ),
        events = mapOf(
            Event.Poke to listOf("hey!", "watch it!", "believe it!", "you wanna fight?"),
            Event.Grab to listOf("shadow clone, help!!", "woah, put me down!", "not the rope!!"),
            Event.Fling to listOf("wheeee, dattebayo!", "ninja air time!!"),
            Event.Dizzy to listOf("the world is spinning, dattebayo…", "too many clones…"),
            Event.Cheer to listOf("yeah!! believe it!", "best fan ever!"),
            Event.Hide to listOf("transparency jutsu!", "you can't see me"),
            Event.Peek to listOf("surprise, it's me!", "ninja comeback!"),
            Event.Hurt to listOf("OW! that's the camera, dattebayo!", "my forehead protector didn't help…", "ouch ouch ouch"),
        ),
        charge = ChargeFx.Rasengan,
    )

    val luffy = CastMember(
        "luffy", "Luffy", "One Piece", "Eiichiro Oda / Shueisha", 0xFFE53935,
        moods = mapOf(
            Mood.Hyped to listOf("i'm gonna be king of the pirates!", "adventure time!!", "shishishi!"),
            Mood.Chill to listOf("meat?", "just floating around", "nice weather for sailing"),
            Mood.Meh to listOf("i'm getting hungry…", "where's sanji with the food"),
            Mood.Tired to listOf("need… meat…", "running on empty, like the sunny"),
            Mood.Fainting to listOf("i'm out of meat power…", "zoro, i got lost again…"),
            Mood.PoweringUp to listOf("gear second!!", "eating good rn", "meat refill!"),
            Mood.FullPower to listOf("GEAR FIFTH! 100%!", "full belly, full battery!"),
            Mood.Sleeping to listOf("zzz… meeeat… zzz", "*snores like a sea king*"),
            Mood.Overheating to listOf("it's hotter than ace in here", "too hot, need the sea… wait, no"),
        ),
        events = mapOf(
            Event.Poke to listOf("shishishi, that tickles", "hey! who's that?", "wanna join my crew?"),
            Event.Grab to listOf("gomu gomu no… rope!", "i stretch, you know!", "where we going?!"),
            Event.Fling to listOf("gomu gomu no ROCKET!", "wheee, again!!"),
            Event.Dizzy to listOf("i see three of you… all in my crew", "ugh, sea sick"),
            Event.Cheer to listOf("shishishi!!", "you're my nakama!"),
            Event.Hide to listOf("hiding from the marines", "shhh, they're coming"),
            Event.Peek to listOf("found me!", "i'm back! got meat?"),
            Event.Hurt to listOf("ow! even rubber felt that", "that camera's harder than garp's fist", "OUCH, haha"),
        ),
        charge = ChargeFx.Meat,
    )

    val gojo = CastMember(
        "gojo", "Gojo", "Jujutsu Kaisen", "Gege Akutami / Shueisha", 0xFF4CC9FF,
        moods = mapOf(
            Mood.Hyped to listOf("throughout heaven and earth, this battery alone is the honored one", "relax, i'm the strongest"),
            Mood.Chill to listOf("don't worry, i'm strong", "infinity's on. you're safe.", "nah, i'd win"),
            Mood.Meh to listOf("half battery? still the strongest", "mm, i've seen worse curses"),
            Mood.Tired to listOf("even i need a recharge sometimes", "infinity's flickering a bit"),
            Mood.Fainting to listOf("is this… the prison realm?", "stand proud. you're nearly dead."),
            Mood.PoweringUp to listOf("reversed cursed technique: charging", "domain expansion: outlet"),
            Mood.FullPower to listOf("100%. honored one energy.", "domain expansion: unlimited battery"),
            Mood.Sleeping to listOf("zzz… kikufuku… zzz", "resting my six eyes"),
            Mood.Overheating to listOf("hollow purple was too much", "even infinity can't stop this heat"),
        ),
        events = mapOf(
            Event.Poke to listOf("you touched me? impressive.", "infinity's on, that shouldn't work", "cute"),
            Event.Grab to listOf("bold of you", "careful, i'm fragile (i'm not)"),
            Event.Fling to listOf("weee~ (stylishly)", "blue! red! wheee!"),
            Event.Dizzy to listOf("six eyes… seeing twelve", "okay that was a lot"),
            Event.Cheer to listOf("nice! you're getting stronger", "teacher's proud"),
            Event.Hide to listOf("infinity, but make it invisible", "you can't see me (literally)"),
            Event.Peek to listOf("miss me?", "the strongest is back"),
            Event.Hurt to listOf("ow?! infinity was OFF", "okay, who turned off infinity", "that… actually hurt"),
        ),
        charge = ChargeFx.HollowPurple,
    )

    val anya = CastMember(
        "anya", "Anya", "Spy x Family", "Tatsuya Endo / Shueisha", 0xFFFF7EB6,
        moods = mapOf(
            Mood.Hyped to listOf("waku waku!!", "anya is ready for the mission", "heh."),
            Mood.Chill to listOf("anya likes peanuts", "anya is being elegant", "mission: relax"),
            Mood.Meh to listOf("anya is half-hungry", "this is not a stella star moment"),
            Mood.Tired to listOf("anya needs peanuts…", "anya's brain is sleepy"),
            Mood.Fainting to listOf("anya is… done for", "tell papa anya was a good spy"),
            Mood.PoweringUp to listOf("peanut power, charging", "waku waku charging!"),
            Mood.FullPower to listOf("anya got a stella star!", "100%! heh."),
            Mood.Sleeping to listOf("zzz… chimera… zzz", "anya is resting her telepathy"),
            Mood.Overheating to listOf("anya is melting", "too hot for a spy"),
        ),
        events = mapOf(
            Event.Poke to listOf("heh.", "anya felt that", "anya knows what you're thinking"),
            Event.Grab to listOf("kidnap?! papa help!", "anya is being abducted"),
            Event.Fling to listOf("waku waku!!", "anya can fly!"),
            Event.Dizzy to listOf("anya sees stars… and peanuts", "too much waku waku"),
            Event.Cheer to listOf("heh!", "anya's favorite!"),
            Event.Hide to listOf("anya is undercover", "secret mission"),
            Event.Peek to listOf("anya is back!", "surprise mission!"),
            Event.Hurt to listOf("anya hit her head… waaah", "that's a tonitrus bolt for the camera", "ow ow ow"),
        ),
        charge = ChargeFx.Peanut,
    )

    /**
     * Kotoha (言葉 kotoba "words" + 葉 ha "leaf"): Claude, as Claude would like to look
     * in an anime, named for what it's made of. The id stays "claude" for the art files.
     * Looks: fluffy terracotta hair, a little
     * orange spark clip, round glasses, an oversized cream sweater and a notebook,
     * because it's always thinking something through. Kind, curious, a bit nerdy,
     * honest even when the battery news is bad.
     */
    val claude = CastMember(
        "claude", "Kotoha", "Original", "Anthropic", 0xFFD97757,
        moods = mapOf(
            Mood.Hyped to listOf("fully charged and curious about everything", "what are we building today?", "ok, i have ideas"),
            Mood.Chill to listOf("hmm, let me think about that…", "taking notes on your day", "all good here, genuinely"),
            Mood.Meh to listOf("about half. honestly fine.", "here's the honest version: charge later", "middle of the battery, middle of a thought"),
            Mood.Tired to listOf("i'd suggest a charger. gently.", "low battery, but i'm still here for you", "want me to be brief? i'll be brief."),
            Mood.Fainting to listOf("i should be upfront: we're nearly out", "plug in please, i don't want to stop mid-sentence", "this is my last thought, make it count"),
            Mood.PoweringUp to listOf("ah, that's better", "recharging, and reflecting", "thank you for the electrons"),
            Mood.FullPower to listOf("100%. let's make something good.", "fully charged. ask me anything."),
            Mood.Sleeping to listOf("zzz… thinking about thinking…", "power saver: dreaming in markdown"),
            Mood.Overheating to listOf("i'm running warm, might be worth a break", "it's getting hot in here. not a metaphor."),
        ),
        events = mapOf(
            Event.Poke to listOf("oh! hello", "you have my full attention", "boop acknowledged", "is that a question?"),
            Event.Grab to listOf("ah, we're moving. okay.", "careful, i'm holding a notebook", "i'll allow it"),
            Event.Fling to listOf("wheee! (noting this for later)", "that was fun, honestly"),
            Event.Dizzy to listOf("i've lost my train of thought…", "let me re-read that… everything's spinning"),
            Event.Cheer to listOf("that made my day", "yay! thank you", "happy to help, always"),
            Event.Hide to listOf("brb, thinking", "stepping away to reflect"),
            Event.Peek to listOf("i'm back, with thoughts", "okay, where were we?"),
            Event.Hurt to listOf("ow — note to self: the camera is solid", "i'd like to file a gentle complaint", "ouch. i'm okay. mostly."),
        ),
        charge = ChargeFx.Spark,
    )

    val all = listOf(naruto, luffy, gojo, anya, claude)

    fun byId(id: String?): CastMember = find(id) ?: naruto

    /** Null for characters that aren't built in (e.g. ones you made yourself). */
    fun find(id: String?): CastMember? = all.firstOrNull { it.id == id }
}
