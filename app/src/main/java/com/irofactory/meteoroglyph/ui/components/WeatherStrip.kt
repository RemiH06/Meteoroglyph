package com.irofactory.meteoroglyph.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.data.glyph.ParsedGlyph
import com.irofactory.meteoroglyph.ui.theme.Border
import com.irofactory.meteoroglyph.ui.theme.PurpleEvent
import com.irofactory.meteoroglyph.ui.theme.SpaceMono
import com.irofactory.meteoroglyph.ui.theme.Surface1
import com.irofactory.meteoroglyph.ui.theme.TextSecondary
import com.irofactory.meteoroglyph.ui.theme.WarnAmber

@Composable
fun WeatherStrip(
    modifier: Modifier = Modifier,
    temp: Int,
    condition: String,
    rainWindow: String?,
    nextEvent: String?,
    glyph: ParsedGlyph? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, Border, RoundedCornerShape(12.dp)),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (glyph != null) {
                GlyphRenderer(
                    glyph    = glyph,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Temperatura y condición
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "$temp°",
                    style      = MaterialTheme.typography.displayMedium,
                    color      = WarnAmber
                )
                Text(
                    text       = condition,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = TextSecondary
                )
                if (rainWindow != null) {
                    Text(
                        text  = "lluvia $rainWindow",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Evento siguiente
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(label = "precaución", state = ChipState.WARN)
                if (nextEvent != null) {
                    Text(
                        text       = nextEvent,
                        fontFamily = SpaceMono,
                        fontSize   = 10.sp,
                        color      = PurpleEvent,
                        modifier   = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}