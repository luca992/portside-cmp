package portside.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import portside.data.Settings
import portside.model.DistanceUnit
import portside.model.Sailing
import portside.model.SailingPresentation
import portside.model.SailingStatus
import portside.model.PortsideMetrics
import org.jetbrains.compose.resources.imageResource
import shared.generated.resources.Res
import shared.generated.resources.vessel_photo
import portside.ui.components.LineBadge
import portside.ui.components.AppIcons
import portside.ui.components.BerthChip
import portside.ui.components.CrossingBar
import portside.ui.components.StatusPill
import portside.ui.components.sfSymbolIcon
import portside.ui.components.timeWithNextDay

/**
 * The light sheet content for a sailing — the map above it is drawn by the
 * shared [portside.ui.components.OceanBackdrop].
 */
@Composable
fun SailingDetailScreen(
    sailing: Sailing,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled)
            .padding(horizontal = PortsideMetrics.SheetHorizontalPadding.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            LineBadge(sailing.line, size = 32)
            Text(
                text = "${sailing.number} · ${sailing.dateLabel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = PortsideColors.TextGray,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(PortsideColors.ChipBg, CircleShape)
                    .clickable(onClick = onBack),
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

        Text(
            text = sailing.cityRoute,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortsideColors.TextDark,
            modifier = Modifier.padding(top = 6.dp),
        )

        // Status as a pill + inline countdown rather than a banner strip.
        val banner = SailingPresentation.banner(sailing)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            StatusPill(sailing.status)
            Text(
                text = banner.prefix,
                fontSize = 13.sp,
                color = PortsideColors.TextGray,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                text = banner.value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(banner.tint),
            )
        }

        Spacer(Modifier.height(12.dp))
        CrossingHero(sailing)

        sailing.inboundNote?.let { note ->
            Spacer(Modifier.height(10.dp))
            VesselCard(sailing = sailing, note = note)
        }

        sailing.seaState?.let { sea ->
            Spacer(Modifier.height(10.dp))
            CrossingConditionsCard(sailing, sea)
        }

        if (sailing.goodToKnow.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            GoodToKnowCard(sailing.goodToKnow)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("ticket", "BOOKING CODE", sailing.bookingCode ?: "—", modifier = Modifier.weight(1f))
            InfoTile("cabin", "CABIN", sailing.cabin ?: "—", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        DetailedTimetableCard(sailing)

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Mock data · built with Compose Multiplatform",
            fontSize = 11.sp,
            color = PortsideColors.TextGray,
            // Extra clearance so the floating action bar never covers content.
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 96.dp),
        )
    }
}

/**
 * The hero card: a large crossing bar with a shore block under each end —
 * port code and harbour name, the time (with the original schedule struck
 * through on delays), the status note, and the pier signs (berth / terminal /
 * vehicle deck) as chips.
 */
