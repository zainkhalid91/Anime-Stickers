package com.zainkhalid.animebattery.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zainkhalid.animebattery.settings.AppSettings
import kotlin.math.roundToInt

/** Controls for what's drawn in the real status bar. Changes apply live. */
@Composable
fun LiveBarCard(modifier: Modifier = Modifier) {
    val settings = AppSettings(LocalContext.current)
    var mode by remember { mutableStateOf(settings.barMode) }
    var size by remember { mutableFloatStateOf(settings.characterSizeDp) }

    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Tokens.Outline, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(Tokens.Gap), verticalArrangement = Arrangement.spacedBy(Tokens.GapSmall)) {
            Text("Live status bar", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(AppSettings.MODE_FULL to "Full custom bar", AppSettings.MODE_BADGE to "Battery badge")
                options.forEachIndexed { i, (key, label) ->
                    SegmentedButton(
                        selected = mode == key,
                        onClick = { mode = key; settings.barMode = key },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            if (mode == AppSettings.MODE_FULL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Character size", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${size.roundToInt()}dp", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
                }
                Slider(
                    value = size, valueRange = 24f..56f, steps = 15,
                    onValueChange = { size = it },
                    onValueChangeFinished = { settings.characterSizeDp = size },
                )
            }
        }
    }
}
