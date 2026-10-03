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

## 2. Accessibility overlay above the status bar: works

TYPE_ACCESSIBILITY_OVERLAY draws above the status bar. The status bar is now a
Compose view, and its accessibility tree gives the stock battery's exact bounds:

```
system_icons  Rect(787, 45 - 930, 70)
  statusIcons Rect(787, 46 - 861, 69)   cellular + wifi
  battery     Rect(872, 45 - 930, 70)   "Battery charging, 53 percent." (pill 872-918 + bolt)
```

| Check | Result |
| --- | --- |
| Portrait, red square on measured bounds | Pixel exact ([phase0_square_portrait.png](phase0_square_portrait.png)) |
| Landscape | Pixel exact at Rect(2108, 15 - 2166, 40) ([phase0_square_landscape.png](phase0_square_landscape.png)) |
| Guess from SystemUI dimens instead of measuring | Wrong (933-1000 vs 872-930). Measure, don't guess. |
| Shade pulled down | Square lands on the Wi-Fi icon in the shade header, so we must hide ([phase0_square_shade.png](phase0_square_shade.png)) |
| Light status bar | Not done yet: Chrome is in dark theme on this phone. Redo with a light app in Phase 1. |

Shade detection: with the shade down, the 113 px SystemUI status bar window disappears
and SystemUI shows one full-screen TYPE_SYSTEM window (0,0-1008,2244). That is the signal.

Bugs found:
- Measuring right after a rotation read the old (portrait) bounds, because the node tree
  wasn't updated yet. Fix: measure after the status bar window bounds match the new rotation.
- Switching the service on also turned on its floating accessibility shortcut button
  (accessibility_button_targets). Onboarding has to tell people to turn the shortcut off.

## 3. Battery state: works

The sticky ACTION_BATTERY_CHANGED gives level, status, plugged and temperature right away:
`level=52 status=2 plugged=1 temp=35.8 saver=false`, and updates arrive as it charges.

## Build notes

- Gradle failed with `Unable to establish loopback connection` from the agent shell.
  Cause: Java NIO makes a Unix socket in %TEMP%, and the session TEMP path was too long.
  Fix: `TEMP=D:\tmp`, `-Djdk.net.unixdomain.tmpdir=D:\tmp`. Builds from Android Studio are unaffected.
- `settings.gradle.kts` regex lost its escaping going through a shell heredoc.
  Edit Kotlin script files with the editor, not heredocs.
