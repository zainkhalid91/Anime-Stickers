package com.zainkhalid.animebattery.system

/**
 * Pure string logic for Settings.Secure "icon_blacklist".
 * The value is a comma separated list of slot names, or null when unset.
 */
object IconBlacklist {
    const val KEY = "icon_blacklist"
    const val BATTERY = "battery"

    fun parse(value: String?): List<String> =
        value.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun contains(value: String?, slot: String = BATTERY): Boolean = slot in parse(value)

    /** Adds the slot, keeping everything else in the same order. */
    fun withSlot(value: String?, slot: String = BATTERY): String {
        val slots = parse(value)
        return if (slot in slots) slots.joinToString(",") else (slots + slot).joinToString(",")
    }

    /** Removes the slot. Returns null when nothing is left, so the setting goes back to unset. */
    fun withoutSlot(value: String?, slot: String = BATTERY): String? =
        parse(value).filter { it != slot }.joinToString(",").ifEmpty { null }

    /**
     * What to write back when the app is turned off.
     * If the user already hid the battery before we touched anything, leave it hidden.
     * Otherwise take the current value (the user may have hidden other icons since)
     * and only drop our battery entry.
     */
    fun restored(current: String?, originalHadBattery: Boolean): String? =
        if (originalHadBattery) current else withoutSlot(current)
}
