package portside.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import portside.data.Settings
import portside.model.Sailing
import portside.model.SailingPresentation
import portside.platformName
import portside.ui.components.CrossingBar
import portside.ui.components.LineBadge
import portside.ui.components.SailingContextMenu
import portside.ui.components.LocalNativeSailingMenuHost
import portside.ui.components.ScreenHeader
import portside.ui.components.timeWithNextDay
import portside.vm.SailingsUiState

@Composable
fun SailingsScreen(
    state: SailingsUiState,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
    onSailingClick: (Sailing) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "My Voyages",
            profile = state.profile,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        )
        // Scrolling only engages once the sheet is expanded: the per-frame
        // nested-scroll arbitration between an active inner scrollable and the
        // sheet drag is what made sheet gestures stutter on iOS.
        Column(
            modifier = Modifier
                .verticalScroll(scrollState, enabled = scrollEnabled)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            (state.upcoming + state.past).forEach { sailing ->
                SailingRow(sailing = sailing, onClick = { onSailingClick(sailing) })
            }
        }
    }
}

@Composable
fun SailingRow(sailing: Sailing, onClick: () -> Unit) {
    val nativeMenuHost = LocalNativeSailingMenuHost.current
    if (nativeMenuHost != null) {
        // A native host owns the long-press: report where this card sits so its
        // system context-menu interaction can hit-test and snapshot the card.
        DisposableEffect(sailing.id) {
            onDispose { nativeMenuHost.removeRow(sailing.id) }
        }
        Box(
            modifier = Modifier.onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                nativeMenuHost.updateRowBounds(
                    sailing, bounds.left, bounds.top, bounds.right, bounds.bottom,
                )
            },
        ) {
            SailingCard(sailing = sailing, onClick = onClick, onLongClick = {})
        }
    } else {
        var showMenu by remember { mutableStateOf(false) }
        Box {
            SailingContextMenu(sailing = sailing, expanded = showMenu, onDismiss = { showMenu = false })
            SailingCard(sailing = sailing, onClick = onClick, onLongClick = { showMenu = true })
        }
    }
}

/**
 * One sailing as a crossing card: status strip on top, the crossing bar
 * (shore-to-shore water line with the vessel at its live position), and a
 * shore column under each end — port code, city, and time.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SailingCard(sailing: Sailing, onClick: () -> Unit, onLongClick: () -> Unit) {
    // The right-click watcher is desktop-only: it must not sit in the touch
    // gesture path on mobile, where every drag already threads through the
    // sheet drag + list scroll + clickable stack.
    val rightClickModifier = if (platformName() != "Desktop JVM") Modifier
    else Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    onLongClick()
                }
            }
        }
    }
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .then(rightClickModifier)
                .padding(14.dp),
        ) {
            val strip = SailingPresentation.statusStrip(sailing)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strip.label + strip.value,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = Color(strip.tint),
                )
                Spacer(Modifier.weight(1f))
                LineBadge(sailing.line, size = 15)
                Text(
                    text = sailing.number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                    color = PortsideColors.TextGray,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            CrossingBar(
                progress = SailingPresentation.crossingProgress(sailing),
                modifier = Modifier.padding(top = 12.dp),
            )

            val timeTint = Color(SailingPresentation.timeTint(sailing))
            val unit by Settings.distanceUnit.collectAsState()
            Row(modifier = Modifier.padding(top = 6.dp)) {
                ShoreColumn(
                    code = sailing.origin.code,
                    city = sailing.origin.city,
                    time = sailing.departTime,
                    tint = timeTint,
                    alignEnd = false,
                    modifier = Modifier.weight(1f),
                )
                // The crossing's length sits in the open water between shores.
                Text(
                    text = SailingPresentation.formatDistance(sailing.distanceNm, unit),
                    fontSize = 11.sp,
                    color = PortsideColors.TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 8.dp),
                )
                ShoreColumn(
                    code = sailing.destination.code,
                    city = sailing.destination.city,
                    time = sailing.arriveTime + if (sailing.arrivesNextDay) " +1" else "",
                    tint = timeTint,
                    alignEnd = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ShoreColumn(
    code: String,
    city: String,
    time: String,
    tint: Color,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    val align = if (alignEnd) Alignment.End else Alignment.Start
    Column(horizontalAlignment = align, modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (alignEnd) {
                Text(
                    text = city,
                    fontSize = 11.sp,
                    color = PortsideColors.TextGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 6.dp, bottom = 1.dp),
                )
            }
            Text(
                text = code,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
            )
            if (!alignEnd) {
                Text(
                    text = city,
                    fontSize = 11.sp,
                    color = PortsideColors.TextGray,
                    modifier = Modifier.padding(start = 6.dp, bottom = 1.dp),
                )
            }
        }
        Text(
            text = timeWithNextDay(time),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
