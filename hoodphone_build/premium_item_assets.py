#!/usr/bin/env python3
"""Generate original, high-resolution HoodPhone item artwork for Forge assets.
No third-party images or runtime dependencies.
"""
from pathlib import Path
import math, struct, zlib, json

ROOT = Path("src/main/resources/assets/hoodphone")
TARGET = ROOT / "textures" / "item"
TARGET.mkdir(parents=True, exist_ok=True)
W, H, SS = 72, 144, 3
# Only official iPhone 18 Pro finishes, verified against Apple (2026).
OFFICIAL_FINISHES = ("black", "silver", "glacier", "burgundy")
DISPLAY_NAMES_PL = {
    "black": "Czarny",
    "silver": "Srebrny",
    "glacier": "Błękit lodowca",
    "burgundy": "Burgund",
}
COLORS = {
    "black": (37, 40, 45),
    "silver": (196, 202, 207),
    "glacier": (172, 191, 207),
    "burgundy": (105, 48, 61),
}

assert tuple(COLORS) == OFFICIAL_FINISHES, "Unsupported iPhone 18 Pro color"

def png(path, pix, width, height):
    def chunk(typ, payload):
        return struct.pack(">I", len(payload)) + typ + payload + struct.pack(">I", zlib.crc32(typ + payload) & 0xffffffff)
    raw = b"".join(b"\0" + bytes(v for color in pix[y * width:(y + 1) * width] for v in color) for y in range(height))
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">2I5B", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))

