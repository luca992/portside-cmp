package portside.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun glyph(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White), pathBuilder = block)
    }.build()

/**
 * The extra glyphs the shared menu spec asks for, keyed by SF Symbol name.
 * The spec names icons with SF Symbol names; each platform maps
 * those names onto its own set — SwiftUI passes them straight to
 * `Label(systemImage:)`, Compose looks them up here.
 */
internal object MenuIcons {
    val Sparkles: ImageVector = glyph("Sparkles") {
        moveTo(9f, 3f)
        lineTo(10.4f, 7.1f)
        lineTo(14.5f, 8.5f)
        lineTo(10.4f, 9.9f)
        lineTo(9f, 14f)
        lineTo(7.6f, 9.9f)
        lineTo(3.5f, 8.5f)
        lineTo(7.6f, 7.1f)
        close()
        moveTo(17f, 12f)
        lineTo(17.9f, 14.6f)
        lineTo(20.5f, 15.5f)
        lineTo(17.9f, 16.4f)
        lineTo(17f, 19f)
        lineTo(16.1f, 16.4f)
        lineTo(13.5f, 15.5f)
        lineTo(16.1f, 14.6f)
        close()
    }

    val Phone: ImageVector = glyph("Phone") {
        moveTo(6.6f, 10.8f)
        curveToRelative(1.4f, 2.8f, 3.8f, 5.1f, 6.6f, 6.6f)
        lineToRelative(2.2f, -2.2f)
        curveToRelative(0.3f, -0.3f, 0.7f, -0.4f, 1f, -0.2f)
        curveToRelative(1.1f, 0.4f, 2.3f, 0.6f, 3.6f, 0.6f)
        curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
        verticalLineTo(20f)
        curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
        curveToRelative(-9.4f, 0f, -17f, -7.6f, -17f, -17f)
        curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
        horizontalLineToRelative(3.5f)
        curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
        curveToRelative(0f, 1.2f, 0.2f, 2.4f, 0.6f, 3.6f)
        curveToRelative(0.1f, 0.3f, 0f, 0.7f, -0.2f, 1f)
        close()
    }

    val Bolt: ImageVector = glyph("Bolt") {
        // Material "bolt" outline: the spec's bolt.slash maps to the plain
        // bolt here (no slashed variant in the material set).
        moveTo(11f, 21f)
        horizontalLineToRelative(-1f)
        lineToRelative(1f, -7f)
        horizontalLineTo(7.5f)
        curveToRelative(-0.58f, 0f, -0.57f, -0.32f, -0.38f, -0.66f)
        curveToRelative(0.19f, -0.34f, 0.05f, -0.08f, 0.07f, -0.12f)
        curveTo(8.48f, 10.94f, 10.42f, 7.54f, 13f, 3f)
        horizontalLineToRelative(1f)
        lineToRelative(-1f, 7f)
        horizontalLineToRelative(3.5f)
        curveToRelative(0.49f, 0f, 0.56f, 0.33f, 0.47f, 0.51f)
        lineToRelative(-0.07f, 0.15f)
        curveTo(12.96f, 17.55f, 11f, 21f, 11f, 21f)
        close()
    }