@Composable
private fun CrossingHero(sailing: Sailing) {
    val timeTint = Color(SailingPresentation.timeTint(sailing))
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            CrossingBar(
                progress = SailingPresentation.crossingProgress(sailing),
                barHeight = 26.dp,
                boatSize = 18.dp,
            )
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 10.dp)) {
                ShoreBlock(
                    code = sailing.origin.code,
                    harbour = sailing.origin.name,
                    time = sailing.departTime,
                    strikethroughTime = sailing.scheduledDepartTime,
                    note = sailing.departNote,
                    tint = timeTint,
                    chips = buildList {
                        add("Berth ${sailing.departBerth}")
                        add("Terminal ${sailing.departTerminal}")
                        sailing.carDeck?.let { add("Deck $it") }
                    },
                    alignEnd = false,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(14.dp))
                ShoreBlock(
                    code = sailing.destination.code,
                    harbour = sailing.destination.name,
                    time = sailing.arriveTime + if (sailing.arrivesNextDay) " +1" else "",
                    strikethroughTime = null,
                    note = sailing.arriveNote,
                    tint = timeTint,
                    chips = buildList {
                        sailing.arriveBerth?.let { add("Berth $it") }
                        add("Terminal ${sailing.arriveTerminal}")
                    },
                    alignEnd = true,
                    modifier = Modifier.weight(1f),
                )
            }
            val unit by Settings.distanceUnit.collectAsState()
            Text(
                text = SailingPresentation.routeSummary(sailing, unit),
                fontSize = 11.sp,
                color = PortsideColors.TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            if (sailing.status == SailingStatus.AtSea) {
                sailing.speedKn?.let { kn ->
                    Text(
                        text = "$kn kn · heading ${sailing.headingDeg}°",
                        fontSize = 11.sp,
                        color = PortsideColors.TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoreBlock(
    code: String,
    harbour: String,
    time: String,
    strikethroughTime: String?,
    note: String,
    tint: Color,
    chips: List<String>,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    val align = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    Column(horizontalAlignment = align, modifier = modifier) {
        Text(
            text = code,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortsideColors.TextDark,
        )
        Text(
            text = harbour,
            fontSize = 11.sp,
            color = PortsideColors.TextGray,
            textAlign = textAlign,
            modifier = Modifier.padding(top = 1.dp),
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            if (alignEnd && strikethroughTime != null) {
                StruckTime(strikethroughTime, endPadded = true)
            }
            Text(
                text = timeWithNextDay(time),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
            if (!alignEnd && strikethroughTime != null) {
                StruckTime(strikethroughTime, endPadded = false)
            }
        }
        val (noteHead, noteTail) = SailingPresentation.noteParts(note)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = tint)) { append(noteHead) }
                if (noteTail != null) {
                    withStyle(SpanStyle(color = PortsideColors.TextGray)) { append(noteTail) }
                }
            },
            fontSize = 11.sp,
            textAlign = textAlign,
            modifier = Modifier.padding(top = 2.dp),
        )
        // Chips reflow as whole units — a chip never breaks mid-word.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            chips.forEach { BerthChip(it) }
        }
    }
}

@Composable
private fun StruckTime(time: String, endPadded: Boolean) {
    Text(
        text = time,
        fontSize = 12.sp,
        color = PortsideColors.TextGray,
        textDecoration = TextDecoration.LineThrough,
        modifier = if (endPadded) {
            Modifier.padding(end = 6.dp, bottom = 3.dp)
        } else {
            Modifier.padding(start = 6.dp, bottom = 3.dp)
        },
    )
}

@Composable
private fun CrossingConditionsCard(sailing: Sailing, sea: portside.model.SeaState) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Crossing Conditions",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
            )
            Text(
                text = "Forecast along the route",
                fontSize = 11.sp,
                color = PortsideColors.TextGray,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                SailingPresentation.seaStats(sea).forEach { stat ->
                    Column {
                        Text(
                            text = stat.label,
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp,
                            color = PortsideColors.TextGray,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            sfSymbolIcon(stat.icon)?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = PortsideColors.Route,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = stat.value,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PortsideColors.TextDark,
                            )
                        }
                    }
                }
            }
            WaveCurve(
                curve = sea.waveCurveM,
                progress = SailingPresentation.crossingProgress(sailing),
                live = sailing.status == SailingStatus.AtSea,
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    text = sailing.departTime,
                    fontSize = 10.sp,
                    color = PortsideColors.TextGray,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "wave height",
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = PortsideColors.TextGray,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = sailing.arriveTime,
                    fontSize = 10.sp,
                    color = PortsideColors.TextGray,
                )
            }
        }
    }
}

/**
 * The wave-height forecast as a smooth champagne curve over the crossing,
 * with a soft fill beneath and, on a live sailing, a marker at the vessel's
 * position along the route.
 */
@Composable
private fun WaveCurve(
    curve: List<Float>,
    progress: Float,
    live: Boolean,
    modifier: Modifier = Modifier,
) {
    val gold = PortsideColors.Route
    val baseline = PortsideColors.Divider
    Canvas(modifier = modifier.fillMaxWidth().height(64.dp)) {
        if (curve.size < 2) return@Canvas
        val maxWave = (curve.max() * 1.25f).coerceAtLeast(0.5f)
        val stepX = size.width / (curve.size - 1)
        fun yAt(i: Int) = size.height - (curve[i] / maxWave) * size.height

        // Smooth the polyline with midpoint quadratics.
        val path = Path()
        path.moveTo(0f, yAt(0))
        for (i in 0 until curve.size - 1) {
            val x0 = i * stepX
            val x1 = (i + 1) * stepX
            path.quadraticTo(x0, yAt(i), (x0 + x1) / 2, (yAt(i) + yAt(i + 1)) / 2)
        }
        path.lineTo(size.width, yAt(curve.size - 1))

        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                listOf(gold.copy(alpha = 0.22f), gold.copy(alpha = 0.02f)),
            ),
        )
        drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        drawPath(path, gold, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        if (live) {
            val px = size.width * progress.coerceIn(0f, 1f)
            val i = (progress * (curve.size - 1)).toInt().coerceIn(0, curve.size - 1)
            drawCircle(PortsideColors.CardBg, 6.dp.toPx(), Offset(px, yAt(i)))
            drawCircle(gold, 3.5.dp.toPx(), Offset(px, yAt(i)))
        }
    }
}

