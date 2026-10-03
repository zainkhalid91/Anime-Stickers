"""
Generate images with the local ComfyUI server (Comfy Desktop, http://127.0.0.1:8188).

Sends a plain SDXL text-to-image workflow, waits, and saves the PNGs into art/raw/.
Nothing is installed; only uses requests.

    python tools/comfy_gen.py "uzumaki naruto, chibi, ..." --seeds 1 2 3 --name naruto_test
"""
import argparse
import json
import pathlib
import time
import uuid

import requests

SERVER = "http://127.0.0.1:8188"
CKPT = "animagine-xl-4.0-opt.safetensors"
OUT = pathlib.Path(__file__).resolve().parent.parent / "art" / "raw"

# Animagine XL 4.0 likes its quality tags at the end of the prompt.
QUALITY = "masterpiece, high score, great score, absurdres"
NEGATIVE = (
    "lowres, bad anatomy, bad hands, text, error, missing finger, extra digits, fewer digits, "
    "cropped, worst quality, low quality, low score, bad score, average score, signature, "
    "watermark, username, blurry, multiple views, speech bubble"
)


def workflow(prompt: str, negative: str, seed: int, w: int, h: int, steps: int, cfg: float, prefix: str) -> dict:
    return {
        "1": {"class_type": "CheckpointLoaderSimple", "inputs": {"ckpt_name": CKPT}},
        "2": {"class_type": "CLIPTextEncode", "inputs": {"text": f"{prompt}, {QUALITY}", "clip": ["1", 1]}},
        "3": {"class_type": "CLIPTextEncode", "inputs": {"text": negative, "clip": ["1", 1]}},
        "4": {"class_type": "EmptyLatentImage", "inputs": {"width": w, "height": h, "batch_size": 1}},
        "5": {
            "class_type": "KSampler",
            "inputs": {
                "model": ["1", 0], "positive": ["2", 0], "negative": ["3", 0], "latent_image": ["4", 0],
                "seed": seed, "steps": steps, "cfg": cfg,
                "sampler_name": "euler_ancestral", "scheduler": "normal", "denoise": 1.0,
            },
        },
        "6": {"class_type": "VAEDecode", "inputs": {"samples": ["5", 0], "vae": ["1", 2]}},
        "7": {"class_type": "SaveImage", "inputs": {"images": ["6", 0], "filename_prefix": prefix}},
    }


def run(prompt: str, seed: int, args) -> list[pathlib.Path]:
    client = str(uuid.uuid4())
    wf = workflow(prompt, args.negative, seed, args.width, args.height, args.steps, args.cfg, f"animebattery/{args.name}")
    r = requests.post(f"{SERVER}/prompt", json={"prompt": wf, "client_id": client}, timeout=30)
    r.raise_for_status()
    pid = r.json()["prompt_id"]
    start = time.time()
    while True:
        hist = requests.get(f"{SERVER}/history/{pid}", timeout=30).json()
        if pid in hist:
            entry = hist[pid]
            status = entry.get("status", {})
            if status.get("status_str") == "error":
                raise RuntimeError(json.dumps(status, indent=1)[:2000])
            break
        time.sleep(2)
    took = time.time() - start
    saved = []
    for node in entry["outputs"].values():
        for img in node.get("images", []):
            data = requests.get(f"{SERVER}/view", params=img, timeout=60).content
            OUT.mkdir(parents=True, exist_ok=True)
            p = OUT / f"{args.name}_s{seed}.png"
            p.write_bytes(data)
            saved.append(p)
    print(f"seed {seed}: {took:.0f}s -> {', '.join(str(s) for s in saved)}", flush=True)
    return saved


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("prompt")
    ap.add_argument("--name", default="test")
    ap.add_argument("--seeds", type=int, nargs="+", default=[1])
    ap.add_argument("--width", type=int, default=1024)
    ap.add_argument("--height", type=int, default=1024)
    ap.add_argument("--steps", type=int, default=26)
    ap.add_argument("--cfg", type=float, default=5.0)
    ap.add_argument("--negative", default=NEGATIVE)
    args = ap.parse_args()
    for s in args.seeds:
        run(args.prompt, s, args)


if __name__ == "__main__":
    main()
