# meteoroglyph — CLAUDE.md

Contexto completo del proyecto para continuar desarrollo en Claude Code.

---

## Proyecto

App Android personal para **Nothing Phone 3 (DEVICE_23112, Glyph Matrix 25x25, Nothing OS / Android 16)**.
Combina clima real, Google Calendar y la interfaz Glyph para recomendar outfits diarios.

El código para Nothing Phone 3a (A059, DEVICE_24111, 36 canales en arco) se conserva
porque sigue siendo parte del repo, pero ya no es el dispositivo activo del autor.

- **Package:** `com.irofactory.meteoroglyph`
- **Repo:** https://github.com/RemiH06/Meteoroglyph
- **Licencia:** AGPL-3.0

---

## Stack

- Kotlin + Jetpack Compose
- `minSdk` / `targetSdk` / `compileSdk`: API 36 (Android 16, sintaxis `release(36) { minorApiLevel = 1 }`)
- Nothing Glyph SDK 2.0 (`app/libs/glyph-matrix-sdk-2.0.aar`)
- Fuentes: `Ndot-57` (nothingfont), `SpaceMono` — en `res/font/`
- Clima: Open-Meteo (sin API key)
- Persistencia: DataStore Preferences
- Background: WorkManager + AlarmManager exacto

---

## Estructura de archivos

```
app/src/main/
├── java/com/irofactory/meteoroglyph/
│   ├── MainActivity.kt              — singleton GlyphController, permisos, tema
│   ├── data/
│   │   ├── calendar/                CalendarRepository (CalendarContract + keywords + color morado)
│   │   ├── glyph/                   GlyphRepository (parsea JSON assets), GlyphModels
│   │   ├── location/                LocationRepository (FusedLocationProvider)
│   │   ├── outfit/                  OutfitEngine (lógica de recomendación, umbrales dinámicos)
│   │   ├── settings/                AppSettings, SettingsRepository (DataStore)
│   │   └── weather/                 WeatherRepository (Open-Meteo), WeatherModels, WeatherCondition
│   ├── glyph/
│   │   ├── GlyphController.kt       9 patrones LED climáticos, arcos 3a (singleton)
│   │   ├── GlyphMatrixController.kt Glifo de clima en la Glyph Matrix 25x25, Phone 3 (singleton)
│   │   └── toy/
│   │       └── WeatherGlyphToyService.kt  Glyph Toy: clima con el botón trasero
│   ├── icon/
│   │   └── IconUpdater.kt           Ícono dinámico via ActivityAlias (11 estados)
│   ├── ui/
│   │   ├── components/              GlyphRenderer, GlyphBitmapRenderer, TextBitmapRenderer,
│   │   │                            WeatherStrip, OutfitChips, StatusChip
│   │   ├── screens/                 HomeScreen, SettingsScreen
│   │   ├── navigation/              NavGraph (Home ↔ Settings)
│   │   └── theme/                   Color.kt, Type.kt, Theme.kt (metro_theme dark/light)
│   ├── viewmodel/                   HomeViewModel, SettingsViewModel
│   ├── widget/                      WeatherInfoWidget, WeatherCircleWidget (Glance)
│   └── worker/                      WeatherCheckWorker, EventAlarmScheduler,
│                                    EventAlarmReceiver, BootReceiver
└── assets/glyphs/
    ├── weather.json     — 17 glifos 12×12 (se reusan en la Glyph Matrix, el SDK escala el bitmap)
    ├── ui.json          — 9 glifos 7×7
    ├── accessories.json — 8 glifos 9×9
    ├── transport.json   — 4 glifos 9×9
    ├── clothes.json     — 15 glifos 9×9
    ├── weather25x25.json         — exportado de GlyphFactory, solo tiene "storm1", sin conectar al repo
    └── glyphfactory_library.json — exportado de GlyphFactory, glifos sueltos sin relación, sin conectar
```

---

## Diseño visual — metro_theme

Paleta semántica implementada como `MetroColors` via `CompositionLocal`:

| Token | Oscuro | Claro | Uso |
|---|---|---|---|
| `accent` | `#00E5A0` | `#6B1A2A` | Verde / acción principal |
| `warn` | `#F5A623` | `#C4691A` | Ámbar / advertencia |
| `danger` | `#FF4560` | `#8B1A1A` | Rojo / bloqueado |
| `blue` | `#457BFF` | `#1A3A6B` | Info / lluvia |
| `purple` | `#9B6DFF` | `#4A1A6B` | Eventos de calendario |
| `orange` | `#FF7A30` | `#A84510` | Transporte |
| `background` | `#080808` | `#F5F5F5` | Fondo principal |

