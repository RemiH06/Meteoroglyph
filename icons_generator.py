# generate_icons.py
from PIL import Image, ImageDraw
import json, os

GLYPHS_PATH  = "app/src/main/assets/glyphs/weather.json"
OUT_DARK     = "app/src/main/res/mipmap-xxxhdpi"
OUT_LIGHT    = "app/src/main/res/mipmap-night-xxxhdpi"  # noche = modo claro en Android
SIZE         = 192

# Paleta modo oscuro — colores originales metro_theme
PALETTE_DARK = [
    (0,229,160,255),   # 0 verde
    (245,166,35,255),  # 1 ámbar
    (255,69,96,255),   # 2 rojo
    (69,123,255,255),  # 3 azul
    (155,109,255,255), # 4 púrpura
    (255,122,48,255),  # 5 naranja
    (240,240,240,255), # 6 blanco
    (136,136,136,255), # 7 gris cl
    (68,68,68,255),    # 8 gris
]

# Paleta modo claro — colores oscuros de metro_theme light
PALETTE_LIGHT = [
    (107,26,42,255),   # 0 accent claro
    (196,105,26,255),  # 1 warn claro
    (139,26,26,255),   # 2 danger claro
    (26,58,107,255),   # 3 blue claro
    (74,26,107,255),   # 4 purple claro
    (168,69,16,255),   # 5 orange claro
    (17,17,17,255),    # 6 text primary claro
    (136,136,136,255), # 7 gris cl
    (68,68,68,255),    # 8 gris
]

ICON_MAP = {
    "ic_weather_cloudy_day":   "partly_cloudy",
    "ic_weather_sunny":        "sunny",
    "ic_weather_clear_night":  "clear_night",
    "ic_weather_cloudy_night": "partly_cloudy_night",
    "ic_weather_overcast":     "overcast",
    "ic_weather_rain":         "rain",
    "ic_weather_storm":        "storm",
    "ic_weather_wind":         "wind",
    "ic_weather_fog":          "fog",
    "ic_weather_cold":         "cold",
    "ic_weather_hot":          "heat",
}

def render_glyph(data, cols, rows, size, palette):
    # Fondo transparente
    img  = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    pad  = size * 0.08
    cell = (size - 2 * pad) / max(cols, rows)
    dot  = (cell * 0.78) / 2

    for r in range(rows):
        for c in range(cols):
            val = data[r][c]
            if val is None:
                continue
            if isinstance(val, int):
                color = palette[val]
            elif isinstance(val, dict):
                h = val["custom"].lstrip("#")
                color = tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)
            else:
                continue
            cx = pad + c * cell + cell / 2
            cy = pad + r * cell + cell / 2
            draw.ellipse([cx-dot, cy-dot, cx+dot, cy+dot], fill=color)
    return img

with open(GLYPHS_PATH) as f:
    lib = json.load(f)

os.makedirs(OUT_DARK,  exist_ok=True)
os.makedirs(OUT_LIGHT, exist_ok=True)

for icon_name, glyph_name in ICON_MAP.items():
    glyph = None
    for key, val in lib["glyphs"].items():
        if val["name"] == glyph_name:
            glyph = val
            break
    if not glyph:
        print(f"⚠ No encontrado: {glyph_name}")
        continue

    # Modo oscuro
    img_dark = render_glyph(glyph["data"], glyph["cols"], glyph["rows"], SIZE, PALETTE_DARK)
    img_dark.save(os.path.join(OUT_DARK, f"{icon_name}.png"))

    # Modo claro
    img_light = render_glyph(glyph["data"], glyph["cols"], glyph["rows"], SIZE, PALETTE_LIGHT)
    img_light.save(os.path.join(OUT_LIGHT, f"{icon_name}.png"))

    print(f"✓ {icon_name} (dark + light)")

print("Done.")

# Agregar al final del script, después del loop principal

OUT_NOTIF = "app/src/main/res/drawable"
os.makedirs(OUT_NOTIF, exist_ok=True)

NOTIF_SIZE = 96  # mdpi equivalente para small icon

for icon_name, glyph_name in ICON_MAP.items():
    glyph = None
    for key, val in lib["glyphs"].items():
        if val["name"] == glyph_name:
            glyph = val
            break
    if not glyph:
        continue

    # Monocromático — solo alpha, sin color
    img  = Image.new("RGBA", (NOTIF_SIZE, NOTIF_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    pad  = NOTIF_SIZE * 0.08
    rows = glyph["rows"]
    cols = glyph["cols"]
    cell = (NOTIF_SIZE - 2 * pad) / max(cols, rows)
    dot  = (cell * 0.78) / 2

    for r in range(rows):
        for c in range(cols):
            val = glyph["data"][r][c]
            if val is None:
                continue
            cx = pad + c * cell + cell / 2
            cy = pad + r * cell + cell / 2
            # Blanco puro — Android lo tintea con el color de la notificación
            draw.ellipse([cx-dot, cy-dot, cx+dot, cy+dot], fill=(255, 255, 255, 255))

    notif_name = icon_name.replace("ic_weather_", "ic_notif_")
    img.save(os.path.join(OUT_NOTIF, f"{notif_name}.png"))
    print(f"✓ {notif_name}.png (notif)")

print("Notif icons done.")