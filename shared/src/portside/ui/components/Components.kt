package portside.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import portside.model.Line
import portside.model.SailingPresentation
import portside.model.SailingStatus
import portside.ui.PortsideColors

private fun materialGlyph(
    name: String,
    fillType: PathFillType = PathFillType.NonZero,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.White), pathFillType = fillType, pathBuilder = block)
}.build()

/**
 * The few Material Design glyphs the app needs, built by hand from their path data
 * (Apache 2.0) — the toolchain's compose catalog doesn't expose material-icons-core,
 * and this keeps the dependency list at zero extras.
 */
object AppIcons {

    /** Material directions_boat: hull riding a three-crest wake. */
    val Ship: ImageVector = materialGlyph("Ship") {
        // Wake: three arcs along the bottom.
        moveTo(20f, 21f)
        curveToRelative(-1.39f, 0f, -2.78f, -0.47f, -4f, -1.32f)
        curveToRelative(-2.44f, 1.71f, -5.56f, 1.71f, -8f, 0f)
        curveTo(6.78f, 20.53f, 5.39f, 21f, 4f, 21f)
        horizontalLineTo(2f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(2f)
        curveToRelative(1.38f, 0f, 2.74f, -0.35f, 4f, -0.99f)
        curveToRelative(2.52f, 1.29f, 5.48f, 1.29f, 8f, 0f)
        curveToRelative(1.26f, 0.65f, 2.62f, 0.99f, 4f, 0.99f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-2f)
        close()
        // Hull and superstructure.
        moveTo(3.95f, 19f)
        horizontalLineTo(4f)
        curveToRelative(1.6f, 0f, 3.02f, -0.88f, 4f, -2f)
        curveToRelative(0.98f, 1.12f, 2.4f, 2f, 4f, 2f)
        reflectiveCurveToRelative(3.02f, -0.88f, 4f, -2f)
        curveToRelative(0.98f, 1.12f, 2.4f, 2f, 4f, 2f)
        horizontalLineToRelative(0.05f)
        lineToRelative(1.89f, -6.68f)
        curveToRelative(0.08f, -0.26f, 0.06f, -0.54f, -0.06f, -0.78f)
        reflectiveCurveToRelative(-0.34f, -0.42f, -0.6f, -0.5f)
        lineTo(20f, 10.62f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineToRelative(-3f)
        verticalLineTo(1f)
        horizontalLineTo(9f)
        verticalLineToRelative(3f)
        horizontalLineTo(6f)
        curveTo(4.9f, 4f, 4f, 4.9f, 4f, 6f)
        verticalLineToRelative(4.62f)
        lineToRelative(-1.29f, 0.42f)
        curveToRelative(-0.26f, 0.08f, -0.48f, 0.26f, -0.6f, 0.5f)
        reflectiveCurveToRelative(-0.15f, 0.52f, -0.06f, 0.78f)
        lineTo(3.95f, 19f)
        close()
        moveTo(6f, 6f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(3.97f)
        lineTo(12f, 8f)
        lineTo(6f, 9.97f)
        verticalLineTo(6f)
        close()
    }

    val Back: ImageVector = materialGlyph("Back") {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12f, 4f)
        lineToRelative(-8f, 8f)
        lineToRelative(8f, 8f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        verticalLineToRelative(-2f)
        close()
    }

    val ArrowRight: ImageVector = materialGlyph("ArrowRight") {
        moveTo(12f, 4f)
        lineToRelative(-1.41f, 1.41f)
        lineTo(16.17f, 11f)
        horizontalLineTo(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(12.17f)
        lineToRelative(-5.58f, 5.59f)
        lineTo(12f, 20f)
        lineToRelative(8f, -8f)
        close()
    }

    // Diagonal endpoint arrows drawn as paths — the Unicode glyphs (↗/↘) pick
    // up emoji presentation on skiko/wasm and render inside a little box.
    val ArrowNE: ImageVector = materialGlyph("ArrowNE") {
        moveTo(9f, 5f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(6.59f)
        lineTo(4f, 18.59f)
        lineTo(5.41f, 20f)
        lineTo(17f, 8.41f)
        verticalLineTo(15f)
        horizontalLineToRelative(2f)
        verticalLineTo(5f)
        close()
    }

    val ArrowSE: ImageVector = materialGlyph("ArrowSE") {
        moveTo(15f, 19f)
        verticalLineToRelative(-2f)
        horizontalLineTo(8.41f)
        lineTo(20f, 5.41f)
        lineTo(18.59f, 4f)
        lineTo(7f, 15.59f)
        verticalLineTo(9f)
        horizontalLineTo(5f)
        verticalLineToRelative(10f)
        close()
    }

    val Close: ImageVector = materialGlyph("Close") {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }

    val Person: ImageVector = materialGlyph("Person") {
        moveTo(12f, 12f)
        curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
        reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
        reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        close()
        moveTo(12f, 14f)
        curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(-2f)
        curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
        close()
    }

    val People: ImageVector = materialGlyph("People") {
        moveTo(16f, 11f)
        curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
        reflectiveCurveTo(17.66f, 5f, 16f, 5f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        close()
        moveTo(8f, 11f)
        curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
        reflectiveCurveTo(9.66f, 5f, 8f, 5f)
        curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        close()
        moveTo(8f, 13f)
        curveToRelative(-2.33f, 0f, -7f, 1.17f, -7f, 3.5f)
        verticalLineTo(19f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
        close()
        moveTo(16f, 13f)
        curveToRelative(-0.29f, 0f, -0.62f, 0.02f, -0.97f, 0.05f)
        curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
        verticalLineTo(19f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
        close()
    }

    val Search: ImageVector = materialGlyph("Search") {
        moveTo(15.5f, 14f)
        horizontalLineToRelative(-0.79f)
        lineToRelative(-0.28f, -0.27f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
        reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
        curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f)
        lineToRelative(0.27f, 0.28f)
        verticalLineToRelative(0.79f)
        lineToRelative(5f, 4.99f)
        lineTo(20.49f, 19f)
        lineToRelative(-4.99f, -5f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
        reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
        reflectiveCurveTo(11.99f, 14f, 9.5f, 14f)
        close()
    }

    val Share: ImageVector = materialGlyph("Share") {
        // iOS square-and-arrow-up: arrow shaft + head, then the open box below.
        moveTo(12f, 3f)
        lineTo(8.5f, 6.5f)
        lineTo(9.9f, 7.9f)
        lineTo(11f, 6.8f)
        verticalLineTo(15f)
        horizontalLineTo(13f)
        verticalLineTo(6.8f)
        lineTo(14.1f, 7.9f)
        lineTo(15.5f, 6.5f)
        close()
        moveTo(6f, 10f)
        verticalLineTo(20f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(8f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(10f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(8f)
        horizontalLineTo(8f)
        verticalLineToRelative(-8f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-2f)
        close()
    }

    /** Notifications bell with a "muted" slash, as on Portside's detail action bar. */
    val BellOff: ImageVector = materialGlyph("BellOff") {
        // Bell body + clapper.
        moveTo(12f, 22f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        horizontalLineToRelative(-4f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        close()
        moveTo(18f, 16f)
        verticalLineToRelative(-5f)
        curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
        verticalLineTo(4f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
        verticalLineToRelative(0.68f)
        curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
        verticalLineToRelative(5f)
        lineToRelative(-2f, 2f)
        verticalLineToRelative(1f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(-1f)
        lineToRelative(-2f, -2f)
        close()
        // Diagonal mute slash.
        moveTo(3.4f, 4.8f)
        lineTo(4.8f, 3.4f)
        lineTo(20.6f, 19.2f)
        lineTo(19.2f, 20.6f)
        close()
    }

    /** Weather-layer cloud. */
    val Cloud: ImageVector = materialGlyph("Cloud") {
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
        horizontalLineToRelative(13f)
        curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
        curveToRelative(0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
        close()
    }

    /** Folded map, for the map-style control. */
    val Map: ImageVector = materialGlyph("Map") {
        moveTo(20.5f, 3f)
        lineToRelative(-0.16f, 0.03f)
        lineTo(15f, 5.1f)
        lineTo(9f, 3f)
        lineTo(3.36f, 4.9f)
        curveTo(3.15f, 4.97f, 3f, 5.15f, 3f, 5.38f)
        verticalLineTo(20.5f)
        curveToRelative(0f, 0.28f, 0.22f, 0.5f, 0.5f, 0.5f)
        lineToRelative(0.16f, -0.03f)
        lineTo(9f, 18.9f)
        lineToRelative(6f, 2.1f)
        lineToRelative(5.64f, -1.9f)
        curveToRelative(0.21f, -0.07f, 0.36f, -0.25f, 0.36f, -0.48f)
        verticalLineTo(3.5f)
        curveTo(21f, 3.22f, 20.78f, 3f, 20.5f, 3f)
        close()
        moveTo(15f, 19f)
        lineTo(9f, 16.89f)
        verticalLineTo(5f)
        lineToRelative(6f, 2.11f)
        verticalLineTo(19f)
        close()
    }

    /** Live-tracking ring with a center dot. */
    val Locate: ImageVector = materialGlyph("Locate", fillType = PathFillType.EvenOdd) {
        circle(12f, 12f, 9f)
        circle(12f, 12f, 7.2f)
        circle(12f, 12f, 3f)
    }

    /** Return/undo arrow, for the Add Sailing "Return Sailing" shortcut. */
    val Return: ImageVector = materialGlyph("Return") {
        moveTo(12.5f, 8f)
        curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
        lineTo(2f, 7f)
        verticalLineToRelative(9f)
        horizontalLineToRelative(9f)
        lineToRelative(-3.62f, -3.62f)
        curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
        curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
        lineToRelative(2.37f, -0.78f)
        curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
        close()
    }

    /** Horizontal three-dot "more" glyph. */
    val More: ImageVector = materialGlyph("More") {
        for (cx in listOf(5f, 12f, 19f)) circle(cx, 12f, 2f)
    }

    /** Notifications bell, un-slashed. */
    val Bell: ImageVector = materialGlyph("Bell") {
        moveTo(12f, 22f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        horizontalLineToRelative(-4f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        close()
        moveTo(18f, 16f)
        verticalLineToRelative(-5f)
        curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
        verticalLineTo(4f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
        verticalLineToRelative(0.68f)
        curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
        verticalLineToRelative(5f)
        lineToRelative(-2f, 2f)
        verticalLineToRelative(1f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(-1f)
        lineToRelative(-2f, -2f)
        close()
    }

    /** Minimal QR mark: three finder squares and a dot. */
    val Qr: ImageVector = materialGlyph("Qr", fillType = PathFillType.EvenOdd) {
        for ((x, y) in listOf(3f to 3f, 13f to 3f, 3f to 13f)) {
            moveTo(x, y)
            horizontalLineToRelative(8f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(-8f)
            close()
            moveTo(x + 2f, y + 2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(-4f)
            close()
        }
        moveTo(14f, 14f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-3f)
        close()
        moveTo(18f, 18f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-3f)
        close()
    }

    /** Calendar sheet with a header band. */
    val Calendar: ImageVector = materialGlyph("Calendar", fillType = PathFillType.EvenOdd) {
        moveTo(5f, 4f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        verticalLineToRelative(13f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        close()
        moveTo(5f, 10f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(9f)
        horizontalLineTo(5f)
        close()
    }

    /** Stacked chart-layers glyph for the map-style control. */
    val Layers: ImageVector = materialGlyph("Layers") {
        moveTo(11.99f, 18.54f)
        lineToRelative(-7.37f, -5.73f)
        lineTo(3f, 14.07f)
        lineToRelative(9f, 7f)
        lineToRelative(9f, -7f)
        lineToRelative(-1.63f, -1.27f)
        lineToRelative(-7.38f, 5.74f)
        close()
        moveTo(12f, 16f)
        lineToRelative(7.36f, -5.73f)
        lineTo(21f, 9f)
        lineToRelative(-9f, -7f)
        lineToRelative(-9f, 7f)
        lineToRelative(1.63f, 1.27f)
        lineTo(12f, 16f)
        close()
    }

    /** Three streaming wind bars, for the weather control and wind stat. */
    val Wind: ImageVector = materialGlyph("Wind") {
        for ((y, x0, x1) in listOf(
            Triple(6.5f, 3f, 17f),
            Triple(11.5f, 5f, 21f),
            Triple(16.5f, 3f, 13f),
        )) {
            moveTo(x0, y - 1f)
            horizontalLineTo(x1)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 2f)
            horizontalLineTo(x0)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -2f)
            close()
        }
    }

    /** Compass ring with a needle, for the live-tracking control. */
    val Compass: ImageVector = materialGlyph("Compass", fillType = PathFillType.EvenOdd) {
        circle(12f, 12f, 9.5f)
        circle(12f, 12f, 7.8f)
        moveTo(15.5f, 8.5f)
        lineTo(13.4f, 13.4f)
        lineTo(8.5f, 15.5f)
        lineTo(10.6f, 10.6f)
        close()
    }

    /** Two rolling swells, for the wave-height stat. */
    val Waves: ImageVector = materialGlyph("Waves") {
        for (y in listOf(9f, 15f)) {
            moveTo(2f, y + 1f)
            curveToRelative(1.8f, 0f, 1.8f, -2f, 4.1f, -2f)
            curveToRelative(2.4f, 0f, 2.4f, 2f, 4.1f, 2f)
            curveToRelative(1.8f, 0f, 1.8f, -2f, 4.1f, -2f)
            curveToRelative(2.4f, 0f, 2.4f, 2f, 4.1f, 2f)
            verticalLineToRelative(2f)
            curveToRelative(-2.4f, 0f, -2.4f, -2f, -4.1f, -2f)
            curveToRelative(-1.8f, 0f, -1.8f, 2f, -4.1f, 2f)
            curveToRelative(-2.4f, 0f, -2.4f, -2f, -4.1f, -2f)
            curveToRelative(-1.8f, 0f, -1.8f, 2f, -4.1f, 2f)
            close()
        }
    }

    /** Thermometer: stem capsule over a bulb. */
    val Thermo: ImageVector = materialGlyph("Thermo", fillType = PathFillType.EvenOdd) {
        moveTo(10.5f, 4f)
        curveTo(10.5f, 3.17f, 11.17f, 2.5f, 12f, 2.5f)
        reflectiveCurveTo(13.5f, 3.17f, 13.5f, 4f)
        verticalLineTo(13.2f)
        curveToRelative(1.2f, 0.63f, 2f, 1.88f, 2f, 3.3f)
        curveToRelative(0f, 2.07f, -1.68f, 3.75f, -3.5f, 3.75f)
        reflectiveCurveToRelative(-3.5f, -1.68f, -3.5f, -3.75f)
        curveToRelative(0f, -1.42f, 0.8f, -2.67f, 2f, -3.3f)
        close()
    }

    /** Vertical swap arrows, for the Add Voyage route picker. */
    val SwapVert: ImageVector = materialGlyph("SwapVert") {
        moveTo(16f, 17.01f)
        verticalLineTo(10f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(7.01f)
        horizontalLineToRelative(-3f)
        lineTo(15f, 21f)
        lineToRelative(4f, -3.99f)
        close()
        moveTo(9f, 3f)
        lineTo(5f, 6.99f)
        horizontalLineToRelative(3f)
        verticalLineTo(14f)
        horizontalLineToRelative(2f)
        verticalLineTo(6.99f)
        horizontalLineToRelative(3f)
        close()
    }

    /** Tune-style sliders glyph standing in for the settings gear. */
    val Settings: ImageVector = materialGlyph("Settings") {
        for ((y, knobX) in listOf(5f to 8f, 12f to 16f, 19f to 10f)) {
            moveTo(3f, y - 1f)
            horizontalLineTo(21f)
            verticalLineTo(y + 1f)
            horizontalLineTo(3f)
            close()
            circle(knobX, y, 2.6f)
        }
    }
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = 2 * r, dy1 = 0f)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = -2 * r, dy1 = 0f)
    close()
}

@Composable
fun LineBadge(line: Line, size: Int = 30, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(Color(line.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = line.code,
            color = Color.White,
            style = TextStyle(
                fontSize = (size * 0.36).sp,
                fontWeight = FontWeight.SemiBold,
                // Trimmed, centered line height: at badge sizes the default
                // font padding visibly pushes the glyphs off-center.
                lineHeight = (size * 0.36).sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }
}

/**
 * A time that may carry a trailing " +1" next-day marker, rendered with "+1"
 * as a proper superscript. Done as an AnnotatedString (not the Unicode ⁺¹
 * glyph) because the superscript-plus renders as a missing-glyph box on wasm.
 */
fun timeWithNextDay(time: String): AnnotatedString {
    val marker = " +1"
    return buildAnnotatedString {
        if (time.endsWith(marker)) {
            append(time.dropLast(marker.length))
            withStyle(
                SpanStyle(
                    baselineShift = BaselineShift.Superscript,
                    fontSize = TextUnit(0.72f, TextUnitType.Em),
                    fontWeight = FontWeight.SemiBold,
                ),
            ) { append("+1") }
        } else {
            append(time)
        }
    }
}

/** A pier-sign chip: "Berth 7", "Terminal 2", "Deck 5". */
@Composable
fun BerthChip(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = PortsideColors.BerthChipText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .background(PortsideColors.BerthChip, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Small status caption pill ("AT SEA", "DELAYED") in the status tint. */
@Composable
fun StatusPill(status: SailingStatus, modifier: Modifier = Modifier) {
    val tint = Color(SailingPresentation.pillTint(status))
    Text(
        text = SailingPresentation.statusStyle(status).label,
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * The app's signature mark: a crossing between two shores. A shore tick on
 * each side, the water line between — solid behind the vessel, dashed ahead —
 * and the boat sitting at [progress] (0 = origin shore, 1 = docked).
 */
@Composable
fun CrossingBar(
    progress: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 22.dp,
    boatSize: Dp = 16.dp,
) {
    val accent = PortsideColors.Accent
    val divider = PortsideColors.Divider
    val shore = PortsideColors.TextGray
    val p = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.fillMaxWidth().height(barHeight)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height / 2
            val tick = 3.dp.toPx()
            val tickH = size.height * 0.8f
            val corner = CornerRadius(tick / 2)
            // A shore tick yields to the hull when the boat is parked on it.
            if (p > 0.055f) {
                drawRoundRect(
                    color = shore.copy(alpha = 0.55f),
                    topLeft = Offset(0f, y - tickH / 2),
                    size = Size(tick, tickH),
                    cornerRadius = corner,
                )
            }
            if (p < 0.945f) {
                drawRoundRect(
                    color = shore.copy(alpha = 0.55f),
                    topLeft = Offset(size.width - tick, y - tickH / 2),
                    size = Size(tick, tickH),
                    cornerRadius = corner,
                )
            }
            val inset = tick + 6.dp.toPx()
            val x0 = inset
            val x1 = size.width - inset
            val bx = x0 + (x1 - x0) * p
            val stroke = 2.5.dp.toPx()
            // The water line parts around the hull instead of running under it.
            val clearance = boatSize.toPx() / 2 + 4.dp.toPx()
            if (bx - clearance - x0 > 1f) {
                drawLine(
                    accent, Offset(x0, y), Offset(bx - clearance, y), stroke, cap = StrokeCap.Round,
                )
            }
            if (x1 - (bx + clearance) > 1f) {
                drawLine(
                    color = divider,
                    start = Offset(bx + clearance, y),
                    end = Offset(x1, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(5.dp.toPx(), 6.dp.toPx()),
                    ),
                )
            }
        }
        Icon(
            imageVector = AppIcons.Ship,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .align(BiasAlignment((p * 2f) - 1f, 0f))
                .size(boatSize),
        )
    }
}