Acceso en Compose: `val mc = metroColors` — nunca hardcodear colores.

Tipografía:
- `Ndot57` / `Ndot57Caps` — temperatura, labels, badges
- `SpaceMono` — cuerpo, metadatos, código
- `MaterialTheme.typography.displayLarge` (48sp Ndot57) = temperatura grande
- `MaterialTheme.typography.labelSmall` (9sp Ndot57Caps) = section titles

---

## Sistema de glifos

Todos los íconos son matrices JSON diseñadas en **GlyphFactory** (proyecto separado).
Formato de cada glifo:
```json
{
  "name": "rain",
  "category": "weather",
  "cols": 12, "rows": 12,
  "palette": ["#00e5a0", "#f5a623", ...],
  "data": [[null, 3, null, ...], ...]
}
```
`null` = celda vacía (transparente), `Int` = índice en palette.

Renderizado:
- En Compose: `GlyphRenderer(glyph, tint, modifier)` — dibuja círculos en Canvas
- Como Bitmap: `GlyphBitmapRenderer.render(glyph, sizePx, tintArgb)` — para notificaciones/widgets
- Como texto Ndot57: `TextBitmapRenderer.render(context, text, sizePx, colorArgb)` — para RemoteViews

---

## Glyph Matrix — Nothing Phone 3 (dispositivo activo)

Dispositivo: **DEVICE_23112** (identificador real de Phone 3 en el SDK, a pesar del nombre),
matriz de 25x25, se obtiene con `Common.getDeviceMatrixLength()`.

API distinta a la de los arcos: `GlyphMatrixManager` (no `GlyphManager`), con
`GlyphMatrixObject` / `GlyphMatrixFrame` para componer capas (imagen o texto) y
`setAppMatrixFrame` para dibujar desde la app sin chocar con los Glyph Toys del sistema
(que tienen prioridad de despliegue si el usuario usa el botón Glyph).

`GlyphMatrixController` es **singleton** (`GlyphMatrixController.getInstance(context)`).
Siempre verificar `Common.is23112()` antes de usar el SDK.
Reusa los 17 glifos de `weather.json` renderizados a Bitmap 1:1 vía `GlyphBitmapRenderer`,
el SDK se encarga de escalarlos a la matriz real.

### Glyph Toy (`WeatherGlyphToyService`)

Servicio declarado en el manifest con `<action android:name="com.nothing.glyph.TOY"/>` y
metadata `com.nothing.glyph.toy.name` / `.image` / `.summary` / `.longpress`. El sistema
lo bindea cuando el usuario lo selecciona en el carrusel del botón Glyph (short-press cicla
entre toys, ya lo maneja el sistema, no la app).

Interacción vía `Messenger` + `Handler`, mensajes `GlyphToy.MSG_GLYPH_TOY` con el evento en
`Bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)`:
- Selección del toy → refresca el clima y muestra el glifo
- `EVENT_ACTION_DOWN` (mantener presionado) → muestra la temperatura en texto
- `EVENT_ACTION_UP` (soltar) → vuelve al glifo
- `EVENT_CHANGE` (long-press del botón Glyph) → fuerza un refresh contra la API

Referencia verificada contra el repo oficial de Nothing (no asumir de memoria si se
vuelve a tocar este servicio): [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit),
[GlyphMatrix-Example-Project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project).

---

## Glyph SDK — Nothing Phone 3a (legado, ya no es el dispositivo del autor)

Dispositivo: **DEVICE_24111** (A059), 36 canales totales.

Mapa físico de los arcos:
- **Arco C** (índices 0–19): **10° a 60°** — superior izquierda
- **Arco A** (índices 20–30): **165° a 215°** — inferior izquierda / abajo
- **Arco B** (índices 31–35): **305° a 330°** — derecha

`GlyphController` es **singleton** (`GlyphController.getInstance(context)`).
Siempre verificar `Common.is24111()` antes de usar el SDK.

Patrones implementados: `rain`, `storm`, `heat`, `wind`, `cold`, `fog`, `sleet`, `snow`, `upcomingEvent`.

