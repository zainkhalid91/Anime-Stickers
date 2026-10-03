"""Cut an art sheet into a 1x overview (shown 2x) and one 8x zoom per state."""
import sys
from PIL import Image

D = 1.9125  # Pixel 8 Pro at its current display size
path = sys.argv[1]
im = Image.open(path)
W, H = im.size
rows = 9
row_h = H // rows
label_w, crop_w = int(96 * D), int(84 * D)
sb_h = int(59 * D)
zx = label_w + crop_w * 2 + 16
base = path[:-4]

ones = Image.new("RGB", (zx, rows * (sb_h + 6)), (40, 40, 46))
for i in range(rows):
    ones.paste(im.crop((0, i * row_h, zx, i * row_h + sb_h)), (0, i * (sb_h + 6)))
ones.resize((zx * 2, ones.height * 2), Image.NEAREST).save(base + "_1x.png")
for i in range(rows):
    im.crop((zx, i * row_h, W, i * row_h + row_h - 8)).save(f"{base}_z{i}.png")
print("ok", W, H)
