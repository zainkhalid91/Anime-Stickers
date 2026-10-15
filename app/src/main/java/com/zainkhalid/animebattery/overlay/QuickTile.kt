package com.zainkhalid.animebattery.overlay

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zainkhalid.animebattery.settings.AppSettings

/**
 * Quick Settings tile: one tap turns the whole thing off (or back on) from the
 * notification shade. It flips the same "paused" setting as the app's master switch;
 * the overlay service listens and takes its windows down straight away.
 */
class QuickTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        OverlayHealth.recover(this)
        render()
    }

    override fun onClick() {
        super.onClick()
        val settings = AppSettings(this)
        settings.paused = !settings.paused
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val on = !AppSettings(this).paused
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (on) "On" else "Off"
        tile.updateTile()
    }
}
