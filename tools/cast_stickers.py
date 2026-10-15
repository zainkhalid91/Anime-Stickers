"""
Turn the roster renders (art/raw/cast/<id>/<look>_s<seed>.png) into app stickers
(app/src/main/res/drawable-nodpi/sticker_<id>_<look>.png) with tools/cutout.py.

Picks seed 1 unless art/raw/cast/picks.txt says otherwise, one line per choice
(seed 0 = rejected, no sticker; the app falls back to idle):
    gojo/hang 2

    python tools/cast_stickers.py            # everything rendered so far
    python tools/cast_stickers.py --sheet    # also write art/raw/cast/sheet.png to review
"""
import argparse
import pathlib
import subprocess
import sys

from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
RAW = ROOT / "art" / "raw" / "cast"
RES = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
# Per-render cutout fixes (see tools/cutout.py).
EXTRA = {
    "naruto/cheer": ["--peel-white"],  # drawn on a white card over a dark background
    "claude/cheer": ["--peel-white", "--strip-lines"],  # drawn on a white circle over mint
}

LOOKS = ["idle", "hang", "grabbed", "dizzy", "sleep", "cheer", "hurt", "meh", "tired", "hot", "power"]


def picks() -> dict:
    f = RAW / "picks.txt"
    out = {}
    if f.exists():
        for line in f.read_text().splitlines():
            if line.strip() and not line.startswith("#"):
                key, seed = line.split()
                out[key] = int(seed)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet", action="store_true")
    args = ap.parse_args()
    chosen = picks()
    made = []
    for cdir in sorted(p for p in RAW.iterdir() if p.is_dir()):
        for look in LOOKS:
            seed = chosen.get(f"{cdir.name}/{look}", 1)
            if seed == 0:
                # Rejected and nothing better yet: fall back to idle in the app.
                (RES / f"sticker_{cdir.name}_{look}.png").unlink(missing_ok=True)
                continue
            src = cdir / f"{look}_s{seed}.png"
            if not src.exists():
                continue
            dst = RES / f"sticker_{cdir.name}_{look}.png"
            if dst.exists() and dst.stat().st_mtime > src.stat().st_mtime:
                made.append(dst)
                continue
            cmd = [sys.executable, str(ROOT / "tools" / "cutout.py"), str(src), "--out", str(dst), "--height", "640"]
            # The app draws its own thread; drop any rope the render ran off the top.
            if look == "hang":
                cmd.append("--strip-lines")
            cmd += EXTRA.get(f"{cdir.name}/{look}", [])
            subprocess.run(cmd, check=True)
            made.append(dst)
    if args.sheet and made:
        # One row per character, one column per look, on a dark background like the bar.
        ids = sorted({p.stem.split("_")[1] for p in made})
        cell = 200
        sheet = Image.new("RGB", (cell * len(LOOKS) + 90, cell * len(ids) + 30), (22, 17, 31))
        d = ImageDraw.Draw(sheet)
        for x, look in enumerate(LOOKS):
            d.text((90 + x * cell + 6, 8), look, fill=(255, 255, 255))
        for y, cid in enumerate(ids):
            d.text((6, 30 + y * cell + cell // 2), cid, fill=(255, 255, 255))
            for x, look in enumerate(LOOKS):
                p = RES / f"sticker_{cid}_{look}.png"
                if not p.exists():
                    continue
                im = Image.open(p).convert("RGBA")
                im.thumbnail((cell - 12, cell - 12))
                sheet.paste(im, (90 + x * cell + (cell - im.width) // 2, 30 + y * cell + (cell - im.height) // 2), im)
        out = RAW / "sheet.png"
        sheet.save(out)
        print(out)


if __name__ == "__main__":
    main()