    val Trash: ImageVector = glyph("Trash") {
        moveTo(6f, 19f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(8f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(7f)
        horizontalLineTo(6f)
        close()
        moveTo(19f, 4f)
        horizontalLineToRelative(-3.5f)
        lineToRelative(-1f, -1f)
        horizontalLineToRelative(-5f)
        lineToRelative(-1f, 1f)
        horizontalLineTo(5f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(14f)
        close()
    }

    val Archive: ImageVector = glyph("Archive") {
        moveTo(20.5f, 5.2f)
        lineToRelative(-1.4f, -1.7f)
        curveTo(18.9f, 3.2f, 18.5f, 3f, 18f, 3f)
        horizontalLineTo(6f)
        curveToRelative(-0.5f, 0f, -0.9f, 0.2f, -1.2f, 0.5f)
        lineTo(3.5f, 5.2f)
        curveTo(3.2f, 5.6f, 3f, 6f, 3f, 6.5f)
        verticalLineTo(19f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6.5f)
        curveToRelative(0f, -0.5f, -0.2f, -0.9f, -0.5f, -1.3f)
        close()
        moveTo(12f, 17.5f)
        lineTo(6.5f, 12f)
        horizontalLineTo(10f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(3.5f)
        close()
        moveTo(5.1f, 5f)
        lineToRelative(0.8f, -1f)
        horizontalLineToRelative(12f)
        lineToRelative(0.9f, 1f)
        close()
    }

    val Report: ImageVector = glyph("Report") {
        moveTo(20f, 2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(18f)
        lineToRelative(4f, -4f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(13f, 14f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        close()
        moveTo(13f, 10f)
        horizontalLineToRelative(-2f)
        verticalLineTo(5f)
        horizontalLineToRelative(2f)
        close()
    }

    val Sun: ImageVector = glyph("Sun") {
        moveTo(12f, 7f)
        curveToRelative(-2.8f, 0f, -5f, 2.2f, -5f, 5f)
        reflectiveCurveToRelative(2.2f, 5f, 5f, 5f)
        reflectiveCurveToRelative(5f, -2.2f, 5f, -5f)
        reflectiveCurveToRelative(-2.2f, -5f, -5f, -5f)
        close()
        moveTo(11f, 2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-2f)
        close()
        moveTo(11f, 19f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-2f)
        close()
        moveTo(2f, 11f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(2f)
        horizontalLineTo(2f)
        close()
        moveTo(19f, 11f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(-3f)
        close()
        moveTo(4.2f, 5.6f)
        lineTo(5.6f, 4.2f)
        lineTo(7.7f, 6.3f)
        lineTo(6.3f, 7.7f)
        close()
        moveTo(16.3f, 17.7f)
        lineTo(17.7f, 16.3f)
        lineTo(19.8f, 18.4f)
        lineTo(18.4f, 19.8f)
        close()
        moveTo(18.4f, 4.2f)
        lineTo(19.8f, 5.6f)
        lineTo(17.7f, 7.7f)
        lineTo(16.3f, 6.3f)
        close()
        moveTo(6.3f, 16.3f)
        lineTo(7.7f, 17.7f)
        lineTo(5.6f, 19.8f)
        lineTo(4.2f, 18.4f)
        close()
    }

    val Clock: ImageVector = glyph("Clock") {
        moveTo(12f, 2f)
        curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
        reflectiveCurveToRelative(4.5f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.5f, 10f, -10f)
        reflectiveCurveTo(17.5f, 2f, 12f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.4f, 0f, -8f, -3.6f, -8f, -8f)
        reflectiveCurveToRelative(3.6f, -8f, 8f, -8f)
        reflectiveCurveToRelative(8f, 3.6f, 8f, 8f)
        reflectiveCurveToRelative(-3.6f, 8f, -8f, 8f)
        close()
        moveTo(12.5f, 7f)
        horizontalLineTo(11f)
        verticalLineToRelative(6f)
        lineToRelative(5.2f, 3.2f)
        lineToRelative(0.8f, -1.3f)
        lineToRelative(-4.5f, -2.7f)
        close()
    }

    val Swap: ImageVector = glyph("Swap") {
        moveTo(6.5f, 4f)
        lineTo(2.5f, 8f)
        lineTo(6.5f, 12f)
        verticalLineTo(9f)
        horizontalLineTo(14f)
        verticalLineTo(7f)
        horizontalLineTo(6.5f)
        close()
        moveTo(17.5f, 12f)
        lineTo(21.5f, 16f)
        lineTo(17.5f, 20f)
        verticalLineTo(17f)
        horizontalLineTo(10f)
        verticalLineTo(15f)
        horizontalLineTo(17.5f)
        close()
    }
}

/** Maps an SF Symbol name from the shared spec onto this platform's icon. */
internal fun sfSymbolIcon(name: String): ImageVector? = when (name) {
    "sparkles" -> MenuIcons.Sparkles
    "square.and.arrow.up" -> AppIcons.Share
    "arrow.triangle.swap" -> MenuIcons.Swap
    "phone" -> MenuIcons.Phone
    "safari" -> AppIcons.Locate
    "map" -> AppIcons.Map
    "ferry.departure", "ferry.arrival", "ferry.circle" -> AppIcons.Ship
    "person.2", "person.2.badge.gearshape" -> AppIcons.People
    "person.crop.circle" -> AppIcons.Person
    "exclamationmark" -> null
    "checkmark" -> null
    "ferry" -> AppIcons.Ship
    "bell.slash" -> AppIcons.BellOff
    "bell" -> AppIcons.Bell
    "qrcode" -> AppIcons.Qr
    "calendar" -> AppIcons.Calendar
    "location" -> AppIcons.Locate
    "bolt.slash", "bolt" -> MenuIcons.Bolt
    "exclamationmark.bubble" -> MenuIcons.Report
    "archivebox" -> MenuIcons.Archive
    "trash" -> MenuIcons.Trash
    "cloud.sun" -> AppIcons.Cloud
    "water.waves" -> AppIcons.Waves
    "wind" -> AppIcons.Wind
    "thermometer" -> AppIcons.Thermo
    "sun.max" -> MenuIcons.Sun
    "clock" -> MenuIcons.Clock
    "ticket" -> MenuIcons.Report
    "stopwatch" -> MenuIcons.Clock
    "cabin" -> AppIcons.Person
    "clock.arrow.2.circlepath" -> MenuIcons.Clock
    "gearshape" -> AppIcons.Settings
    else -> null
}
