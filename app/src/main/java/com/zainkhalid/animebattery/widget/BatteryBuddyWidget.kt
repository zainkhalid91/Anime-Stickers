package com.zainkhalid.animebattery.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.zainkhalid.animebattery.MainActivity
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.battery.BatteryStateMachine
import com.zainkhalid.animebattery.settings.AppSettings
import com.zainkhalid.animebattery.system.BatteryReader
import kotlin.math.roundToInt

/**
 * Home screen widget. One widget, three looks picked by size: pill (2x1), mood face
 * (2x2) and battery buddy (4x2). The art is a bitmap from [WidgetArt].
 *
 * Updated by the overlay service when the level or state changes (see [refresh]),
 * when the app opens, and every 30 minutes as a fallback (updatePeriodMillis), so it
 * can't sit on an old number if the service isn't running (e.g. after a force stop).
 */
class BatteryBuddyWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(DpSize(140.dp, 64.dp), DpSize(150.dp, 150.dp), DpSize(300.dp, 150.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val context = LocalContext.current
        val size = LocalSize.current
        val d = context.resources.displayMetrics.density
        val (level, state) = readBattery(context)
        val art = WidgetArt.render(
            context, AppSettings(context).characterId,
            (size.width.value * d).roundToInt(), (size.height.value * d).roundToInt(), d, level, state,
        )
        Image(
            provider = ImageProvider(art),
            contentDescription = "Battery ${level} percent, ${WidgetArt.mood(state)}",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
        )
    }

    companion object {
        fun readBattery(context: Context): Pair<Int, BatteryState> {
            val snap = BatteryReader.read(context).snapshot
            return snap.level to BatteryStateMachine().update(snap)
        }

        /** Redraw every placed widget. Call only when level or state changed. */
        suspend fun refresh(context: Context) = BatteryBuddyWidget().updateAll(context)
    }
}

class BatteryBuddyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryBuddyWidget()
}
