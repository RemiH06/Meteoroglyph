package com.irofactory.meteoroglyph.data.settings

data class AppSettings(
    // ── Ubicación ─────────────────────────────────────────────────────────────
    val useGps: Boolean             = true,
    val homeLat: Double             = 20.6597,
    val homeLon: Double             = -103.3496,
    val homeLabel: String           = "Casa",

    // ── Trabajos (hasta 3) ────────────────────────────────────────────────────
    val workplaces: List<Location>  = emptyList(),

    // ── Escuelas (hasta 3) ────────────────────────────────────────────────────
    val schools: List<Location>     = emptyList(),

    // ── Keywords → ubicación ──────────────────────────────────────────────────
    val calendarKeywords: List<CalendarKeyword> = emptyList(),

    // ── Umbrales de clima ─────────────────────────────────────────────────────
    val heatThresholdC: Int         = 28,
    val coldThresholdC: Int         = 14,
    val rainThresholdPct: Int       = 40,
    val windThresholdKmh: Int       = 35,

    // ── Transporte ────────────────────────────────────────────────────────────
    val uberRainThresholdPct: Int   = 60,

    // ── Notificaciones ────────────────────────────────────────────────────────
    val notificationHour: Int       = 7,
    val notificationMinute: Int     = 0,

    // ── Tema ──────────────────────────────────────────────────────────────────
    val themeMode: String = "SYSTEM",  // "SYSTEM" | "DARK" | "LIGHT"

    // ── Simulación de fluido ──────────────────────────────────────────────────
    val fluidFillRatio:       Float = 0.30f,
    val fluidViscosity:       Float = 0.50f,
    val fluidStiffness:       Float = 1.00f,
    val fluidRestitution:     Float = 0.40f,
    val fluidSmoothingRadius: Float = 2.50f,
    val fluidParticleCount:   Int   = 80
)

data class Location(
    val label: String,
    val lat: Double,
    val lon: Double
)

data class CalendarKeyword(
    val keyword: String,       // "MdG T-202", "Chambeanding"
    val locationLabel: String  // "Escuela 1", "Trabajo 1" — debe coincidir con Location.label
)