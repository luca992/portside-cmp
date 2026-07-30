package portside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import portside.data.Settings
import portside.model.Profile
import portside.model.PortsidePalette
import portside.model.SailingPresentation
import portside.model.PortsideStrings
import portside.model.VoyageStats
import portside.ui.components.AppIcons
import portside.ui.components.SettingsMenuButton

/**
 * The Profile tab: an Instagram-style header — big avatar, name, and a counts
 * row whose Friends column opens the friends list — with the logbook
 * dashboard (teal distance hero, stat tiles, amber month strip) below it.
 */
@Composable
fun ProfileScreen(
    stats: VoyageStats,
    runningOn: String,
    profile: Profile,
    friendCount: Int,
    onOpenFriends: () -> Unit,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled)
            .padding(horizontal = 16.dp),
    ) {
        // No title bar: the avatar block IS the header, with the settings
        // button anchored to the trailing edge of the name row.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        ) {
            BigAvatar(profile)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PortsideColors.TextDark,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsMenuButton(profile)
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    CountColumn("${stats.sailings}", "Voyages", modifier = Modifier.weight(1f))
                    CountColumn(
                        "$friendCount",
                        "Friends",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenFriends),
                    )
                    CountColumn("${stats.ports}", "Ports", modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(PortsideColors.LogTop, PortsideColors.LogBottom)),
                    RoundedCornerShape(18.dp),
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = PortsideStrings.LogbookCardTitle,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = AppIcons.Share,
                    contentDescription = "Share logbook",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp),
                )
            }
            val unit by Settings.distanceUnit.collectAsState()
            Text(
                text = SailingPresentation.formatDistance(stats.distanceNm, unit),
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.Route,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = stats.comparison,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("VOYAGES", "${stats.sailings}", "${stats.overnight} Overnight", Modifier.weight(1f))
            StatTile("TIME AT SEA", stats.timeAtSea, null, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("PORTS", "${stats.ports}", null, Modifier.weight(1f))
            StatTile("LINES", "${stats.lines}", null, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    text = "${stats.monthSailings}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PortsideColors.Route,
                )
                Text(
                    text = stats.monthLabel,
                    fontSize = 13.sp,
                    color = PortsideColors.TextGray,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                Icon(
                    imageVector = AppIcons.Share,
                    contentDescription = null,
                    tint = PortsideColors.TextGray,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = PortsideStrings.AllVoyageStats,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PortsideColors.TextDark,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "›",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PortsideColors.TextGray,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Running on $runningOn",
            fontSize = 11.sp,
            color = PortsideColors.TextGray,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 96.dp),
        )
    }
}

@Composable
private fun BigAvatar(profile: Profile) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(PortsidePalette.AvatarTop), Color(PortsidePalette.AvatarEnd)),
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = profile.initials,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CountColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortsideColors.TextDark,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = PortsideColors.TextGray,
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = PortsideColors.TextGray,
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortsideColors.TextDark,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (sub != null) {
                Text(
                    text = sub,
                    fontSize = 10.sp,
                    color = PortsideColors.TextGray,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

internal fun formatThousands(value: Int): String {
    val s = value.toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, c ->
        sb.append(c)
        val remaining = s.length - 1 - i
        if (remaining > 0 && remaining % 3 == 0) sb.append(',')
    }
    return sb.toString()
}
