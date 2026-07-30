package portside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import portside.model.Friend
import portside.model.Profile
import portside.ui.components.AppIcons
import portside.ui.components.ScreenHeader
import portside.ui.components.StatusPill

@Composable
fun FriendsScreen(
    friends: List<Friend>,
    profile: Profile,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
    /** Non-null when pushed from the Profile tab: shows a back chip instead
     *  of the tab-style header. */
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 20.dp, top = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(PortsideColors.ChipBg, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = "Back",
                        tint = PortsideColors.TextDark,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "Friends",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PortsideColors.TextDark,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        } else {
            ScreenHeader(
                title = "Friends",
                profile = profile,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) {
            SegmentLabel("Everyone", selected = true)
            SegmentLabel("Today", selected = false)
            Spacer(Modifier.weight(1f))
            Text(
                text = "+ Add Friend",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PortsideColors.Accent,
            )
        }
        Column(
            modifier = Modifier
                .verticalScroll(scrollState, enabled = scrollEnabled)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            friends.forEach { friend ->
                FriendCard(friend)
            }
        }
    }
}

/** An underline segment: quiet text with a champagne bar under the active one. */
@Composable
private fun SegmentLabel(label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) PortsideColors.TextDark else PortsideColors.TextGray,
        )
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(width = 18.dp, height = 2.dp)
                .background(
                    if (selected) PortsideColors.Accent else Color.Transparent,
                    RoundedCornerShape(1.dp),
                ),
        )
    }
}

@Composable
private fun FriendCard(friend: Friend) {
    Surface(color = PortsideColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(friend.color), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.initials,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = friend.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PortsideColors.TextDark,
                )
                Text(
                    text = "${friend.route} · ${friend.sailingLabel}",
                    fontSize = 12.sp,
                    color = PortsideColors.TextGray,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(friend.status)
                Text(
                    text = friend.statusNote,
                    fontSize = 11.sp,
                    color = PortsideColors.TextGray,
                )
            }
        }
    }
}
