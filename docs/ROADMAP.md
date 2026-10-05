# Anime Battery: Gen Z roadmap

The plan for making Anime Battery feel like a Gen Z companion app instead of a
settings screen. It covers the research, the new look ("Sticker Pop"), and every
feature, split into phases we build in order.

**Context:** personal use only, so no Play Store and no licensing limits. We can
use any anime character and drop the store-only concerns (accessibility API policy,
loot-box rules, ads).

**Status:** Phase 1 built (2026-10-04); compiles, not yet checked on a device. Phase 2 next.

---

## 1. Research summary

### 1.1 Who and why

| Finding | What it means for us |
| --- | --- |
| ~60% of 13–17 year olds call themselves anime fans (Crunchyroll / NRG study). | Anime isn't niche for this audience. Lean in: manga styling, speech bubbles, speed lines, chibi. |
| 62% of Gen Z have an *oshi* (a favourite character they support). | The character is *theirs*. Bond, dress-up, and "my character" matter more than settings. |
| Character widgets and animated wallpapers are a growing category; personalisation app downloads +14% YoY (Q1 2026). | Our status bar + widget + wallpaper combo is in the right category. |
| Locket hit #1 in 30+ countries by living on the home screen and spreading through TikTok. | Things that live *outside* the app win. Shareable clips are the growth engine. |
| Finch / Pixel Pals / Widgetable: a small pet you care for builds an emotional bond. | We already have a character; it needs to *react*, *talk*, and *grow with you*. |
| Streak screens fell from 14% of apps (2024) to 8.1% (2025); Gen Z reports streak stress. | Use a *bond* that grows and never punishes. No loss for missed days. |

### 1.2 What the #1 apps do with their UI

| App | What to borrow | What to avoid |
| --- | --- | --- |
| **Duolingo** | Chunky "pressable" buttons with a flat 4dp bottom shadow that squashes on press. A 4-colour meaning language (green=go, orange=streak, gold=XP, purple=premium). Thick 2–3dp borders. Rive-animated mascot that reacts to everything. | Guilt-tripping notifications. |
| **Finch** | The pet is the home screen: a stage with the character in the middle, everything else orbits it. Immediate joyful animation after every action. Deep dress-up. Soft pastels. | Home overloaded with shop / upsell entry points. |
| **Widgetable** | Hero image of the pet, bottom sheet for options (size, style), one primary button. Couple/friend widgets. | Endless grid of near-identical widgets. |
| **Pixel Pals** | The pet lives *in system UI* (Dynamic Island) and you can poke it. | — |
| **Locket** | One job, done with zero friction. Social without feeds or follower counts. | — |
| **Gen Z design trend (Neubrutalism)** | Hard ink outlines, flat offset shadows, saturated pop colours, bold type: the "sticker pressed onto the page" look. | Pure brutalism's sharp corners feel harsh; we round them. |

### 1.3 What's wrong with the current UI (v2)

Looked at `docs/v2_home.png`:

1. **It reads as a settings app.** Long scroll of identical outlined cards with ALL-CAPS headers (POSE, COLLECTIONS, QUICK SETTINGS).
2. **The character is tiny and static.** The "friend" is a 64dp image in the header corner. The live preview is mostly empty gradient.
3. **Generic palette.** Tailwind orange + slate navy is the default "SaaS dark mode" and has no anime personality.
4. **System font, no personality** in type.
5. **No motion.** Nothing bounces, nothing reacts to touches, no screen transitions.
6. **No navigation model.** Everything on one page, plus "‹ Back" text buttons.
7. **Developer leftovers** on the home screen (dp numbers, "Ideas / Samples lab").

---

## 2. New design system: "Sticker Pop"

A blend of **Neubrutalism** (Gen Z, sticker-on-page), **Duolingo's tactile buttons**
and **manga** details (halftone, speech bubbles, speed lines). Dark-first, because it's
a phone-customisation app people use at night, and so the character art pops.

Built with the `ui-ux-pro-max` design skill (style: Claymorphism / Neubrutalism hybrid;
fonts: "Claymorphism Mobile" pairing).

### 2.1 Principles

1. **The character is the UI.** Every screen has the character somewhere, reacting.
2. **Everything is a sticker.** Ink outline + flat offset shadow + rounded corners. Things look like they can be peeled off.
3. **Everything responds.** Every tap squashes, every success celebrates, every state change animates.
4. **Talk like a friend.** Short, lowercase-friendly copy with a little attitude. No "Configure your overlay".
5. **No guilt.** Nothing is lost by not opening the app.

