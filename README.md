![Made with Kotlin](https://forthebadge.com/images/badges/made-with-java.svg)
![Built with Love](http://ForTheBadge.com/images/badges/built-with-love.svg)

```
 ███╗   ███╗███████╗████████╗███████╗ ██████╗ ██████╗  ██████╗ ██╗  ██╗   ██╗██████╗ ██╗  ██╗
 ████╗ ████║██╔════╝╚══██╔══╝██╔════╝██╔═══██╗██╔══██╗██╔═══██╗██║  ╚██╗ ██╔╝██╔══██╗██║  ██║
 ██╔████╔██║█████╗     ██║   █████╗  ██║   ██║██████╔╝██║   ██║██║   ╚████╔╝ ██████╔╝███████║
 ██║╚██╔╝██║██╔══╝     ██║   ██╔══╝  ██║   ██║██╔══██╗██║   ██║██║    ╚██╔╝  ██╔═══╝ ██╔══██║
 ██║ ╚═╝ ██║███████╗   ██║   ███████╗╚██████╔╝██║  ██║╚██████╔╝███████╗██║   ██║     ██║  ██║
 ╚═╝     ╚═╝╚══════╝   ╚═╝   ╚══════╝ ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝   ╚═╝     ╚═╝  ╚═╝
        by Hex (@RemiH06) · iroFactory          version 2.0
```

![Maintained](https://img.shields.io/badge/Maintained%3F-yes-green.svg?style=for-the-badge)
![AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-16%2B-green.svg?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg?style=for-the-badge)

## 🌦️

### General Description

**meteoroglyph** is a personal weather + outfit assistant for the Nothing Phone 3a. It reads your calendar and current weather to recommend what to wear each day, when to take an umbrella, and whether to use public transit or call an Uber. All rendered in a custom dot-matrix aesthetic built on top of [metro_theme](https://remih06.github.io/iroFactory/metro/metro_theme_demo.html) and Nothing's Ndot-57 typeface.

Designed exclusively for the **Nothing Phone 3a** for it integrates with the Glyph SDK to run fluid simulations and weather patterns on the physical LED arcs.

```diff
+ Requires Android 16 (Nothing OS 4.0) or higher
+ Glyph features require Nothing Phone 3a (A059 / DEVICE_24111)
- Not available on the Play Store — personal project, sideload only
- Nothing API key required for distribution (uses "test" key for development)
```

---

## Screenshots

| Home Screen | Widget Circle | Widget Info |
|:-----------:|:-------------:|:-----------:|
| <img src="https://remiah06.github.io/Meteoroglyph/mg_ss1.png" onerror="this.src='docs/mg_ss1.png'" width="220"/> | <img src="https://remiah06.github.io/Meteoroglyph/mg_ss3.png" onerror="this.src='docs/mg_ss3.png'" width="220"/> | <img src="https://remiah06.github.io/Meteoroglyph/mg_ss4.png" onerror="this.src='docs/mg_ss4.png'" width="220"/> |

### Settings & Fluid Simulation

https://remiah06.github.io/Meteoroglyph/mg_ss2.mp4

---

## Requirements

- Nothing Phone 3a running Nothing OS 4.0 (Android 16)
- Android Studio Meerkat or later
- JDK 21
- Google Calendar (optional — for event-based notifications)
- Internet connection (weather data via Open-Meteo)

---

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/RemiH06/meteoroglyph.git
   cd meteoroglyph
   ```

2. Open the project in **Android Studio** (`File → Open`)

3. Download the **Nothing Glyph SDK** (v2.0) from the [Nothing Developer Programme](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) and place the `.aar` file at:
   ```
   app/libs/glyph-matrix-sdk-2.0.aar
   ```

4. Download the **Ndot-57** fonts from [nothingfont](https://github.com/xeji01/nothingfont) and place them in `app/src/main/res/font/`:
   ```
   ndot57_regular.otf
   ndot57caps_regular.otf
   ```

5. Connect your Nothing Phone 3a via USB with **Developer Options → USB Debugging** enabled

6. Run the project from Android Studio (`▶ Run`)

7. On first launch, grant the requested permissions:
   - Calendar access (for event-based notifications)
   - Location (for automatic GPS weather)
   - Notifications

---

## Configuration

Open the app and navigate to **Settings** (gear-like icon in the top right):

| Section | Description |
|---|---|
| **Location** | Toggle GPS or set manual coordinates for home |
| **Workplaces / Schools** | Up to 3 of each — used to resolve calendar event locations |
| **Calendar Keywords** | Map event title keywords to specific locations (e.g. "Chambeanding" → Trabajo 1) |
| **Weather Thresholds** | Customize at what temperature, rain %, or wind speed alerts trigger |
| **Daily Notification** | Set the time for your morning outfit briefing |
| **Theme** | System / Dark / Light |
| **Fluid Simulation** | Tune the SPH fluid parameters: fill ratio, viscosity, stiffness, restitution, smoothing radius, particle count |
| **Test Glyph** | Test each weather LED pattern on the physical arcs |

---

## Features

- Real-time weather via **Open-Meteo** (no API key required)
- Dynamic outfit recommendations based on weather + configured thresholds
- Google Calendar integration with keyword-to-location mapping
- Event color detection (purple = social event)
- GPS automatic location or manual coordinates
- Daily morning notification with outfit summary (custom RemoteViews, Ndot-57 font, glyph bitmaps)
- Event notifications 1 hour before each calendar event
- Dynamic app icon that changes with the weather condition (11 states, light/dark variants)
- Custom dot-matrix icon system via **GlyphFactory** (JSON-based glyph library)
- Nothing Ndot-57 typeface throughout the UI
- Full **metro_theme** aesthetic — dark/light mode with semantic color tokens
- Home screen widgets (rectangular info + circular glyph)
- **Glyph LED patterns** — 9 weather-specific animations on the physical arcs
- **SPH fluid simulation** — real-time physics on the 25×25 dot matrix (screen) and Glyph arcs, responding to the accelerometer

---

## Project Structure

```
app/src/main/
├── java/com/irofactory/meteoroglyph/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── calendar/          CalendarRepository, CalendarModels
│   │   ├── glyph/             GlyphRepository, GlyphModels
│   │   ├── location/          LocationRepository
│   │   ├── outfit/            OutfitEngine
│   │   ├── settings/          AppSettings, SettingsRepository
│   │   └── weather/           WeatherRepository, WeatherModels
│   ├── fluid/
│   │   ├── FluidSimulation.kt     SPH physics engine
│   │   ├── FluidGlyphController.kt Glyph LED bridge
│   │   └── FluidMatrixView.kt     25×25 Compose canvas
│   ├── glyph/
│   │   └── GlyphController.kt     Weather LED patterns
│   ├── icon/
│   │   └── IconUpdater.kt         Dynamic app icon
│   ├── ui/
│   │   ├── components/        GlyphRenderer, WeatherStrip, OutfitChips...
│   │   ├── screens/           HomeScreen, SettingsScreen
│   │   ├── navigation/        NavGraph
│   │   └── theme/             Color, Type, Theme (metro_theme)
│   ├── viewmodel/             HomeViewModel, SettingsViewModel
│   ├── widget/                WeatherInfoWidget, WeatherCircleWidget
│   └── worker/                WeatherCheckWorker, EventAlarmScheduler...
└── assets/
    └── glyphs/
        ├── weather.json       17 weather glyphs (12×12)
        ├── ui.json            9 UI glyphs (7×7)
        ├── accessories.json   8 accessory glyphs (9×9)
        ├── transport.json     4 transport glyphs (9×9)
        └── clothes.json       15 clothing glyphs (9×9)
```

---

## Related Projects

- **[GlyphFactory](https://github.com/RemiH06/GlyphFactory)**: the dot-matrix glyph editor used to create all icons in this project
- **[metro_theme](https://github.com/RemiH06/iroFactory)**: the CSS/design system this app's aesthetic is based on

---

## Glyph SDK Notice

This project uses the **Nothing Glyph SDK** under the Nothing Developer Programme terms. The `NothingKey` is set to `"test"` for development purposes. Distribution requires a valid API key from Nothing.

Fonts (Ndot-57, NType82) are property of **Nothing Technology Limited**. Used with credit per repository terms at [nothingfont](https://github.com/xeji01/nothingfont).

---

## Future Features (if I get a new phone)

- Migrate to Nothing OS with Glyph Matrix (Phone 3 / 3+ support)
- Full 25×25 Glyph Matrix fluid simulation when hardware is available
- Gallery image → dot matrix widget
- Outfit history and learning
- Apple Watch–style complication widgets

---

## License

AGPL-3.0 © Hex (@RemiH06) · iroFactory

This project's documentation is built on top of [metro_theme](https://github.com/RemiH06/metro_theme) (AGPL-3.0).