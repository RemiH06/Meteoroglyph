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
import com.irofactory.meteoroglyph.ui.theme.metroColors

@Composable
fun WeatherStrip(
    modifier: Modifier = Modifier,
    temp: Int,
    condition: String,
    rainWindow: String?,
    nextEvent: String?,
    glyph: ParsedGlyph? = null
) {
    val mc = metroColors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, mc.border, RoundedCornerShape(12.dp)),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = mc.surface1)
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
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text  = "$temp°",
                    style = MaterialTheme.typography.displayMedium,
                    color = mc.warn
                )
                Text(
                    text       = condition,
                    fontFamily = SpaceMono,
                    fontSize   = 11.sp,
                    color      = mc.textSecondary
                )
                if (rainWindow != null) {
                    Text(
                        text       = "lluvia $rainWindow",
                        fontFamily = SpaceMono,
                        fontSize   = 10.sp,
                        color      = mc.textSecondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(label = "precaución", state = ChipState.WARN)
                if (nextEvent != null) {
                    Text(
                        text       = nextEvent,
                        fontFamily = SpaceMono,
                        fontSize   = 10.sp,
                        color      = mc.purple,
                        modifier   = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}