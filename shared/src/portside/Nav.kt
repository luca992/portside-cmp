package portside

/** Navigation 3 back-stack keys. */
sealed interface AppScreen {
    data object Home : AppScreen
    data class SailingDetail(val sailingId: String) : AppScreen
    /** Friends list, pushed from the Profile tab's friend count. */
    data object Friends : AppScreen
}
