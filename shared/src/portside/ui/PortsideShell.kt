package portside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import portside.ui.components.SailingMenuAboveAnchor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import portside.model.PortsidePalette
import portside.model.PortsideStrings
import portside.model.Sailing
import portside.platformName
import portside.ui.components.AppIcons
import portside.ui.components.MapBackdrop
import portside.ui.components.PhoneMaxWidth

/**
 * The Portside look shared by every host: map/globe backdrop, floating map
 * controls, and the persistent bottom sheet the content lives in. The
 * full-Compose app wraps its own navigation and tab bar around this; the
 * native-chrome iOS app instantiates one shell per SwiftUI destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortsideShell(
    backdropSailing: Sailing?,
    detail: Boolean,
    modifier: Modifier = Modifier,
    /** Non-null shows the floating search button above the map controls. */
    onSearch: (() -> Unit)? = null,
    bottomOverlay: @Composable BoxScope.() -> Unit = {},
    sheetContent: @Composable (innerScrollEnabled: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(PortsideColors.Sea)) {
        val screenHeight = maxHeight
        val peekHeight = if (detail) screenHeight * 0.55f else screenHeight * 0.66f

        // Globe ↔ real map handoff switches instantly: the MapLibre map is
        // a native interop view, and Compose cannot alpha-composite interop
        // layers — crossfading it painted the whole backdrop black on iOS
        // for the duration of the fade. The map's own camera fly-in
        // animation carries the transition instead.
        MapBackdrop(
            sailing = backdropSailing,
            detail = detail,
            mapHeightFraction = if (detail) 0.45f else 0.34f,
        )

        // Everything except the backdrop is phone-designed: on wide hosts
        // (web, tablets, big desktop windows) it lives in a centered
        // phone-width column while the map/globe keeps bleeding edge to edge.
        Box(
            modifier = Modifier
                .widthIn(max = PhoneMaxWidth)
                .fillMaxHeight()
                .align(Alignment.TopCenter),
        ) {

        // The search button and instrument rail float over the map, under the
        // sheet: they must disappear behind it as the sheet expands.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 10.dp),
        ) {
            if (onSearch != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xB31A202A), CircleShape)
                        .clickable(onClick = onSearch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            MapControlsOverlay()
        }

        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            // No Hidden: the sheet is structural chrome and must never be
            // dismissable (the new API's default enabledValues includes it).
            enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
        )
        val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

        // Re-anchor the sheet whenever the destination's peek height changes:
        // closing the detail while the sheet sat at the detail's LOWER peek
        // otherwise leaves it parked at that stale offset on Home/Friends/
        // Logbook (the scaffold doesn't re-position on a peek-height change by
        // itself). A no-op when the sheet is already at the right anchor.
        LaunchedEffect(detail) {
            if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                runCatching { sheetState.partialExpand() }
            }
        }

        // Drag the sheet or scroll its content to grow it over the map,
        // Portside-style; drag down to reveal more of the globe.
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peekHeight,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContainerColor = PortsideColors.SheetBg,
            sheetShadowElevation = 0.dp,
            containerColor = Color.Transparent,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(PortsideColors.Divider, RoundedCornerShape(50)),
                )
            },
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight - 72.dp),
                ) {
                    // Touch platforms: inner scrolling engages only once the
                    // sheet has SETTLED fully expanded — at peek, gestures are
                    // pure sheet drags, and a berth that flips mid-gesture
                    // recomposes the scrollable into the nested-scroll chain
                    // mid-drag (it steals the gesture and makes collapse
                    // flings jump on iOS).
                    // Desktop & Web (pointer/wheel platforms): inner scroll is
                    // always enabled. A wheel scroll on the collapsed sheet is
                    // then offered to the scaffold's nested-scroll connection,
                    // which spends it expanding the sheet first and only scrolls
                    // the detail content once fully expanded — the smooth,
                    // trackpad-native model. (Gating inner scroll on Expanded, as
                    // touch does, left the collapsed web sheet unscrollable until
                    // it was mouse-dragged open.)
                    val innerScrollEnabled = platformName() == "Desktop JVM" ||
                        platformName() == "Web" ||
                        sheetState.currentValue == SheetValue.Expanded
                    sheetContent(innerScrollEnabled)
                }
            },
        ) { /* Map area — the backdrop behind the scaffold shows through. */ }

        bottomOverlay()
        }
    }
}

/**
 * The map's instrument rail: one vertical capsule with hairline separators —
 * chart layers, wind overlay, and a compass for live tracking.
 */
@Composable
fun MapControlsOverlay(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(Color(0xB31A202A), RoundedCornerShape(16.dp)),
    ) {
        MapControlButton(AppIcons.Layers, "Chart layers")
        Box(Modifier.size(width = 16.dp, height = 1.dp).background(Color(0x33FFFFFF)))
        MapControlButton(AppIcons.Wind, "Wind overlay")
        Box(Modifier.size(width = 16.dp, height = 1.dp).background(Color(0x33FFFFFF)))
        MapControlButton(AppIcons.Compass, "Live tracking")
    }
}

@Composable
private fun MapControlButton(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier.size(width = 34.dp, height = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(15.dp),
        )
    }
}

/**
 * The share / alerts / more cluster plus the Add Return pill that floats over
 * the bottom of the sailing-detail sheet, standing in for the tab bar there.
 */
/**
 * One rounded bar carries the whole detail action row: share / alerts / more
 * on the left and the primary "Book Return" button on the right.
 */
@Composable
fun DetailActionBar(sailing: Sailing? = null, modifier: Modifier = Modifier) {
    Surface(
        color = Color(PortsidePalette.Raised),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(PortsidePalette.RaisedStroke)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            DetailActionIcon(AppIcons.Share, "Share")
            DetailActionIcon(AppIcons.BellOff, "Alerts")
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                DetailActionIcon(
                    AppIcons.More,
                    "More",
                    onClick = if (sailing != null) ({ menuOpen = true }) else null,
                )
                sailing?.let {
                    SailingMenuAboveAnchor(
                        sailing = it,
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = PortsideColors.Accent,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = PortsideStrings.BookReturn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PortsideColors.SheetBg,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailActionIcon(icon: ImageVector, description: String, onClick: (() -> Unit)? = null) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = PortsideColors.TextDark,
        modifier = Modifier
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .size(19.dp),
    )
}
