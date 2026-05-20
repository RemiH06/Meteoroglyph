package com.irofactory.meteoroglyph.data.outfit

import com.irofactory.meteoroglyph.data.settings.AppSettings
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.OutfitItem
import com.irofactory.meteoroglyph.ui.components.OutfitItemState

object OutfitEngine {

    fun recommend(weather: WeatherState, settings: AppSettings): List<OutfitItem> {
        val items = mutableListOf<OutfitItem>()

        // ── Variables de condición ────────────────────────────────────────────
        val temp      = weather.tempCelsius
        val rain      = weather.rainProbability >= settings.rainThresholdPct
        val heavyRain = weather.rainProbability >= (settings.rainThresholdPct + 30).coerceAtMost(90)
        val wind      = weather.windSpeedKmh >= settings.windThresholdKmh
        val hot       = temp >= settings.heatThresholdC
        val cold      = temp <= settings.coldThresholdC
        val veryCold  = temp <= settings.coldThresholdC - 6
        val rainWindow  = weather.rainWindow
        val storm       = weather.condition == WeatherCondition.STORM
        val snow        = weather.condition == WeatherCondition.SNOW ||
                weather.condition == WeatherCondition.SLEET
        val sunny       = weather.condition == WeatherCondition.SUNNY
        val fog         = weather.condition == WeatherCondition.FOG

        // ── Parte superior ────────────────────────────────────────────────────
        when {
            veryCold || snow -> items.add(outfit("abrigo",          "clothes", "top_coat",     OutfitItemState.RECOMMENDED))
            cold             -> items.add(outfit("suéter",          "clothes", "top_sweater",  OutfitItemState.RECOMMENDED))
            hot              -> items.add(outfit("camiseta",        "clothes", "top_tshirt",   OutfitItemState.RECOMMENDED))
            else             -> items.add(outfit("camisa ligera",   "clothes", "top_shirt",    OutfitItemState.RECOMMENDED))
        }

        if (cold && !veryCold) {
            items.add(outfit("hoodie", "clothes", "top_hoodie",
                if (rain) OutfitItemState.CONDITIONAL else OutfitItemState.RECOMMENDED,
                if (rain) "puede mojarse" else null))
        }

        // ── Parte inferior ────────────────────────────────────────────────────
        when {
            veryCold || snow -> items.add(outfit("pantalón grueso", "clothes", "bottom_jeans",       OutfitItemState.RECOMMENDED))
            cold             -> items.add(outfit("jeans",           "clothes", "bottom_jeans",       OutfitItemState.RECOMMENDED))
            hot && !rain && !wind -> {
                items.add(outfit("short",   "clothes", "bottom_shorts", OutfitItemState.RECOMMENDED))
                items.add(outfit("falda",   "clothes", "bottom_skirt",  OutfitItemState.RECOMMENDED))
            }
            rain || wind     -> {
                items.add(outfit("pantalón largo", "clothes", "bottom_pants_light", OutfitItemState.RECOMMENDED))
                items.add(outfit("short",  "clothes", "bottom_shorts", OutfitItemState.BLOCKED, "lluvia/viento"))
                items.add(outfit("falda",  "clothes", "bottom_skirt",  OutfitItemState.BLOCKED, "lluvia/viento"))
            }
            else             -> {
                items.add(outfit("pantalón ligero", "clothes", "bottom_pants_light", OutfitItemState.RECOMMENDED))
                items.add(outfit("short",  "clothes", "bottom_shorts", OutfitItemState.CONDITIONAL, "posible lluvia"))
            }
        }

        if (cold) {
            items.add(outfit("leggings", "clothes", "bottom_leggings",
                OutfitItemState.CONDITIONAL, "bajo el pantalón"))
        }

        // ── Calzado ───────────────────────────────────────────────────────────
        when {
            heavyRain || snow -> {
                items.add(outfit("bota impermeable", "clothes", "shoe_long_boot",  OutfitItemState.RECOMMENDED))
                items.add(outfit("bota corta",       "clothes", "shoe_ankle_boot", OutfitItemState.CONDITIONAL, "lluvia leve"))
                items.add(outfit("tenis",            "clothes", "shoe_sneaker",    OutfitItemState.BLOCKED,     "lluvia fuerte"))
                items.add(outfit("sandalias",        "clothes", "shoe_sandal",     OutfitItemState.BLOCKED,     "lluvia"))
            }
            rain -> {
                items.add(outfit("bota corta",  "clothes", "shoe_ankle_boot", OutfitItemState.RECOMMENDED))
                items.add(outfit("zapato cerrado", "clothes", "shoe_closed",  OutfitItemState.RECOMMENDED))
                items.add(outfit("tenis",        "clothes", "shoe_sneaker",   OutfitItemState.CONDITIONAL, "puede mojarse"))
                items.add(outfit("sandalias",    "clothes", "shoe_sandal",    OutfitItemState.BLOCKED,     "lluvia"))
            }
            cold -> {
                items.add(outfit("bota corta",     "clothes", "shoe_ankle_boot", OutfitItemState.RECOMMENDED))
                items.add(outfit("zapato cerrado", "clothes", "shoe_closed",     OutfitItemState.RECOMMENDED))
                items.add(outfit("sandalias",      "clothes", "shoe_sandal",     OutfitItemState.BLOCKED,     "frío"))
            }
            hot -> {
                items.add(outfit("tenis",     "clothes", "shoe_sneaker", OutfitItemState.RECOMMENDED))
                items.add(outfit("sandalias", "clothes", "shoe_sandal",  OutfitItemState.RECOMMENDED))
            }
            else -> {
                items.add(outfit("tenis",          "clothes", "shoe_sneaker",    OutfitItemState.RECOMMENDED))
                items.add(outfit("zapato cerrado", "clothes", "shoe_closed",     OutfitItemState.RECOMMENDED))
                items.add(outfit("sandalias",      "clothes", "shoe_sandal",     OutfitItemState.CONDITIONAL))
            }
        }

        // ── Accesorios ────────────────────────────────────────────────────────
        when {
            storm || heavyRain -> {
                items.add(outfit("paraguas",  "accessories", "umbrella",
                    OutfitItemState.RECOMMENDED, rainWindow?.let { "lluvia $it" }))
                items.add(outfit("sombrilla", "accessories", "parasol",
                    OutfitItemState.BLOCKED, "viento/lluvia"))
                items.add(outfit("mochila impermeable", "accessories", "backpack",
                    OutfitItemState.RECOMMENDED))
            }
            rain -> {
                items.add(outfit("paraguas",  "accessories", "umbrella",
                    OutfitItemState.CONDITIONAL, rainWindow?.let { "lluvia $it" }))
                items.add(outfit("sombrilla", "accessories", "parasol",
                    if (wind) OutfitItemState.BLOCKED else OutfitItemState.NEUTRAL,
                    if (wind) "viento fuerte" else null))
                items.add(outfit("mochila impermeable", "accessories", "backpack",
                    OutfitItemState.CONDITIONAL))
            }
            sunny && hot -> {
                items.add(outfit("sombrilla",    "accessories", "parasol",     OutfitItemState.RECOMMENDED))
                items.add(outfit("lentes de sol","accessories", "sunglasses",  OutfitItemState.RECOMMENDED))
                items.add(outfit("cap",          "accessories", "cap",         OutfitItemState.RECOMMENDED))
            }
            sunny -> {
                items.add(outfit("lentes de sol","accessories", "sunglasses",  OutfitItemState.RECOMMENDED))
                items.add(outfit("cap",          "accessories", "cap",         OutfitItemState.NEUTRAL))
            }
            fog -> {
                items.add(outfit("paraguas", "accessories", "umbrella", OutfitItemState.NEUTRAL))
            }
        }

        if (veryCold || (cold && wind)) {
            items.add(outfit("bufanda",  "accessories", "scarf",  OutfitItemState.RECOMMENDED))
            items.add(outfit("gorro",    "accessories", "beanie", OutfitItemState.RECOMMENDED))
        } else if (cold) {
            items.add(outfit("bufanda",  "accessories", "scarf",  OutfitItemState.CONDITIONAL))
        }

        if (wind && !sunny) {
            items.add(outfit("paraguas", "accessories", "umbrella",
                OutfitItemState.BLOCKED, "viento fuerte"))
        }

        return items
    }

    // ── Transporte ────────────────────────────────────────────────────────────
    fun recommendTransit(weather: WeatherState, settings: AppSettings): TransitRecommendation {
        val rain      = weather.rainProbability >= settings.rainThresholdPct
        val heavyRain = weather.rainProbability >= settings.uberRainThresholdPct
        val storm     = weather.condition == WeatherCondition.STORM
        val wind      = weather.windSpeedKmh >= settings.windThresholdKmh

        return when {
            storm || heavyRain -> TransitRecommendation.UBER
            rain && wind       -> TransitRecommendation.UBER
            rain               -> TransitRecommendation.CONSIDER_UBER
            else               -> TransitRecommendation.PUBLIC_OK
        }
    }

    private fun outfit(
        label:     String,
        file:      String,
        glyphName: String,
        state:     OutfitItemState,
        reason:    String? = null
    ) = OutfitItem(label, file, glyphName, state, reason)
}

enum class TransitRecommendation {
    PUBLIC_OK,       // transporte público sin problema
    CONSIDER_UBER,   // considera uber si llevas cosas
    UBER             // mejor uber/taxi
}