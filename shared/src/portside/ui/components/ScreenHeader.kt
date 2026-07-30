package portside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import portside.data.Settings
import portside.model.DistanceUnit
import portside.model.PortsidePalette
import portside.model.Profile
import portside.ui.PortsideColors

/**
 * The screen-title row: bold title on the left, the settings button (which
 * carries the account menu) on the right.
 */
@Composable
fun ScreenHeader(
    title: String,
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortsideColors.TextDark,
            modifier = Modifier.weight(1f),
        )
        SettingsMenuButton(profile)
    }
}

/**
 * The settings button: a sliders glyph in a soft chip that opens the account
 * menu (or anchors the native host's system menu).
 */
@Composable
fun SettingsMenuButton(profile: Profile, modifier: Modifier = Modifier) {
    val nativeHost = LocalNativeProfileMenuHost.current
    if (nativeHost != null) {
        // A native host owns the account menu: report where the button sits
        // so its invisible system-menu button can cover it.
        DisposableEffect(Unit) {
            onDispose { nativeHost.clearAvatar() }
        }
        SettingsChip(
            modifier = modifier.onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                nativeHost.updateAvatarBounds(
                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                )
            },
        )
        return
    }
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SettingsChip(modifier = Modifier.clickable { menuOpen = true })
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = PortsideColors.MenuGlass,
            // No elevation shadow or tonal tint: the soft dark rim they leave
            // reads as a border around the glass. The translucent fill alone
            // separates the menu from the content beneath it.
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = null,
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = profile.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PortsideColors.TextDark,
                        )
                        Text(
                            text = "Edit Profile",
                            fontSize = 11.sp,
                            color = PortsideColors.TextGray,
                        )
                    }
                },
                leadingIcon = { Avatar(profile, size = 26) },
                onClick = { menuOpen = false },
            )
            HorizontalDivider(color = PortsideColors.Divider)
            DropdownMenuItem(
                text = { MenuLabel("Manage Friends") },
                leadingIcon = { MenuIcon(AppIcons.People) },
                onClick = { menuOpen = false },
            )
            DropdownMenuItem(
                text = { MenuLabel("Settings") },
                leadingIcon = { MenuIcon(AppIcons.Settings) },
                onClick = { menuOpen = false },
            )
            HorizontalDivider(color = PortsideColors.Divider)
            Text(
                text = "DISTANCE",
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium,
                color = PortsideColors.TextGray,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
            )
            val unit by Settings.distanceUnit.collectAsState()
            DistanceUnit.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { MenuLabel(candidate.label) },
                    trailingIcon = {
                        if (candidate == unit) {
                            Text(
                                text = "✓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PortsideColors.Accent,
                            )
                        }
                    },
                    onClick = {
                        Settings.setDistanceUnit(candidate)
                        menuOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuLabel(label: String) {
    Text(
        text = label,
        fontSize = 14.sp,
        color = PortsideColors.TextDark,
    )
}

@Composable
private fun MenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = PortsideColors.TextDark,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun SettingsChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .background(Color(0x1AFFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Settings,
            contentDescription = "Settings",
            tint = PortsideColors.TextDark,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun Avatar(profile: Profile, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(listOf(Color(PortsidePalette.AvatarTop), Color(PortsidePalette.AvatarEnd))),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = profile.initials,
            color = Color.White,
            fontSize = (size * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
