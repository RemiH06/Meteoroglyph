package com.irofactory.meteoroglyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.ui.theme.AccentGreen
import com.irofactory.meteoroglyph.ui.theme.AmberSurface
import com.irofactory.meteoroglyph.ui.theme.Border
import com.irofactory.meteoroglyph.ui.theme.DangerRed
import com.irofactory.meteoroglyph.ui.theme.GreenSurface
import com.irofactory.meteoroglyph.ui.theme.RedSurface
import com.irofactory.meteoroglyph.ui.theme.SpaceMono
import com.irofactory.meteoroglyph.ui.theme.WarnAmber

enum class ChipState { OK, WARN, DANGER, NEUTRAL }

@Composable
fun StatusChip(
    modifier: Modifier = Modifier,
    state: ChipState = ChipState.NEUTRAL,
    label: String
) {
    val (bg, fg, border) = when (state) {
        ChipState.OK      -> Triple(GreenSurface, AccentGreen, AccentGreen.copy(alpha = 0.3f))
        ChipState.WARN    -> Triple(AmberSurface, WarnAmber,   WarnAmber.copy(alpha = 0.3f))
        ChipState.DANGER  -> Triple(RedSurface,   DangerRed,   DangerRed.copy(alpha = 0.3f))
        ChipState.NEUTRAL -> Triple(Color(0xFF111111), Color(0xFF555555), Border)
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(0.5.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text          = label.uppercase(),
            color         = fg,
            fontFamily    = SpaceMono,
            fontSize      = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
            letterSpacing = 0.06.sp
        )
    }
}