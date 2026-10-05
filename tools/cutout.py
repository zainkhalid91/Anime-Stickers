"""
Turn a generated image on a white background into a transparent sticker.

1. Background = near-white pixels connected to the image border (the character's
   line art stops the fill, so eye whites and teeth stay).
2. Soft 1px edge, crop to the character.
3. Add our own white sticker border (same thickness for every character).

    python tools/cutout.py art/raw/naruto_s1.png --out art/stickers/naruto_happy.png --height 640
"""
import argparse
import pathlib

import cv2
import numpy as np
from scipy import ndimage


def cut(rgb: np.ndarray, tolerance: int = 26) -> np.ndarray:
    r, g, b = (rgb[..., i].astype(int) for i in range(3))
    mn = np.minimum(np.minimum(r, g), b)
    # The background is "white" but renders vary (pure white, cream, beige), so take
    # its colour from the image border and match anything close to it.
    rim = np.concatenate([rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]]).astype(int)
    bg_col = np.median(rim, axis=0)
    dist = np.abs(rgb.astype(int) - bg_col).max(axis=2)
    near_white = dist <= tolerance
    labels, _ = ndimage.label(near_white)
    border = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    bg = np.isin(labels, border[border > 0])
    fg = ~bg
    # Drop specks, keep the biggest blobs.
    lab, n = ndimage.label(fg)
    if n > 1:
        sizes = ndimage.sum(fg, lab, range(1, n + 1))
        keep = np.isin(lab, 1 + np.where(sizes >= sizes.max() * 0.02)[0])
        fg = keep
    alpha = fg.astype(np.float32)
    # Soft edge: fade pixels on the boundary by how white they are.
    edge = fg & ~ndimage.binary_erosion(fg)
    whiteness = np.clip((mn - 200) / 55.0, 0, 1)
    alpha[edge] = 1.0 - 0.7 * whiteness[edge]
    return (alpha * 255).astype(np.uint8)


def peel_white(rgb: np.ndarray, alpha: np.ndarray, tolerance: int = 22) -> np.ndarray:
    """Second pass for renders that put the character on a white card or circle over a
    coloured background: also drop near-white areas touching what's already removed.
    Opt-in, because it would eat white hair or clothes on the edge of a character."""
    near_white = (np.abs(rgb.astype(int) - 255).max(axis=2) <= tolerance)
    labels, _ = ndimage.label(near_white)
    removed = alpha == 0
    # Reach across the card's own thin outline to find it.
    reach = max(2, int(alpha.shape[0] * 0.008))
    touching = ndimage.binary_dilation(removed, iterations=reach) & near_white
    ids = np.unique(labels[touching])
    peel = np.isin(labels, ids[ids > 0])
    out = alpha.copy()
    out[peel] = 0
    # The card's outline is now a loose thin ring; drop whatever no longer touches the body.
    lab, n = ndimage.label(out > 0)
    if n > 1:
        sizes = ndimage.sum(out > 0, lab, range(1, n + 1))
        out[~np.isin(lab, 1 + np.where(sizes >= sizes.max() * 0.02)[0])] = 0
    return out


def strip_lines(alpha: np.ndarray, frac: float = 0.014) -> np.ndarray:
    """Drop long thin strokes (e.g. ropes running off the top) but keep everything near
    the body: open the mask to lose thin lines, grow it back a little, and keep only
    what the grown body covers."""
    k = max(3, int(alpha.shape[0] * frac)) | 1
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (k, k))
    body = cv2.morphologyEx((alpha > 0).astype(np.uint8), cv2.MORPH_OPEN, kernel)
    # Keep the biggest piece of body only.
    n, lab, stats, _ = cv2.connectedComponentsWithStats(body)
    if n > 1:
        big = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        body = (lab == big).astype(np.uint8)
    near = cv2.dilate(body, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (k * 2 + 1, k * 2 + 1)))
    return np.where(near > 0, alpha, 0).astype(np.uint8)


def sticker(rgba: np.ndarray, border: int) -> np.ndarray:
    a = rgba[..., 3]
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * border + 1, 2 * border + 1))
    grown = cv2.dilate(a, k)
    grown = cv2.GaussianBlur(grown, (3, 3), 0)
    out = np.zeros_like(rgba)
    out[..., :3] = 255
    out[..., 3] = grown
    # Character over the white border.
    fa = a[..., None].astype(np.float32) / 255.0
    out[..., :3] = (rgba[..., :3] * fa + out[..., :3] * (1 - fa)).astype(np.uint8)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("--out", required=True)
    ap.add_argument("--height", type=int, default=640, help="final height in px")
    ap.add_argument("--border", type=float, default=0.022, help="border as a fraction of height")
    ap.add_argument("--strip-lines", action="store_true", help="remove thin lines like ropes off the body")
    ap.add_argument("--peel-white", action="store_true", help="also remove a white card/circle behind the character")
    args = ap.parse_args()

    bgr = cv2.imread(args.src, cv2.IMREAD_COLOR)
    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    alpha = cut(rgb)
    if args.peel_white:
        alpha = peel_white(rgb, alpha)
    if args.strip_lines:
        alpha = strip_lines(alpha)
    ys, xs = np.where(alpha > 0)
    pad = int(rgb.shape[0] * 0.04)
    y0, y1 = max(0, ys.min() - pad), min(rgb.shape[0], ys.max() + pad)
    x0, x1 = max(0, xs.min() - pad), min(rgb.shape[1], xs.max() + pad)
    rgba = np.dstack([rgb, alpha])[y0:y1, x0:x1]

    scale = args.height / rgba.shape[0]
    size = (max(1, round(rgba.shape[1] * scale)), args.height)
    rgba = cv2.resize(rgba, size, interpolation=cv2.INTER_AREA)
    # Pad so the border has room, then add it.
    b = max(2, round(args.height * args.border))
    rgba = np.pad(rgba, ((b + 2, b + 2), (b + 2, b + 2), (0, 0)))
    out = sticker(rgba, b)

    p = pathlib.Path(args.out)
    p.parent.mkdir(parents=True, exist_ok=True)
    cv2.imwrite(str(p), cv2.cvtColor(out, cv2.COLOR_RGBA2BGRA))
    print(f"{p}  {out.shape[1]}x{out.shape[0]}")


if __name__ == "__main__":
    main()
