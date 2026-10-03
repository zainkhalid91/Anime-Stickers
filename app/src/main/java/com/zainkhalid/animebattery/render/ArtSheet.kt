package com.zainkhalid.animebattery.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.zainkhalid.animebattery.battery.BatteryState
import com.zainkhalid.animebattery.characters.BadgeLayout
import com.zainkhalid.animebattery.characters.CharacterArt
import com.zainkhalid.animebattery.characters.Frame
import java.io.File

/**
 * Debug: renders every state of a character into one PNG for review.
 * Columns: dark status bar 1x, light status bar 1x, dark at 8x (nearest neighbour).
 * A magenta pill is drawn where the stock battery sits; if any magenta shows, the
 * plate isn't covering it.
 */
object ArtSheet {

    /** Representative level for each state. */
    val SAMPLE_LEVEL = mapOf(
        BatteryState.Full to 92, BatteryState.Good to 66, BatteryState.Mid to 38,
        BatteryState.Low to 14, BatteryState.Critical to 3, BatteryState.Charging to 47,
        BatteryState.Charged to 100, BatteryState.PowerSaver to 30, BatteryState.Hot to 58,
    )

    private const val ZOOM = 8
    private val DARK = Color.rgb(16, 16, 20)
    private val LIGHT = Color.rgb(243, 238, 242)

    fun render(context: Context, art: CharacterArt, chargeFrame: Int = 3): File {
        val d = context.resources.displayMetrics.density
        val cropW = (84 * d).toInt()
        val sbH = (59 * d).toInt()
        val painter = BadgePainter(d)
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15 * d; typeface = Typeface.DEFAULT_BOLD }

        val states = BatteryState.entries
        val labelW = (96 * d).toInt()
        val sheetW = labelW + cropW * 2 + cropW * ZOOM + 3 * 8
        val rowH = sbH * ZOOM + 8
        val sheet = Bitmap.createBitmap(sheetW, rowH * states.size, Bitmap.Config.ARGB_8888)
        val sc = Canvas(sheet)
        sc.drawColor(Color.rgb(40, 40, 46))

        states.forEachIndexed { row, state ->
            val f = Frame().apply {
                look = state; previous = state; transition = 1f
                level = SAMPLE_LEVEL.getValue(state).toFloat()
                this.chargeFrame = chargeFrame
            }
            val charging = state == BatteryState.Charging || state == BatteryState.Charged
            val dark = crop(painter, art, f, d, cropW, sbH, DARK, Color.WHITE, charging)
            val light = crop(painter, art, f, d, cropW, sbH, LIGHT, Color.rgb(30, 30, 34), charging)
            val zoom = Bitmap.createScaledBitmap(dark, cropW * ZOOM, sbH * ZOOM, false)

            val y = (row * rowH).toFloat()
            sc.drawText(state.name, 8f, y + 24 * d, label)
            sc.drawBitmap(dark, labelW.toFloat(), y, null)
            sc.drawBitmap(light, (labelW + cropW + 8).toFloat(), y, null)
            sc.drawBitmap(zoom, (labelW + cropW * 2 + 16).toFloat(), y, null)
        }

        val dir = File(context.getExternalFilesDir(null), "sheets").apply { mkdirs() }
        val out = File(dir, "${art.id}.png")
        out.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return out
    }

    /** One status bar crop: fake signal and Wi-Fi, the magenta stock pill, then the badge. */
    fun crop(
        painter: BadgePainter, art: CharacterArt, f: Frame, d: Float,
        w: Int, h: Int, bg: Int, ink: Int, charging: Boolean,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(bg)

        // Stock battery geometry as measured on the Pixel 8 Pro, in dp.
        val stockW = (if (charging) 30.3f else 24f) * d
        val stockH = 13f * d
        val stockRight = w - 10f * d
        val stockTop = (h - stockH) / 2f - 0.5f * d
        val stock = RectF(stockRight - stockW, stockTop, stockRight, stockTop + stockH)
        val wifiRight = stock.left - 5.75f * d

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        c.drawCircle(wifiRight - 8 * d, stock.centerY(), 6.5f * d, p)
        c.drawRect(wifiRight - 30 * d, stock.centerY() - 2 * d, wifiRight - 18 * d, stock.centerY() + 6 * d, p)
        c.drawRoundRect(stock, stockH / 2, stockH / 2, Paint().apply { color = Color.MAGENTA })

        val layout = BadgeLayout().place(
            stock.left, stock.top, stock.right, stock.bottom,
            minLeft = wifiRight + 3f, statusBarHeight = h.toFloat(), screenWidth = w.toFloat(),
            density = d,
        )
        c.save()
        c.translate(layout.windowLeft.toFloat(), layout.windowTop.toFloat())
        painter.paint(c, art, f, layout, showPercent = true)
        c.restore()
        return bmp
    }
}