---

## Notificaciones

Dos canales:
1. `meteoroglyph_daily` — WorkManager, hora configurable, resumen diario
2. `meteoroglyph_events` — AlarmManager exacto, 1 hora antes de cada evento fuera de casa

Ambas usan **RemoteViews** con layouts XML (`notification_collapsed`, `notification_expanded`),
glifos como Bitmap y texto Ndot57 renderizado como Bitmap via `TextBitmapRenderer`.
Fondo fijo `#313035` (match con sistema de notificaciones de Nothing OS).

---

## Ícono dinámico

11 `ActivityAlias` en el manifest, todos apuntando a `.MainActivity` (sin intent-filter propio).
`IconUpdater.update(context, weather)` activa el alias correcto y desactiva los demás.
PNGs generados con `generate_icons.py` (requiere Pillow) en `mipmap-xxxhdpi/` y `mipmap-night-xxxhdpi/`.

---

## Widgets (Glance)

- `WeatherInfoWidget` / `WeatherInfoWidgetReceiver` — rectangular, clima + outfit chips
- `WeatherCircleWidget` / `WeatherCircleWidgetReceiver` — circular (`cornerRadius(999.dp)`), solo glifo de clima
- Fondo: `#f5edf4` (claro) / `#1e1a20` (oscuro) via `ColorProvider(day, night)`
- Actualización: llamar `WeatherInfoWidget().updateAll(context)` desde el worker

---

## Convenciones de código

- Filosofía **ponytail** — lo mínimo necesario, nunca sacrificar validación ni seguridad
- Conventional commits breves sin saltos de línea
- Español mexicano en todo texto de UI y comentarios
- Sin guión largo (`—`) en ningún texto generado
- `metroColors` en vez de colores hardcodeados
- `GlyphController.getInstance(context)` / `GlyphMatrixController.getInstance(context)` — nunca instanciar directamente

---

## Dependencias clave (build.gradle.kts)

```kotlin
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("androidx.work:work-runtime-ktx:2.10.1")
implementation("androidx.navigation:navigation-compose:2.8.9")
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.glance:glance-material3:1.1.1")
implementation("com.google.android.gms:play-services-location:21.3.0")
implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
```

---

## Pendientes / ideas futuras

- El set de 17 glifos climáticos nativos a 25x25 ya está en `weather25x25.json`, pulido
  a mano en GlyphFactory (`GlyphMatrixController` y el Glyph Toy ya leen de ahí, no de
  `weather.json`). Es probable que se vuelva a retocar; si se rehacen, mantener el mismo
  espaciado simétrico: misma cantidad de celdas nulas arriba que abajo, y misma cantidad
  a la izquierda que a la derecha del ícono dentro del lienzo de 25x25.
- La Glyph Matrix real del Phone 3 no ilumina las 625 celdas del lienzo, solo 489 en
  forma de diamante (filas de 7 a 25 celdas de ancho según la fila, ver
  `ROW_SPANS`/`MASK` en `tools/generate_toy_icon.py` para la forma exacta, viene de
  50.RemsGlyphToys). Los 17 glifos actuales no están recortados a esa forma, cualquier
  detalle en las esquinas del lienzo no se va a ver en el hardware.
- El brillo hacia la Glyph Matrix (`GlyphBitmapRenderer.renderToMatrixArray`) manda
  cualquier celda no nula a brillo máximo (255), sin leer el color de paleta como
  luminancia. El LED es monocromático, así que un color como el gris de contorno se
  vería correcto en pantalla pero saldría tenue si se calculara por luminancia.
- Widget de galería — imagen → matriz de puntos via average pooling
- Historial de outfits y aprendizaje
- Migrar dependencias al version catalog de Gradle

## Fuera de este proyecto (movido a una app nueva)

La simulación de fluidos (SPH) que vivía en `fluid/` se quitó de Meteoroglyph. El plan es
llevarla a un proyecto nuevo (Glyph Matrix 25x25) enfocado en juguetes personalizados:
1. La simulación de fluidos como primer juguete (el código sigue disponible en el historial
   de git de este repo, commit `1b0beaf` en adelante)
2. Una animación tipo esfera de NCS que reaccione al audio que se esté reproduciendo
3. Una glyph factory integrada en la propia app para poner cualquier imagen del dispositivo
   en la matriz (esto último todavía solo es idea, no hay fecha)