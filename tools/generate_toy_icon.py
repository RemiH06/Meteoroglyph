"""
Genera el icono de preview del Glyph Toy de Meteoroglyph con el mismo estilo
que los de 50.RemsGlyphToys (celdas del 489-LED real de la Glyph Matrix del
Nothing Phone 3, encendidas en blanco sobre celdas apagadas tenues), en vez
del mipmap generico que se usaba como placeholder.

Toma el glifo "partly_cloudy" ya diseñado en weather25x25.json (cualquier
celda no nula cuenta como encendida, sin importar el color de paleta).

Uso, desde la raiz del repo:  python tools/generate_toy_icon.py
"""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
N = 25
ROW_SPANS = [7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25, 25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7]
MASK = [[(N - w) // 2 <= c < (N - w) // 2 + w for c in range(N)] for w in ROW_SPANS]

ON_WHITE = "#F0F0F0"
OFF_DIM = "#262626"
GLYPH_NAME = "partly_cloudy"


def cells_path(cells, pitch, cell, origin):
    parts = []
    for r, c in cells:
        x = origin + c * pitch + (pitch - cell) / 2
        y = origin + r * pitch + (pitch - cell) / 2
        parts.append(f"M{x:.2f},{y:.2f}h{cell:.2f}v{cell:.2f}h{-cell:.2f}z")
    return "".join(parts)


def vector_drawable(grid, pitch=4.2, origin=1.5):
    on_cells = [(r, c) for r in range(N) for c in range(N) if MASK[r][c] and grid[r][c]]
    off_cells = [(r, c) for r in range(N) for c in range(N) if MASK[r][c] and not grid[r][c]]
    cell = pitch * 0.82
    paths = [
        f'    <path android:fillColor="{OFF_DIM}" android:pathData="{cells_path(off_cells, pitch, cell, origin)}"/>',
        f'    <path android:fillColor="{ON_WHITE}" android:pathData="{cells_path(on_cells, pitch, cell, origin)}"/>',
    ]
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Icono de preview del Glyph Toy de clima. Generado por tools/generate_toy_icon.py, no editar a mano. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n'
        + "\n".join(paths) + "\n</vector>\n"
    )


def main():
    data = json.loads((ROOT / "app/src/main/assets/glyphs/weather25x25.json").read_text(encoding="utf-8"))
    glyph = data["glyphs"][f"weather::{GLYPH_NAME}"]
    grid = [[glyph["data"][r][c] is not None for c in range(N)] for r in range(N)]

    out = ROOT / "app/src/main/res/drawable/ic_toy_weather.xml"
    out.write_text(vector_drawable(grid), encoding="utf-8")
    print(f"ok: {out}")


if __name__ == "__main__":
    main()
