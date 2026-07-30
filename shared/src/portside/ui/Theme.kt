package portside.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle
import portside.model.SailingStatus
import portside.model.PortsidePalette

/**
 * Portside look: a night-sea chart backdrop with a light deck sheet floating over it.
 */
object PortsideColors {
    // Every value comes from the shared palette so the Compose apps and the
    // all-SwiftUI app cannot drift apart.
    val Sea = Color(PortsidePalette.Sea)
    val SeaGlow = Color(PortsidePalette.SeaGlow)
    val Horizon = Color(PortsidePalette.Horizon)
    val Route = Color(PortsidePalette.Route)
    val Shimmer = Color(PortsidePalette.Shimmer)

    val SheetBg = Color(PortsidePalette.SheetBg)
    val CardBg = Color(PortsidePalette.CardBg)
    val Divider = Color(PortsidePalette.Divider)
    val ChipBg = Color(PortsidePalette.ChipBg)
    val MenuGlass = Color(PortsidePalette.MenuGlass)
    val TextDark = Color(PortsidePalette.TextDark)
    val TextGray = Color(PortsidePalette.TextGray)

    val GreenTime = Color(PortsidePalette.GreenTime)
    val RedTime = Color(PortsidePalette.RedTime)
    val Accent = Color(PortsidePalette.Accent)
    val BerthChip = Color(PortsidePalette.BerthChip)
    val BerthChipText = Color(PortsidePalette.BerthChipText)
    val AvatarEnd = Color(PortsidePalette.AvatarEnd)

    val LogTop = Color(PortsidePalette.LogTop)
    val LogBottom = Color(PortsidePalette.LogBottom)
}

val SailingStatus.tint: Color
    get() = when (this) {
        SailingStatus.Scheduled -> PortsideColors.GreenTime
        SailingStatus.Delayed -> PortsideColors.RedTime
        SailingStatus.AtSea -> PortsideColors.Accent
        SailingStatus.Docked -> PortsideColors.TextGray
    }

/** Color of the big departure/arrival times, Portside-style. */
val SailingStatus.timeTint: Color
    get() = when (this) {
        SailingStatus.Scheduled -> PortsideColors.GreenTime
        SailingStatus.Delayed -> PortsideColors.RedTime
        SailingStatus.AtSea -> PortsideColors.GreenTime
        SailingStatus.Docked -> PortsideColors.GreenTime
    }

val SailingStatus.label: String
    get() = when (this) {
        SailingStatus.Scheduled -> "On Time"
        SailingStatus.Delayed -> "Delayed"
        SailingStatus.AtSea -> "At Sea"
        SailingStatus.Docked -> "Docked"
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PortsideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PortsideColors.Accent,
            onPrimary = Color.White,
            background = PortsideColors.Sea,
            onBackground = PortsideColors.TextDark,
            surface = PortsideColors.CardBg,
            onSurface = PortsideColors.TextDark,
            surfaceVariant = PortsideColors.SheetBg,
            onSurfaceVariant = PortsideColors.TextGray,
            outline = PortsideColors.Divider,
        ),
    ) {
        // SwiftUI text sits tight to the glyph box; Compose reserves extra
        // leading above/below every line plus Android font padding, which
        // compounds across a card's many rows and makes the Compose apps read
        // ~30% looser than the all-SwiftUI app for identical padding values.
        // Trim that leading and drop font padding so both render at the same
        // density as the all-SwiftUI app.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
            content = content,
        )
    }
}
