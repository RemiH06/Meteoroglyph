# meteoroglyph — CLAUDE.md

Contexto completo del proyecto para continuar desarrollo en Claude Code.

---

## Proyecto

App Android personal para **Nothing Phone 3a (A059, DEVICE_24111, Nothing OS 4.0 / Android 16)**.
Combina clima real, Google Calendar y la interfaz Glyph LED para recomendar outfits diarios.

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
│   ├── fluid/
│   │   ├── FluidSimulation.kt       Motor SPH (partículas, presión, viscosidad, gravedad)
│   │   ├── FluidGlyphController.kt  Puente SPH ↔ LEDs físicos + acelerómetro
│   │   └── FluidMatrixView.kt       Canvas 25×25 circular en Compose (loop withFrameMillis)
│   ├── glyph/
│   │   └── GlyphController.kt       9 patrones LED climáticos (singleton)
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
    ├── weather.json     — 17 glifos 12×12
    ├── ui.json          — 9 glifos 7×7
    ├── accessories.json — 8 glifos 9×9
    ├── transport.json   — 4 glifos 9×9
    └── clothes.json     — 15 glifos 9×9
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

## Glyph SDK — Nothing Phone 3a

Dispositivo: **DEVICE_24111** (A059), 36 canales totales.

Mapa físico de los arcos:
- **Arco C** (índices 0–19): **10° a 60°** — superior izquierda
- **Arco A** (índices 20–30): **165° a 215°** — inferior izquierda / abajo
- **Arco B** (índices 31–35): **305° a 330°** — derecha

`GlyphController` es **singleton** (`GlyphController.getInstance(context)`).
Siempre verificar `Common.is24111()` antes de usar el SDK.

Patrones implementados: `rain`, `storm`, `heat`, `wind`, `cold`, `fog`, `sleet`, `snow`, `upcomingEvent`.

---

## Simulación de fluidos (SPH)

`FluidSimulation` — motor de física con N partículas, kernels Poly6/Spiky/Viscosity.
`FluidGlyphController` — mapea densidad de partículas a brillo de cada LED via posición angular.
`FluidMatrixView` — Canvas 25×25 circular animado con `withFrameMillis`, lee acelerómetro.

Parámetros configurables en Settings: `fillRatio`, `viscosity`, `stiffness`, `restitution`, `smoothingRadius`, `particleCount`.

Comportamiento horizontal: cuando `|az|` domina, se aplica fuerza centrífuga que distribuye las partículas con brillo proporcional al `fillRatio`.

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
- `GlyphController.getInstance(context)` — nunca instanciar directamente

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

- Soporte Nothing Phone 3 / 3+ con Glyph Matrix 25×25 real
- Simulación de fluidos en matriz 25×25 física
- Widget de galería — imagen → matriz de puntos via average pooling
- Historial de outfits y aprendizaje
- Migrar dependencias al version catalog de Gradle