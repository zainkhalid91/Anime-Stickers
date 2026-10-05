package com.zainkhalid.animebattery.ui.kit

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.zainkhalid.animebattery.R
import com.zainkhalid.animebattery.cast.Look
import com.zainkhalid.animebattery.render.StickerCache

/** A character's sticker for [look]: your own art if you made it, else the built-in one. */
@Composable
fun stickerPainter(characterId: String, look: Look = Look.Idle): Painter {
    val context = LocalContext.current
    val file = remember(characterId, look) { StickerCache.file(context, characterId, look) }
    if (file != null) {
        val bmp = remember(file.path, file.lastModified()) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
        if (bmp != null) return BitmapPainter(bmp)
    }
    val res = remember(characterId, look) { StickerCache.res(context, characterId, look) }
    return painterResource(if (res != 0) res else R.drawable.sticker_naruto_happy)
}