### 2.2 Colour tokens (dark)

| Token | Hex | Use |
| --- | --- | --- |
| `Ink` | `#0D0A14` | Outlines, hard shadows, text on pop colours |
| `Bg` | `#16111F` | App background (deep grape-black) |
| `Surface` | `#221A30` | Cards |
| `SurfaceHigh` | `#2E2440` | Raised cards, chips, inputs |
| `Line` | `#3D3152` | Subtle dividers on dark |
| `Text` | `#FFF8F0` | Primary text (warm white) |
| `TextDim` | `#BDB2CF` | Secondary text (≥ 4.5:1 on Surface) |
| `Pink` | `#FF4FA3` | **Primary**: main actions, selection, the brand |
| `Lime` | `#C6FF3D` | Energy / charging / success / bond XP |
| `Sky` | `#4CC9FF` | Info, links, Wi-Fi-ish stuff |
| `Sun` | `#FFD23F` | Rewards, drops, rare items, highlights |
| `Lilac` | `#B69CFF` | Secondary accent, collections |
| `Coral` | `#FF6B5A` | Low battery, danger, destructive |

**Meaning language (Duolingo-style, never colour alone; always paired with an icon or word):**
Pink = you / your character · Lime = power & progress · Sun = rewards & drops ·
Coral = low / warning · Sky = info.

### 2.3 Type

- **Display / headings:** Nunito (Black 900 / ExtraBold 800). Rounded terminals are friendly and anime-ish.
- **Body / UI:** DM Sans (Medium 500, Bold 700).
- Bundled from Google Fonts (OFL) in `res/font` as variable fonts.

| Style | Font | Size / line | Notes |
| --- | --- | --- | --- |
| displayLarge | Nunito 900 | 40 / 44 | Hero, -1 letter spacing |
| headlineMedium | Nunito 900 | 28 / 32 | Screen titles |
| titleLarge | Nunito 800 | 22 / 28 | Card titles |
| titleMedium | Nunito 800 | 17 / 22 | Small card titles |
| bodyLarge | DM Sans 500 | 16 / 24 | Body |
| bodyMedium | DM Sans 500 | 14 / 20 | Secondary |
| labelLarge | DM Sans 700 | 15 / 20 | Buttons |
| labelMedium | DM Sans 700 | 12 / 16 | Chips, tags (sentence case, not ALL CAPS) |

### 2.4 Shape, outline, depth

- Radius: **24dp** cards, **18dp** buttons & tiles, **pill** for chips.
- Outline: **2.5dp** `Ink` on every pop-coloured element; **1.5dp** `Line` on dark cards.
- Shadow: **flat offset**, 4dp down (buttons) / 5dp down-right (cards) in `Ink`. No blur.
- Press: the element moves down into its shadow (translationY = shadow), 90 ms in, spring out.

### 2.5 Motion

Compose `spring()` everywhere instead of tweens, so things feel physical.

