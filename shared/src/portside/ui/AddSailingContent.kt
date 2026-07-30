package portside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import portside.data.AppGraph
import portside.model.Crossing
import portside.model.Line
import portside.ui.components.AppIcons
import portside.ui.components.LineBadge
import portside.ui.components.PhoneMaxWidth
import portside.vm.AddSailingViewModel

/**
 * The full Add Voyage sheet: an M3 modal sheet rendered inside the Compose
 * canvas, used by every host. The native-chrome iOS app also uses this —
 * presenting a UIKit modal over a Compose canvas either snapshots the Metal
 * layer blank (.sheet: white flash) or permanently suspends its rendering
 * (fullScreenCover: white screen after dismiss) on the current CMP beta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSailingSheetHost(onDismiss: () -> Unit) {
    val addSailingViewModel = viewModel { AddSailingViewModel(AppGraph.sailingRepository) }
    val state by addSailingViewModel.uiState.collectAsState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = PhoneMaxWidth,
        // Opens straight to a full-height sheet — no partial stop, so the
        // enabled values skip PartiallyExpanded.
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = PortsideColors.SheetBg,
        // From google issue 467297218 (comment #4): the TOP window inset
        // makes near-full-height sheets oscillate on fast flings — keep
        // only the bottom inset so content clears the home indicator.
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        AddSailingContent(
            crossings = state.crossings,
            lines = state.lines,
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The Add Voyage content, route-first: a From/To picker with a swap control,
 * the well-travelled crossings as tappable rows, and the operators as a chip
 * rail — a departure-board flow, not a search box.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSailingContent(
    crossings: List<Crossing>,
    lines: List<Line>,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Add Voyage",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(PortsideColors.ChipBg, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Close",
                        tint = PortsideColors.TextDark,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // From / To picker with a swap control riding the shared edge.
        Surface(
            color = PortsideColors.CardBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    PortField(label = "FROM", value = "Helsinki · HEL")
                    HorizontalDivider(
                        color = PortsideColors.Divider,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                    PortField(label = "TO", value = "Choose a port")
                }
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(34.dp)
                        .background(PortsideColors.ChipBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.SwapVert,
                        contentDescription = "Swap ports",
                        tint = PortsideColors.Accent,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        Text(
            text = "WELL-TRAVELLED",
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
            color = PortsideColors.TextGray,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            crossings.forEach { crossing -> CrossingRow(crossing) }
        }

        Text(
            text = "LINES",
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
            color = PortsideColors.TextGray,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        // Pills reflow as whole units — a pill never breaks mid-name.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lines.forEach { line -> LinePill(line) }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PortField(label: String, value: String) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
        Text(
            text = label,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            color = PortsideColors.TextGray,
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (value.startsWith("Choose")) PortsideColors.TextGray else PortsideColors.TextDark,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** One well-travelled route: codes joined by a tiny crossing mark, duration chip right. */
@Composable
private fun CrossingRow(crossing: Crossing) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = crossing.origin.code,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PortsideColors.TextDark,
                    )
                    Icon(
                        imageVector = AppIcons.Ship,
                        contentDescription = null,
                        tint = PortsideColors.Accent,
                        modifier = Modifier.padding(horizontal = 8.dp).size(13.dp),
                    )
                    Text(
                        text = crossing.destination.code,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PortsideColors.TextDark,
                    )
                }
                Text(
                    text = "${crossing.origin.city} – ${crossing.destination.city}",
                    fontSize = 11.sp,
                    color = PortsideColors.TextGray,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = crossing.duration + if (crossing.overnight) " · overnight" else "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PortsideColors.TextGray,
                modifier = Modifier
                    .background(PortsideColors.ChipBg, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LinePill(line: Line) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(PortsideColors.ChipBg, RoundedCornerShape(50))
            .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
    ) {
        LineBadge(line, size = 20)
        Text(
            text = line.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PortsideColors.TextDark,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}