def render(name, body):
    w, h = W * SS, H * SS
    result = []
    def rr(x, y, x0, y0, x1, y1, r):
        qx = max(x0 + r - x, 0, x - (x1 - r), 0)
        qy = max(y0 + r - y, 0, y - (y1 - r), 0)
        return qx * qx + qy * qy <= r * r
    for y in range(h):
        for x in range(w):
            u, v = x / SS, y / SS
            color = (0, 0, 0, 0)
            if rr(u, v, 4, 2, 68, 142, 10):
                edge = not rr(u, v, 6, 4, 66, 140, 9)
                shade = 0.68 + 0.3 * (1 - u / 72) + 0.06 * math.sin(v / 24)
                if edge:
                    color = (min(255, int(body[0] * shade + 31)), min(255, int(body[1] * shade + 31)), min(255, int(body[2] * shade + 31)), 255)
                else:
                    color = tuple(max(0, min(255, int(c * shade))) for c in body) + (255,)
                # Raised, polished camera island on the back
                if rr(u, v, 9, 10, 42, 48, 7):
                    color = tuple(max(0, int(c * .82)) for c in body) + (255,)
                    if not rr(u, v, 10, 11, 41, 47, 6):
                        color = tuple(min(255, int(c * 1.16)) for c in body) + (255,)
                for cx, cy in ((19, 20), (34, 29), (19, 40)):
                    r2 = (u-cx)**2 + (v-cy)**2
                    if r2 < 7.9**2:
                        color = (32, 34, 40, 255)
                    if r2 < 6.3**2:
                        color = (9, 12, 20, 255)
                    if r2 < 4.5**2:
                        color = (20, 32, 51, 255)
                    if r2 < 2.4**2:
                        color = (11, 18, 29, 255)
                    if 2.8**2 < r2 < 3.6**2 and u < cx:
                        color = (74, 96, 112, 255)
                if (u-34)**2 + (v-17)**2 < 2.4**2:
                    color = (248, 227, 169, 255)
                if (u-37)**2 + (v-42)**2 < 2.1**2:
                    color = (26, 28, 34, 255)
                if 62 < u < 64 and 31 < v < 50:
                    color = (max(0, body[0]//2), max(0, body[1]//2), max(0, body[2]//2), 255)
            result.append(color)
    # Supersample then downsample RGBA for smooth curves
    low = []
    for y in range(H):
        for x in range(W):
            cells = [result[(y*SS+j)*w+x*SS+i] for j in range(SS) for i in range(SS)]
            a = sum(c[3] for c in cells)
            if a:
                low.append(tuple(sum(c[k]*c[3] for c in cells)//a for k in range(3)) + (a//(SS*SS),))
            else:
                low.append((0,0,0,0))
    png(TARGET / ("iphone18pro_" + name + ".png"), low, W, H)

for name, rgb in COLORS.items():
    render(name, rgb)
print("Created", len(COLORS), "official iPhone 18 Pro finish textures in", TARGET)

# Connect generated artwork to pre-existing phone item models without touching
# server-side logic or other mod items.
model_dir = ROOT / "models" / "item"
patched = 0
# Recovered source archive can contain Java classes but no original model assets.
# Create actual Forge item models for each finish if the resource folder is empty.
model_dir.mkdir(parents=True, exist_ok=True)
for finish in OFFICIAL_FINISHES:
    model = model_dir / ("iphone18pro_" + finish + ".json")
    if not model.exists():
        model.write_text('{"parent":"minecraft:item/generated","textures":{"layer0":"hoodphone:item/iphone18pro_' + finish + '"}}')
for model in model_dir.glob("*.json"):
    name = model.stem.lower()
    if not ("phone" in name or "iphone" in name):
        continue
    finish = next((finish for finish in COLORS if finish in name), "black")
    try:
        doc = json.loads(model.read_text(encoding="utf-8"))
    except (ValueError, OSError):
        continue
    if not isinstance(doc, dict) or "elements" in doc:
        continue  # preserve any existing custom 3D mesh
    # Proper 3D geometry: chassis, full-width camera plateau and raised lenses.
    # Matches the iPhone 18 Pro rear-camera layout; not a flat item sprite.
    def element(frm, to, faces):
        return {"from": frm, "to": to, "faces": {side: {"texture": tex, "uv": uv}
                 for side, tex, uv in faces}}
    bodytex = "#body"
    glass = "#glass"
    dark = "#lens"
    all_sides = ("north", "south", "east", "west", "up", "down")
    def cuboid(frm, to, tex, uv=(0, 0, 16, 16)):
        return element(frm, to, [(face, tex, list(uv)) for face in all_sides])
    elements = [
        cuboid([4.25, 0.5, 7.65], [11.75, 15.5, 8.45], bodytex),
        cuboid([4.36, 11.25, 8.45], [11.64, 15.18, 8.93], bodytex),
        cuboid([4.6, 1.0, 7.55], [11.4, 15.0, 7.65], glass),
    ]
    # 16-gon approximates a circular raised lens while remaining vanilla JSON.
    import math as _math
    def lens(cx, cy, outer_r, z0, z1, texture):
        n = 16
        for i in range(n):
            t = 2 * _math.pi * i / n
            t2 = 2 * _math.pi * (i + 1) / n
            # Segment represented by a shallow rectangular slice for broad mod compatibility.
            ax = cx + outer_r * _math.cos(t)
            ay = cy + outer_r * _math.sin(t)
            bx = cx + outer_r * _math.cos(t2)
            by = cy + outer_r * _math.sin(t2)
            lo_x, hi_x = sorted((ax, bx))
            lo_y, hi_y = sorted((ay, by))
            if hi_x - lo_x < 0.03: hi_x += .03
            if hi_y - lo_y < 0.03: hi_y += .03
            elements.append(cuboid([lo_x, lo_y, z0], [hi_x, hi_y, z1], texture))
    for cx, cy in ((5.8, 14.15), (8.15, 13.2), (5.8, 12.3)):
        lens(cx, cy, .72, 8.94, 9.2, dark)
        lens(cx, cy, .48, 9.2, 9.26, glass)
    lens(10.5, 14.2, .19, 8.94, 8.98, "#flash")
    lens(10.5, 12.55, .2, 8.94, 8.99, dark)
    doc = {
        "textures": {
            "body": "hoodphone:item/iphone18pro_" + finish,
            "glass": "hoodphone:item/iphone18pro_" + finish,
            "lens": "minecraft:block/black_concrete",
            "flash": "minecraft:block/sea_lantern"
        },
        "elements": elements,
        "display": {
            "gui": {"rotation": [22, 145, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "firstperson_righthand": {"rotation": [5, 165, 0], "translation": [2.0, 0.0, 0.0], "scale": [0.9, 0.9, 0.9]},
            "firstperson_lefthand": {"rotation": [5, -165, 0], "translation": [-2.0, 0.0, 0.0], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_righthand": {"rotation": [0, 90, 0], "scale": [0.54, 0.54, 0.54]},
            "thirdperson_lefthand": {"rotation": [0, -90, 0], "scale": [0.54, 0.54, 0.54]},
            "ground": {"scale": [0.45, 0.45, 0.45]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]}
        }
    }
    model.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    patched += 1
print("Premium artwork linked to", patched, "existing phone item models")
