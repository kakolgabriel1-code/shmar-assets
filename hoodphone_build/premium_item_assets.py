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
COLORS = {
    "black": (37, 40, 45),
    "silver": (196, 202, 207),
    "glacier": (172, 191, 207),
    "burgundy": (105, 48, 61),
}

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
print("Created", len(COLORS), "original HoodPhone premium phone textures in", TARGET)