| Interaction | Spec |
| --- | --- |
| Button / tile press | Sink into shadow + scale 0.97, `spring(dampingRatio=0.5, stiffness=800)` |
| Selection change | Scale pop 1 → 1.08 → 1, `spring(0.45, 600)` |
| Screen change | Fade + slide 24dp, 220 ms enter / 160 ms exit (exit faster than enter) |
| Character idle (in app) | Breathing scale 1 ↔ 1.03, 2.4 s, plus gentle bob |
| Character tap ("poke") | Squash & stretch 0.85/1.15 then wobble, random reaction bubble |
| Speech bubble | Pop in from tail (scale 0.6 → 1, overshoot), auto-hide after 3.5 s |
| Celebration | Confetti / sparkle burst, ≤ 1 s |
| Reduced motion | Respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0` and the in-app "Animations" toggle: final states only |

### 2.6 Components (in `ui/kit/`)

| Component | Description |
| --- | --- |
| `PopButton` | Chunky pressable button, colour variants (Pink/Lime/Sun/Surface), optional leading icon |
| `PopCard` | Outlined card with hard shadow; optional tint |
| `PopChip` | Pill toggle chip with selection pop |
| `PopTile` | Square selectable tile (poses, moods, collections) |
| `SpeechBubble` | Manga bubble with tail and ink outline |
| `MoodBadge` | Small pill showing the current mood (icon + word) |
| `Halftone` | Manga dot-pattern background modifier |
| `CharacterStage` | Hero area: halftone sky, character with idle + poke, speech bubble, live battery |
| `PopBottomBar` | 4-tab bottom bar with sticker-style selected pill |
| `PopSwitch` | Switch styled to match (thumb with ink outline) |

### 2.7 App structure (new navigation)

Bottom bar, 4 tabs (≤ 5 rule):

| Tab | Icon | Contents |
| --- | --- | --- |
| **Buddy** (home) | heart-face | Character stage (big, alive, reacting to live battery), mood badge, today's line, quick toggles (overlay on/off, animations) |
| **Studio** | brush | Bar editor (existing), poses, decor, size, bar mode |
| **Drops** | gift | Collections / theme sets incl. seasonal drops (Halloween), wallpapers, widget |
| **Me** | user | Settings, setup/permissions, about, lab (dev tools moved here) |

---

## 3. Phases

Each feature lists **what**, **how** (where in the code), and **done when**.

### Phase 1: Glow-up & alive  *(now)*

Goal: the app looks and feels new, and the character visibly *lives*.

#### 1.1 Sticker Pop design system
- **What:** tokens, fonts, Material 3 theme mapping, component kit (§2.6).
- **How:** rewrite `ui/Theme.kt` (`Pop` tokens + `AnimeBatteryTheme`), add `res/font/nunito.ttf`, `res/font/dm_sans.ttf`, new `ui/kit/*.kt`.
- **Done when:** every screen uses kit components; no raw hex in screens except character art.

#### 1.2 New app shell & navigation
- **What:** 4-tab bottom bar (Buddy / Studio / Drops / Me), animated tab switches, system back returns to Buddy.
- **How:** `MainActivity` hosts an `AppShell` composable (`ui/AppShell.kt`); existing screens slot into tabs; Ideas/Lab moves under Me.
- **Done when:** no "‹ Back" text buttons at top level; transitions animate.

#### 1.3 Buddy home screen
- **What:** a big character stage (Finch-style) with the character at ~45% of screen width, halftone sky that tints with mood, live battery %, mood badge, speech bubble with the current mood line, tap-to-poke reactions, setup card when overlay is off, quick toggles.
- **How:** `ui/BuddyScreen.kt`, `ui/kit/CharacterStage.kt`. Reads live battery via a `rememberBatterySnapshot()` composable (sticky `ACTION_BATTERY_CHANGED` receiver) and the same `BatteryStateMachine`.
- **Done when:** plugging the charger flips the stage to the Charging mood within a second.

#### 1.4 Battery moods (overlay)
- **What:** the character reacts to battery state in the real status bar, beyond the existing charging orb:

| State | Mood | Overlay effect (drawn in code on top of the sticker) |
| --- | --- | --- |
| Full (81–100) | Hyped | Occasional sparkle burst, little hop |
| Good (51–80) | Chill | Default idle |
| Mid (21–50) | Meh | Slight droop (sticker tilted 4°) |
| Low (6–20) | Tired | Sweat drop, slower sway |
| Critical (≤5) | Fainting | Tilted 14°, swirl over head, pulse |
| Charging | Powering up | Existing Rasengan orb + lime energy sparks |
| Charged | Full power | Sun-coloured aura, sparkle burst |
| PowerSaver | Sleeping | "z z" floating up, no sway |
| Hot (≥42 °C) | Overheating | Steam puffs + red-tint blush |

- **How:** new `decor/MoodPainter.kt` (allocation-free Canvas painter like `DecorPainter`), `Mood` enum in `decor/Mood.kt` mapping `BatteryState → Mood`. `FullBarView.drawCharacter` applies the mood tilt and calls `MoodPainter` around the sticker. Motion only while the mood's effect is visibly animating (keep the battery budget in `docs/PERFORMANCE.md`).
- **Done when:** `adb ... --es cmd force --ei level 4` shows the fainting mood; preview in Buddy & Studio matches.

#### 1.5 Speech bubbles
- **What:** a short line in a manga bubble below the character when the mood changes, when you plug/unplug, and on unlock (at most once every 20 min). Lines per mood, with variety, e.g.
  - Critical: "i'm literally running on vibes rn", "plug me in or i'm haunting you"
  - Charging: "rasengan loading…", "ok ok i'm healing"
  - Charged: "100%. main character energy."
  - Hot: "it's giving sauna", "why am i so toasty"
  - Low: "low key need a charger", "kinda tired ngl"
- **How:** `decor/Lines.kt` (line pools per `Mood`, no repeats back-to-back), bubble drawn by `MoodPainter.bubble()` in `FullBarView` below the bar; the window grows while a bubble shows (`neededHeight`). Toggle "Speech bubbles" in settings (`AppSettings.speech`).
- **Done when:** bubble appears for ~4 s on plug-in, never overlaps the clock/icons, disappears cleanly.

#### 1.6 Halloween drop
- **What:** new decor (Pumpkin, Bat, Candy, Witch hat on the character) and a "Halloween" collection; Drops tab shows a "Limited: Halloween" banner during Oct.
- **How:** add `DecorType.Pumpkin/Bat/Candy/WitchHat` to `BarLayout.kt`, paint them in `DecorPainter`, new `ThemeSet("halloween", …)`; `Drops` screen shows seasonal sets first by date.
- **Done when:** applying Halloween puts a pumpkin, bats and a witch hat on Naruto in the real bar.

#### Phase 1 status and device check

All six items are in the code and the debug build passes. Still to do on a phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd force --ei level 4      # fainting + bubble
adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd force --ei level 60 --ez plugged true   # powering up
adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd force --ef temp 44       # overheating
adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd say --es text "hi bestie"
adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd unforce
```

Then re-run the battery report in `docs/PERFORMANCE.md` with a moving mood (Tired) to confirm we're still under 0.5%/h.

Where things live: tokens and fonts `ui/Theme.kt`; components `ui/kit/`; tabs `ui/AppShell.kt`; screens `ui/BuddyScreen.kt`, `StudioScreen.kt`, `DropsScreen.kt`, `MeScreen.kt`; moods `decor/Mood.kt` + `decor/MoodPainter.kt`; Halloween art in `decor/DecorPainter.kt`.

---

### Phase 2: Share it

Goal: a one-tap way to show your bar off.

#### 2.1 Share clip export
- **What:** record a 4 s, 1080×1920 vertical clip: the phone-top mock with your bar animating, the character doing its mood, a caption ("my status bar is a whole vibe"), small "Anime Battery" watermark. Share via Android share sheet (TikTok, Reels, Stories, WhatsApp).
- **How:** off-screen render `FullBarView` + backdrop into a `Surface` from `MediaCodec` (H.264, 30 fps) with `MediaMuxer`; or animated WebP/GIF fallback. `FileProvider` + `ACTION_SEND`. New `share/ClipRecorder.kt`.
- **Done when:** clip plays in Google Photos and shares to any app.

#### 2.2 Share card (still image)
- **What:** a poster-style PNG: big character, bar preview, mood, battery %, stats ("days together: 34").
- **How:** Compose → `GraphicsLayer.toImageBitmap()`; share sheet.

#### 2.3 Charging celebration screen
- **What:** when you plug in with the screen on, a full-screen 2.5 s animation (character powering up, % counting up), then it gets out of the way. Tap to dismiss.
- **How:** in `StatusOverlayService`, a temporary full-screen `TYPE_ACCESSIBILITY_OVERLAY` with a lightweight View; respects animations toggle and a setting.

#### 2.4 Haptics & sounds
- **What:** tiny haptic tick on poke and on plug-in; optional sound pack (character voice line on plug-in).
- **How:** `HapticFeedbackConstants` / `VibrationEffect.Composition` (PRIMITIVE_CLICK, PRIMITIVE_SPIN); `SoundPool` for voice lines in `res/raw`.

---

### Phase 3: Bond & collect

Goal: reasons to come back that feel good, never guilty.

#### 3.1 Bond level ("days together")
- **What:** bond XP from: each day the overlay ran, pokes (capped), charging from low to full, opening the app. Levels unlock poses, outfits, decor, bubble line packs. Missing days costs nothing.
- **How:** `bond/BondStore.kt` (DataStore), daily rollup from the service (`ACTION_DATE_CHANGED`), level curve table; Buddy shows a lime progress ring around the character.

#### 3.2 Daily capsule (gacha, free only)
- **What:** one capsule per day on the Buddy screen. Opening it plays a shake + pop animation and reveals a decor/sticker with rarity (Common / Rare / Ultra, colour coded Sky / Lilac / Sun). Duplicates turn into "stardust" to craft a chosen item.
- **How:** `drops/Capsule.kt`, seeded RNG by date, inventory in DataStore.

#### 3.3 Collection book
- **What:** a binder of everything you own and silhouettes of what you don't, by series and season.
- **How:** `ui/CollectionScreen.kt` grid of `PopTile`s.

#### 3.4 Interactive widget
- **What:** tap the Glance widget to poke/pet the character (reaction + bubble); feed it with the charger (charging = eating).
- **How:** Glance `actionRunCallback` → update widget state → re-render with a reaction frame for 3 s.

#### 3.5 Aesthetic presets ("vibes")
- **What:** one tap sets bar decor + wallpaper + widget style together: **Y2K**, **Kawaii pastel**, **Cyber neon**, **Dark academia**, **Cottagecore**, **Spooky**.
- **How:** extend `ThemeSet` with wallpaper + widget style ids; Drops tab "Vibes" row.

#### 3.6 Outfits
- **What:** hats, glasses, scarves layered on the sticker (anchors per character: head top, eyes, neck).
- **How:** anchor points in `CharacterArt` metadata; outfits are decor drawn relative to anchors.

---

### Phase 4: Characters, looks and play  *(built 2026-10-05)*

#### 4.0 Play with the hanging character
- **What:** in the Hanging pose you can touch the character in the real status bar.
  - **Tap:** boop. It swings and says a line. **Double tap:** big happy spin.
  - **Drag:** pull it on a stretchy rope (rubber band, harder the further you pull); let go and it yo-yos and swings back.
  - **Flick:** swing it right round the camera; two or more loops makes it dizzy.
  - **Hold:** it hides inside the camera and peeks back out after 5 s (or when you tap).
  - **Bonk:** if the rope snaps back hard, or you shove it into the lens, its head hits the camera: comic impact burst, a bump, circling stars, an "ouch" line, the hurt look and a haptic.
- **How:** `overlay/HangPhysics.kt` (pendulum + spring rope + body wobble + camera collision, unit tested), `render/StickerMesh.kt` (`drawBitmapMesh` jelly bend/stretch/squash), a small touch window over the character only (`FullBarController.updatePad`, never over the status bar so the shade still opens). Every-vsync frames only while the rope moves, then back to the idle clock. Setting: Buddy, "Play with your buddy".
- **Animation approach:** no frame-by-frame video (no video model on this PC, AI video drifts off-model, and it would cost battery). Instead: a few AI pose stickers per character, real physics, and mesh deformation of one still image.

#### 4.1 The cast: 5 characters, 7 looks, their own voices
- **Who:** Naruto, Luffy, Gojo, Anya, and Kotoha (言葉 "words" + 葉 "leaf"; an original: how Claude would draw itself in an anime: fluffy terracotta hair, orange spark clip, round glasses, oversized cream sweater, notebook; kind, curious, honest).
- **Looks:** idle, hang (holding the rope), grabbed, dizzy, sleep, cheer, hurt. Picked by what's happening: hurt after a bonk, grabbed while held, dizzy after spins or at critical battery, sleep in power saver, cheer when full or double-tapped, else hang/idle. Missing looks fall back to idle.
- **Voices:** every character has its own lines for each mood and each event (poke, grab, fling, dizzy, cheer, hide, peek, hurt) in `cast/Cast.kt`.
- **Art pipeline:** `tools/gen_cast.py` renders every look with the local ComfyUI (Animagine XL 4.0, same recipe as the first Naruto, 832 px, 22 steps) → `tools/cast_stickers.py` cuts them out with `tools/cutout.py` into `res/drawable-nodpi/sticker_<id>_<look>.png` and writes a review sheet. Pick a different seed per look in `art/raw/cast/picks.txt`. New art needs no code: stickers are found by name.
- **Note:** on this laptop GPU (GTX 1660 Ti, 6 GB) one render takes ~4–10 min, more if Gradle or Android Studio is holding RAM. Run `./gradlew --stop` before a batch.

#### 4.1b Per-character touches (2026-10-05)
- **Charging props:** Naruto Rasengan, Luffy meat, Gojo Hollow Purple, Anya peanut, Kotoha a turning orange spark; characters you make get none (`decor/ChargePainter.kt`, `CastMember.charge`).
- **Luffy is rubber:** pulling keeps the rope still and stretches two long rubber arms down to his body while his legs stretch in the art (`CastMember.rubber`).
- **Threads:** forked to both fists only when the art has two raised fists; otherwise one thread tied to the highest point of the art.
- **Smoothness:** pose swaps crossfade (220 ms), the attach point and mood lean ease, sway runs at 25 fps.
- **Master switch** on Buddy and a **Quick Settings tile** (hide everything instantly); **battery test** slider with Charging / Hot / Power saver that drives the real status bar for up to 10 minutes.
- Re-run the battery report in `docs/PERFORMANCE.md`: the 25 fps sway costs more than the old 8 fps.

#### 4.2 Make your own character
- **What:** pick a photo (your pet, your OC, fan art) → cut out on the phone → white sticker border → name it → it lives in your status bar. Uses generic lines and the idle look for every pose.
- **How:** ML Kit Subject Segmentation (`cast/CustomCast.kt`, model downloads with the app), border by stamping the silhouette in a ring, saved to `files/cast/<id>/idle.png`. Screen: Buddy → Your character → Make.

#### 4.3 Character switcher
- **What:** "Your character" row on Buddy: the 5 built-ins, yours, and Make. Per-character bond levels come with Phase 3.

### Phase 5: Friends

#### 5.1 Friend battery widget
- **What:** pair with up to 5 friends by invite code / QR; widget shows their character and battery ("bestie at 3% 💀"). Poke a friend → their character shows your bubble.
- **How:** Firebase (Auth anonymous + Firestore) or a tiny Cloudflare Worker + KV; FCM for pokes; battery published at most every 15 min or on state change.

#### 5.2 Matching bars
- **What:** couple/bestie sets: two characters that complete each other across two phones.

---

### Phase 6: Seasonal & polish (ongoing)

- **Seasonal drops calendar:** Halloween (Oct), Winter/Christmas (Dec), New Year, Valentine's (Feb), anime season starts (Jan/Apr/Jul/Oct), birthday of your oshi.
- **Remote content:** line packs and drops loaded from a JSON file (GitHub raw / gist) so new slang and drops don't need a build.
- **Lock screen / AOD:** character on the lock screen clock area (Android 16+ lock screen widgets where available).
- **Sound packs, "now playing" reaction** (character vibes to music via `MediaSessionManager`).
- **Accessibility:** TalkBack labels on all kit components, 48dp targets, reduced-motion path, contrast checks.

---

## 4. Engineering rules (all phases)

1. **Battery budget stays under 0.5%/h** (see `docs/PERFORMANCE.md`). The overlay never uses Compose; new effects are allocation-free Canvas painters and only tick while visibly moving.
2. **Previews use the real view.** `BarPreview` wraps `FullBarView`, so in-app previews never drift from the overlay.
3. **Settings stay in `AppSettings`** (SharedPreferences, same process as the service) until Phase 3 adds DataStore for bond/inventory.
4. **adb hooks for every visual state**, so we can record and test without draining the battery:
   `adb shell am start -n com.zainkhalid.animebattery/.MainActivity --es cmd force --ei level 4 --ez plugged false`
5. **Design system first.** New UI is built from `ui/kit` components and `Pop` tokens only.

## 5. Sources

- [Trend Hunter: animated wallpaper apps](https://www.trendhunter.com/trends/lockcanvaswidget)
- [People's Pharmacy: personalisation download data](https://library.peoplespharmacy.com/cute-backgrounds-phone)
- [The Widget Effect](https://neoads.substack.com/p/the-widget-effect?r=ifkgx)
- [Dealroom: Locket](https://app.dealroom.co/companies/locket)
- [Lazyweb: are streaks still spreading?](https://www.lazyweb.com/research/are-streaks-still-spreading-beyond-duolingo.md)
- [StriveCloud: app gamification strategy](https://strivecloud.io/blog/app-gamification-strategy)
- [Paste: Finch](https://www.pastemagazine.com/article/finch-app-mental-health-virtual-pet-self-care)
- [Pratt IxD: Finch design critique](https://ixd.prattsi.org/2025/09/design-critique-finch-ios-app-2/)
- [Deconstructor of Fun: Finch widgets & retention](https://www.deconstructoroffun.com/blog/x0hd2ssr80y5n7gv0w967pg7hwd7tl)
- [Screensdesign: Widgetable customising flow](https://screensdesign.com/explore/flows/customizing-widgets/)
- [Kimola: Widgetable review report](https://kimola.com/reports/widgetable-virtual-pet-care-lock-screen-140828)
- [Duolingo design system notes](https://www.webdesignhot.com/design.md/duolingo/)
- [Pixel Pals (App Store)](https://apps.apple.com/us/app/pixel-pets-dynamic-island/id6444085825)
- [Kidscreen: anime popularity with teens](https://kidscreen.com/?p=222715)
- [AWN: Crunchyroll Gen Z study](https://awn.com/news/new-crunchyroll-commissioned-study-reveals-animes-impact-gen-z)
- [JACCC: oshi fandom](https://jaccc.org/learn/a-unique-form-of-fandom-has-taken-hold-in-japan/)
