# Battery and performance

Target: under **0.5% of battery per hour**. Pixel 8 Pro, 4448 mAh learned capacity, so the
budget is about **22 mAh/h**.

## How we measure

```bash
adb shell settings put global stay_on_while_plugged_in 7   # keep the screen on (restore after!)
adb shell dumpsys battery unplug                            # stats only count while "on battery"
adb shell dumpsys batterystats --reset
# ... leave the phone on the home screen, untouched ...
adb shell dumpsys batterystats > stats.txt
adb shell dumpsys battery reset
python tools/battery_report.py stats.txt --uid u0a558 --capacity 4448
```

Worst case on purpose: screen on the whole time, custom bar, sparkles on, animations on.
Don't open the app during a run: a foreground app is also charged for the screen.

## Results

| Run | Build | Time | App mAh | mAh/h | % battery/h | CPU (user + sys) | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | full bar, 12 fps idle clock | 20 min | 0.70 | 2.09 | **0.047%** | 22.0 s + 10.5 s | clean |
| 2 | 4 fps idle clock | 10 min | 4.51 (bg 0.52) | 27.0 (bg 3.1) | 0.61% (bg 0.07%) | 32.8 s + 10.4 s | invalid: the app was opened for 3m 37s, so screen power was charged to it |

## What keeps it low

- No composition in the overlay: plain Views drawing cached paths and bitmaps.
- Frame clock instead of a render loop. It only invalidates when a visible pixel changes.
  - Idle: wakes 4 times a second plus exactly at each blink.
  - Charging, state changes and the critical pulse: 12 fps.
  - Power saver, animations off, screen off, or hidden: 0 fps.
- Sticker is pre-shrunk once per size and drawn 1:1.
- Bar colour sampling: one accessibility screenshot per app switch, and once after scrolling stops.
  At most one a second, reading a single pixel row. Never during scrolling.
- Everything stops on screen off; the window is removed.
