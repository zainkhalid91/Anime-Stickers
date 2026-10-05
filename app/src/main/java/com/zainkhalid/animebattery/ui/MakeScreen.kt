package com.zainkhalid.animebattery.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.cast.CustomCast
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.ui.kit.BackHeader
import com.zainkhalid.animebattery.ui.kit.PopButton
import com.zainkhalid.animebattery.ui.kit.PopCard
import com.zainkhalid.animebattery.ui.kit.PopIcons
import com.zainkhalid.animebattery.ui.kit.SectionTitle
import com.zainkhalid.animebattery.ui.kit.hardShadow
import com.zainkhalid.animebattery.ui.kit.stickerPainter
import kotlinx.coroutines.launch

/** Make a character from a photo: pick, cut out on the phone, name it, use it. */
@Composable
fun MakeScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sticker by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var mine by remember { mutableStateOf(CustomCast.list(context)) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        scope.launch {
            runCatching { CustomCast.makeSticker(context, uri) }
                .onSuccess { if (it == null) error = "Couldn't find anyone in that photo. Try one where they stand out from the background." else sticker = it }
                .onFailure { error = "The cutout didn't work (${it.message}). The first time, Android downloads the cutout model, so try again in a minute." }
            busy = false
        }
    }

    BackHeader("Make your own", onClose)
    Text(
        "Pick a photo of your pet, your OC or some fan art. It's cut out on your phone and turned into a sticker that lives in your status bar.",
        style = MaterialTheme.typography.bodyLarge, color = Pop.TextDim,
    )

    val shape = RoundedCornerShape(Pop.RadiusCard)
    Box(
        Modifier.fillMaxWidth().height(280.dp).padding(end = 3.dp, bottom = Pop.ShadowCard)
            .hardShadow(Pop.RadiusCard, dx = 3.dp, dy = Pop.ShadowCard)
            .clip(shape).background(Brush.verticalGradient(listOf(Color(0xFF3A2357), Pop.Surface)))
            .border(Pop.Outline, Pop.Ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        val s = sticker
        when {
            busy -> CircularProgressIndicator(color = Pop.Pink)
            s != null -> Image(s.asImageBitmap(), contentDescription = "Your new sticker", modifier = Modifier.padding(20.dp))
            else -> Text("your sticker shows up here", style = MaterialTheme.typography.titleSmall, color = Pop.TextDim)
        }
    }
    error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Pop.Coral) }

    PopButton(
        if (sticker == null) "Pick a photo" else "Pick another photo",
        onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        enabled = !busy, color = if (sticker == null) Pop.Pink else Pop.SurfaceHigh,
        contentColor = if (sticker == null) Pop.Ink else Pop.Text, icon = PopIcons.Image, modifier = Modifier.fillMaxWidth(),
    )

    sticker?.let { s ->
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(18) }, singleLine = true,
            label = { Text("Name") }, placeholder = { Text("My buddy") },
            shape = RoundedCornerShape(Pop.RadiusTile), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pop.Pink, unfocusedBorderColor = Pop.Line, focusedLabelColor = Pop.Pink,
                cursorColor = Pop.Pink, focusedTextColor = Pop.Text, unfocusedTextColor = Pop.Text,
            ),
        )
        PopButton(
            "Save and put it in my status bar",
            onClick = {
                val member = CustomCast.add(context, name, s)
                AppSettings(context).characterId = member.id
                onClose()
            },
            color = Pop.Lime, icon = PopIcons.Check, modifier = Modifier.fillMaxWidth(),
        )
    }

    if (mine.isNotEmpty()) {
        SectionTitle("Made by you")
        mine.forEach { m ->
            PopCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(stickerPainter(m.id), contentDescription = null, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(m.name, style = MaterialTheme.typography.titleMedium, color = Pop.Text, modifier = Modifier.weight(1f))
                    PopButton("Delete", onClick = {
                        val settings = AppSettings(context)
                        if (settings.characterId == m.id) settings.characterId = "naruto"
                        CustomCast.remove(context, m.id)
                        mine = CustomCast.list(context)
                    }, color = Pop.Coral, modifier = Modifier.width(110.dp))
                }
            }
        }
    }
}
