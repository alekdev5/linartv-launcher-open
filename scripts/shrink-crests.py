"""Fit bundled crests to 512 px (longest side), keeping transparency.

The app never shows a crest larger than 512 px: CrestRepository decodes bigger images
with power-of-two subsampling down to <= 512 px and draws them at about 150 px.
Pre-scaling with Lanczos keeps the on-screen result identical or sharper and makes
the APK much smaller. Files already within 512 px are left untouched.

Usage (Pillow required):  python scripts/shrink-crests.py
"""
import os
from PIL import Image

LIMIT = 512
FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "crests", "curated")

before = after = changed = 0
for name in sorted(os.listdir(FOLDER)):
    if not name.lower().endswith(".png"):
        continue
    path = os.path.join(FOLDER, name)
    size = os.path.getsize(path)
    before += size
    with Image.open(path) as im:
        im.load()
        if max(im.size) <= LIMIT:
            after += size
            continue
        mode = "RGBA" if im.mode in ("RGBA", "LA", "P") or "transparency" in im.info else "RGB"
        im = im.convert(mode)
        scale = LIMIT / max(im.size)
        target = (max(1, round(im.width * scale)), max(1, round(im.height * scale)))
        im = im.resize(target, Image.LANCZOS)
        with open(path, "rb") as f:
            original = f.read()
        im.save(path, "PNG", optimize=True)
    if os.path.getsize(path) >= size:
        # Already well compressed (e.g. palette PNG): keep the original bytes.
        with open(path, "wb") as f:
            f.write(original)
        after += size
        print("%-55s kept original (resized file was not smaller)" % name)
        continue
    changed += 1
    after += os.path.getsize(path)
    print("%-55s %6.0f kB -> %5.0f kB  %s" % (name, size / 1024, os.path.getsize(path) / 1024, target))
print("resized %d files: %.1f MB -> %.1f MB" % (changed, before / 2**20, after / 2**20))
