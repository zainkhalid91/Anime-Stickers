"""
Render the character roster in every look with the local ComfyUI, in the same style
as the Naruto sticker (Animagine XL 4.0, chibi, white background, thick outlines).

Writes art/raw/cast/<id>/<look>_s<seed>.png, skipping files that already exist, then
use tools/contact_sheet.py to pick the best seed and tools/cutout.py to make stickers.

    python tools/gen_cast.py                      # everything
    python tools/gen_cast.py --only luffy gojo    # some characters
    python tools/gen_cast.py --looks hang dizzy --seeds 1 2 3
"""
import argparse
import pathlib
import sys
import time
import uuid

import requests

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from comfy_gen import NEGATIVE, SERVER, workflow  # noqa: E402

OUT = pathlib.Path(__file__).resolve().parent.parent / "art" / "raw" / "cast"

# One sticker = one character, never a pair or a turnaround.
EXTRA_NEGATIVE = ", 2girls, 2boys, multiple girls, multiple boys, duplicate, clone, reference sheet, noose"

STYLE = "chibi, full body, big head, cute, simple background, white background, flat color, thick outlines"

# Danbooru-style tags; Animagine knows these characters by name, which keeps every look on-model.
CAST = {
    "naruto": "uzumaki naruto, naruto (series), 1boy, solo, blonde hair, spiky hair, blue eyes, whisker markings, forehead protector, orange jumpsuit, black shoulders, sandals",
    "luffy": "monkey d. luffy, one piece, 1boy, solo, black hair, straw hat, scar under eye, red vest, open vest, blue shorts, sandals",
    "gojo": "gojou satoru, jujutsu kaisen, 1boy, solo, white hair, blindfold, black jacket, high collar, black pants",
    "anya": "anya (spy x family), spy x family, 1girl, solo, pink hair, green eyes, cone hair bun, hair ornament, eden academy school uniform, black dress",
    # Original character: Claude, drawn the way it would like to look. Warm terracotta
    # hair, a little orange spark clip, round glasses, a cosy oversized sweater and a
    # notebook, because it's always thinking something through.
    "claude": "original, 1other, androgynous, solo, single character, short hair, fluffy hair, messy hair, orange hair, terracotta hair, amber eyes, (round eyewear:1.3), glasses, star hair ornament, orange hair clip, oversized sweater, cream sweater, sleeves past wrists, brown shorts, white socks, brown shoes, holding notebook, pencil",
}

# What each look adds. "hang" is the default status bar pose: holding a rope above the head.
LOOKS = {
    "idle": "standing, smile, open mouth, waving",
    # Not the "hanging" tag: it can come out dark. A rope swing reads as playful.
    "hang": "arms up, reaching up, holding onto rope, rope swing, both hands up, legs dangling, feet off ground, smile, looking at viewer, playful",
    "grabbed": "surprised, open mouth, wide-eyed, sweatdrop, flailing, arms up, panicking",
    "dizzy": "dizzy, @_@, spiral eyes, swirly eyes, tongue out, swaying",
    "sleep": "sleeping, closed eyes, sitting, hugging knees, drooling, peaceful",
    "cheer": "jumping, arms up, cheering, closed eyes, open mouth, happy, ^_^",
    "hurt": "crying, tears, >_<, hands on own head, comical pain, wavy mouth, standing",
}


def cool_down(limit: int = 78) -> None:
    """Laptop GPUs throttle to a crawl when hot; wait until it's below [limit] °C."""
    import subprocess
    while True:
        try:
            t = int(subprocess.run(["nvidia-smi", "--query-gpu=temperature.gpu", "--format=csv,noheader"],
                                   capture_output=True, text=True, timeout=10).stdout.strip().splitlines()[0])
        except Exception:
            return
        if t < limit:
            return
        time.sleep(10)


def render(prompt: str, seed: int, out: pathlib.Path, size: int, steps: int) -> float:
    wf = workflow(f"{prompt}", NEGATIVE + EXTRA_NEGATIVE, seed, size, size, steps, 5.0, f"animebattery/cast_{out.parent.name}_{out.stem}")
    r = requests.post(f"{SERVER}/prompt", json={"prompt": wf, "client_id": str(uuid.uuid4())}, timeout=30)
    r.raise_for_status()
    pid = r.json()["prompt_id"]
    start = time.time()
    while True:
        hist = requests.get(f"{SERVER}/history/{pid}", timeout=30).json()
        if pid in hist:
            entry = hist[pid]
            if entry.get("status", {}).get("status_str") == "error":
                raise RuntimeError(str(entry["status"])[:1500])
            break
        time.sleep(1.5)
    for node in entry["outputs"].values():
        for img in node.get("images", []):
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(requests.get(f"{SERVER}/view", params=img, timeout=60).content)
    return time.time() - start


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="*", default=list(CAST))
    ap.add_argument("--looks", nargs="*", default=list(LOOKS))
    ap.add_argument("--seeds", type=int, nargs="+", default=[1])
    # Stickers show at ~110 px in the bar and ~460 px in the app; 832 is plenty and
    # roughly halves the time on a laptop GPU.
    ap.add_argument("--size", type=int, default=832)
    ap.add_argument("--steps", type=int, default=22)
    args = ap.parse_args()
    # Look by look, so every character gets its most important poses first.
    for look in args.looks:
        for cid in args.only:
            for seed in args.seeds:
                out = OUT / cid / f"{look}_s{seed}.png"
                if out.exists():
                    continue
                prompt = f"{CAST[cid]}, {STYLE}, {LOOKS[look]}"
                cool_down()
                took = render(prompt, seed, out, args.size, args.steps)
                print(f"{cid}/{look} s{seed}: {took:.0f}s", flush=True)


if __name__ == "__main__":
    main()
