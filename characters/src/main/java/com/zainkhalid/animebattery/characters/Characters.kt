package com.zainkhalid.animebattery.characters

import com.zainkhalid.animebattery.characters.naruto.Naruto

/** Every character, in gallery order. */
object Characters {
    val all: List<CharacterArt> = listOf(Naruto)

    fun byId(id: String?): CharacterArt = all.firstOrNull { it.id == id } ?: all.first()
}
