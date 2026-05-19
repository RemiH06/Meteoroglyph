package com.irofactory.meteoroglyph.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.ui.theme.AccentGreen
import com.irofactory.meteoroglyph.ui.theme.Border
import com.irofactory.meteoroglyph.ui.theme.DangerRed
import com.irofactory.meteoroglyph.ui.theme.GreenSurface
import com.irofactory.meteoroglyph.ui.theme.RedSurface
import com.irofactory.meteoroglyph.ui.theme.AmberSurface
import com.irofactory.meteoroglyph.ui.theme.SpaceMono
import com.irofactory.meteoroglyph.ui.theme.TextSecondary
import com.irofactory.meteoroglyph.ui.theme.WarnAmber

enum class OutfitItemState { RECOMMENDED, CONDITIONAL, BLOCKED, NEUTRAL }

data class OutfitItem(
    val label: String,
    val glyphFile: String,    // "clothes", "accessories", "transit"
    val glyphName: String,    // nombre exacto en el JSON
    val state: OutfitItemState,
    val reason: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutfitChip(
    item: OutfitItem,
    glyphRepo: GlyphRepository
) {
    val (bg, fg, borderColor) = when (item.state) {
        OutfitItemState.RECOMMENDED -> Triple(GreenSurface, AccentGreen, AccentGreen.copy(alpha = 0.3f))
        OutfitItemState.CONDITIONAL -> Triple(AmberSurface, WarnAmber,   WarnAmber.copy(alpha = 0.3f))
        OutfitItemState.BLOCKED     -> Triple(RedSurface,   DangerRed,   DangerRed.copy(alpha = 0.2f))
        OutfitItemState.NEUTRAL     -> Triple(Color(0xFF111111), TextSecondary, Border)
    }

    val glyph = remember(item.glyphFile, item.glyphName) {
        glyphRepo.getGlyph(item.glyphFile, item.glyphName)
    }

    val tint = when (item.state) {
        OutfitItemState.RECOMMENDED -> AccentGreen
        OutfitItemState.CONDITIONAL -> WarnAmber
        OutfitItemState.BLOCKED     -> DangerRed
        OutfitItemState.NEUTRAL     -> TextSecondary
    }

    Surface(
        color    = bg,
        shape    = RoundedCornerShape(6.dp),
        modifier = Modifier.border(0.5.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (glyph != null) {
                GlyphRenderer(
                    glyph    = glyph,
                    tint     = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text           = item.label,
                    fontFamily     = SpaceMono,
                    fontSize       = 10.sp,
                    color          = fg,
                    textDecoration = if (item.state == OutfitItemState.BLOCKED)
                        TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.reason != null) {
                    Text(
                        text       = item.reason,
                        fontFamily = SpaceMono,
                        fontSize   = 8.sp,
                        color      = fg.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitChips(
    modifier: Modifier = Modifier,
    items: List<OutfitItem>,
    glyphRepo: GlyphRepository
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text          = "recomendación · hoy",
            fontFamily    = SpaceMono,
            fontSize      = 9.sp,
            color         = TextSecondary,
            letterSpacing = 0.14.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                OutfitChip(item = item, glyphRepo = glyphRepo)
            }
        }
    }
}