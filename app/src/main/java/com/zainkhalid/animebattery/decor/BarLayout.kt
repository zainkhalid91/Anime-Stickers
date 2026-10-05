package com.zainkhalid.animebattery.decor

import org.json.JSONArray
import org.json.JSONObject

/** How the character shows up in the custom bar. */
enum class Pose {
    /** Hangs from the camera on a thread and sways. Lower battery = longer thread. */
    Hanging,
    /** Stands at the right end, after the battery. */
    Battery,
    /** Stands just right of the camera. */
    Camera,
    /** No character; big anime eyes either side of the camera instead. */
    Eyes,
}

/** Things you can stick on the bar. All drawn in code, so they stay sharp at any size. */
enum class DecorType(val label: String) {
    Sparkle("Sparkle"),
    Heart("Heart"),
    Star("Star"),
    Spider("Spider"),
    Web("Web"),
    Ghost("Ghost"),
    Cloud("Cloud"),
    Leaf("Leaf"),
    Wings("Wings"), // sits on the battery icon wherever it is
    Pumpkin("Pumpkin"),
    Bat("Bat"),
    Candy("Candy"),
    WitchHat("Witch hat"), // worn by the character
    ;

    /** Worn or attached to something, so there's one at most and it isn't dragged round. */
    val attached: Boolean get() = this == Wings || this == WitchHat
}

/**
 * One placed decoration. [x] is a fraction of the bar width (0..1), [y] is dp from
 * the top of the bar (can go below it), [scale] 0.5..3.
 */
data class Decor(val type: DecorType, val x: Float, val y: Float, val scale: Float = 1f)

/** Everything the custom bar draws apart from the system bits. Saved as JSON. */
data class BarLayout(
    val pose: Pose = Pose.Hanging,
    val characterSizeDp: Float = 40f,
    val sway: Boolean = true,
    val decor: List<Decor> = emptyList(),
) {
    fun toJson(): String = JSONObject().apply {
        put("pose", pose.name)
        put("size", characterSizeDp.toDouble())
        put("sway", sway)
        put("decor", JSONArray().apply {
            decor.forEach { d ->
                put(JSONObject().put("t", d.type.name).put("x", d.x.toDouble()).put("y", d.y.toDouble()).put("s", d.scale.toDouble()))
            }
        })
    }.toString()

    companion object {
        fun fromJson(json: String?): BarLayout? = runCatching {
            val o = JSONObject(json ?: return null)
            val arr = o.optJSONArray("decor") ?: JSONArray()
            BarLayout(
                pose = runCatching { Pose.valueOf(o.getString("pose")) }.getOrDefault(Pose.Hanging),
                characterSizeDp = o.optDouble("size", 40.0).toFloat().coerceIn(24f, 72f),
                sway = o.optBoolean("sway", true),
                decor = (0 until arr.length()).mapNotNull { i ->
                    val d = arr.getJSONObject(i)
                    val type = runCatching { DecorType.valueOf(d.getString("t")) }.getOrNull() ?: return@mapNotNull null
                    Decor(type, d.getDouble("x").toFloat().coerceIn(0f, 1f), d.getDouble("y").toFloat(), d.optDouble("s", 1.0).toFloat().coerceIn(0.5f, 3f))
                },
            )
        }.getOrNull()
    }
}

/** A themed set (shown as a collection): a ready-made layout plus how it looks in the gallery. */
data class ThemeSet(
    val id: String,
    val name: String,
    val blurb: String,
    val colors: Pair<Long, Long>,
    val layout: BarLayout,
    /** Months (1..12) it's a limited drop, or null if it's always around. */
    val season: IntRange? = null,
) {
    fun inSeason(month: Int) = season?.contains(month) == true
}

object ThemeSets {
    val all = listOf(
        ThemeSet(
            "ninja", "Ninja Way", "Naruto hangs from the camera and charges a Rasengan",
            0xFFF97316 to 0xFF7C2D12,
            BarLayout(
                pose = Pose.Hanging, characterSizeDp = 44f,
                decor = listOf(
                    Decor(DecorType.Sparkle, 0.40f, 8f, 1f), Decor(DecorType.Sparkle, 0.60f, 14f, 0.8f),
                    Decor(DecorType.Leaf, 0.30f, 20f, 1f),
                ),
            ),
        ),
        ThemeSet(
            "kawaii", "Kawaii Pastel", "Hearts, clouds and little wings on the battery",
            0xFFFF6FA0 to 0xFF7C3AED,
            BarLayout(
                pose = Pose.Battery, characterSizeDp = 40f,
                decor = listOf(
                    Decor(DecorType.Heart, 0.38f, 10f, 1f), Decor(DecorType.Cloud, 0.58f, 12f, 1.1f),
                    Decor(DecorType.Heart, 0.30f, 26f, 0.7f), Decor(DecorType.Wings, 0f, 0f, 1f),
                ),
            ),
        ),
        ThemeSet(
            "halloween", "Halloween", "Witch hat on, pumpkins out, bats on patrol",
            0xFFFF8A1F to 0xFF2B1D45,
            BarLayout(
                pose = Pose.Hanging, characterSizeDp = 44f,
                decor = listOf(
                    Decor(DecorType.WitchHat, 0f, 0f, 1f),
                    Decor(DecorType.Pumpkin, 0.30f, 14f, 1.1f), Decor(DecorType.Bat, 0.64f, 12f, 1f),
                    Decor(DecorType.Bat, 0.72f, 26f, 0.7f), Decor(DecorType.Candy, 0.38f, 32f, 0.8f),
                ),
            ),
            season = 10..10,
        ),
        ThemeSet(
            "spooky", "Spooky Night", "A spider on a thread, a web in the corner, eyes by the camera",
            0xFF312E81 to 0xFF0F172A,
            BarLayout(
                pose = Pose.Eyes, characterSizeDp = 40f,
                decor = listOf(
                    Decor(DecorType.Web, 0.0f, 0f, 1.2f), Decor(DecorType.Spider, 0.30f, 30f, 1f),
                    Decor(DecorType.Ghost, 0.68f, 10f, 0.9f),
                ),
            ),
        ),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Seasonal drops that are live this month first, then the rest. */
    fun ordered(month: Int) = all.sortedByDescending { it.inSeason(month) }
}