@Composable
private fun VesselCard(sailing: Sailing, note: String) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // A real ship at sea (CC0, Wikimedia Commons), pre-cropped to the
            // card's exact ratio: explicit box + FillBounds sidesteps the
            // desktop resource loader's intrinsic-size bug, and imageResource
            // decodes eagerly so the card never hitches the scroll.
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.9f),
            ) {
                Image(
                    bitmap = imageResource(Res.drawable.vessel_photo),
                    contentDescription = sailing.vessel,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                // Ink scrim so the title band reads over the sky.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xB30A0E14), Color(0x000A0E14)),
                            ),
                        ),
                )
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp)) {
                    Text(
                        text = "Your Vessel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = sailing.vessel +
                            (sailing.vesselInfo?.let { " · ${it.substringBefore(" ·")}" } ?: ""),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "INBOUND STATUS",
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        color = PortsideColors.TextGray,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (sailing.late) {
                            "Departed ${sailing.departNote.substringBefore(" ·").lowercase()}"
                        } else {
                            "Departed on time"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (sailing.late) PortsideColors.RedTime else PortsideColors.GreenTime,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note,
                    fontSize = 12.sp,
                    color = PortsideColors.TextGray,
                )
            }
        }
    }
}

/** Times as a small grid: SCHEDULED and ACTUAL columns per port call. */
@Composable
private fun DetailedTimetableCard(sailing: Sailing) {
    val tint = if (sailing.late) PortsideColors.RedTime else PortsideColors.GreenTime
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Timetable",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
            )
            TimetableGridRow("", "SCHEDULED", "ACTUAL", header = true)
            HorizontalDivider(color = PortsideColors.Divider)
            TimetableGridRow(
                label = "Depart ${sailing.origin.code}",
                scheduled = sailing.scheduledDepartTime ?: sailing.departTime,
                actual = sailing.departTime,
                tint = tint,
            )
            TimetableGridRow(
                label = "Arrive ${sailing.destination.code}",
                scheduled = sailing.arriveTime,
                actual = sailing.arriveTime,
                tint = tint,
            )
        }
    }
}

@Composable
private fun TimetableGridRow(
    label: String,
    scheduled: String,
    actual: String,
    tint: Color = PortsideColors.TextDark,
    header: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = if (header) 9.sp else 13.sp,
            letterSpacing = if (header) 0.8.sp else 0.sp,
            color = if (header) PortsideColors.TextGray else PortsideColors.TextDark,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = scheduled,
            fontSize = if (header) 9.sp else 13.sp,
            letterSpacing = if (header) 0.8.sp else 0.sp,
            color = PortsideColors.TextGray,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = actual,
            fontSize = if (header) 9.sp else 13.sp,
            letterSpacing = if (header) 0.8.sp else 0.sp,
            fontWeight = if (header) FontWeight.Normal else FontWeight.Medium,
            color = if (header) PortsideColors.TextGray else tint,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Practical notes for the day of travel, one card with hairline rows. */
@Composable
private fun GoodToKnowCard(items: List<portside.model.GoodToKnowItem>) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                text = "Before You Board",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
            )
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = PortsideColors.Divider,
                        modifier = Modifier.padding(start = 48.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    val icon = sfSymbolIcon(item.icon)
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = PortsideColors.Route,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PortsideColors.TextDark,
                        )
                        Text(
                            text = item.note,
                            fontSize = 12.sp,
                            color = PortsideColors.TextGray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTile(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                sfSymbolIcon(icon)?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = PortsideColors.TextGray,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    color = PortsideColors.TextGray,
                )
            }
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
