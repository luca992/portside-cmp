package portside.model

/**
 * Platform-neutral description of a native menu, so every renderer shows the
 * same items in the same order: SwiftUI builds `Menu`/`.contextMenu` content
 * from it, Compose builds its dropdown from it. Icons are named with SF
 * Symbols as the cross-renderer vocabulary; renderers without SF Symbols
 * map the name to their own icon set.
 */
data class MenuSpec(val sections: List<MenuSection>)

data class MenuSection(val items: List<MenuItem>)

data class MenuItem(
    val title: String,
    val icon: String,
    /** Second line, e.g. the profile item's "Edit Profile". */
    val subtitle: String? = null,
    val destructive: Boolean = false,
    /** Non-empty makes this a submenu parent. */
    val submenu: List<MenuItem> = emptyList(),
)

/** The app's menus, defined once for every renderer. */
object PortsideMenus {
    fun sailing(sailing: Sailing): MenuSpec = MenuSpec(
        listOf(
            MenuSection(
                listOf(
                    MenuItem("Boarding Pass", "qrcode"),
                    MenuItem("Share Voyage", "square.and.arrow.up"),
                ),
            ),
            MenuSection(
                listOf(
                    MenuItem("Route Timetable", "calendar"),
                    MenuItem(
                        title = "Terminal Directions",
                        icon = "map",
                        submenu = listOf(
                            MenuItem("To ${sailing.origin.name}", "map"),
                            MenuItem("To ${sailing.destination.name}", "map"),
                        ),
                    ),
                    MenuItem("Track Vessel", "location"),
                ),
            ),
            MenuSection(
                listOf(
                    MenuItem("Notify Me at Docking", "bell"),
                    MenuItem("Report a Problem", "exclamationmark.bubble"),
                    MenuItem("Remove Voyage", "trash", destructive = true),
                ),
            ),
        ),
    )

    fun profile(profile: Profile): MenuSpec = MenuSpec(
        listOf(
            MenuSection(
                listOf(
                    MenuItem(profile.name, "person.crop.circle", subtitle = "Edit Profile"),
                    MenuItem("Manage Friends", "person.2.badge.gearshape"),
                    MenuItem("Settings", "gearshape"),
                ),
            ),
        ),
    )
}
