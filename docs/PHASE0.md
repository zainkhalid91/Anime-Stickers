# Phase 0 spike: results (2026-10-03)

Device: Pixel 8 Pro (husky), Android 17, API 37, build CP3A.260905.009 (QPR1).
Display: 1008x2244 resolution setting, display size density 306 (360 default).

## 1. Hiding the stock battery with icon_blacklist: does NOT work on this build

| Test | Result |
| --- | --- |
| `icon_blacklist=battery` | Battery still shown ([phase0_blacklist_battery.png](phase0_blacklist_battery.png)) |
| `icon_blacklist=wifi,battery` | Wi-Fi hidden, battery still shown ([phase0_blacklist_wifi_battery.png](phase0_blacklist_wifi_battery.png)) |
| Restored to `null` | Original state back |

Why: `dumpsys activity service com.android.systemui/.SystemUIService` lists the
icon slots, and the battery slot has nothing attached:

```
37:(wifi) holder=StatusBarIconHolder(type=WIFI_NEW tag=0 visible=true)
39:(battery) holder=null
```

On Android 17 the battery (the new pill with the percentage inside) is drawn by
`com.android.systemui.statusbar.pipeline.battery`, outside the icon controller
that reads icon_blacklist. So the blacklist mechanism still works for other
icons, but it can no longer hide the battery. Per the brief, I stopped here.

Steps 2 (accessibility overlay) and 3 (battery broadcast) are coded in the spike
but not run on the phone yet. The overlay service needs to be switched on by
hand in Accessibility settings.

## Build notes

- Gradle failed with `Unable to establish loopback connection` from the agent shell.
  Cause: Java NIO makes a Unix socket in %TEMP%, and the session TEMP path was too long.
  Fix: `TEMP=D:\tmp`, `-Djdk.net.unixdomain.tmpdir=D:\tmp`. Builds from Android Studio are unaffected.
- `settings.gradle.kts` regex lost its escaping going through a shell heredoc.
  Edit Kotlin script files with the editor, not heredocs.
