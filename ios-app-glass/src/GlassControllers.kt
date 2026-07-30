import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.ComposeUIViewController
import portside.App
import portside.ProfileFlow
import portside.SailingsFlow
import portside.data.AppGraph
import portside.model.Sailing
import portside.model.PortsideMenus
import portside.model.MenuSpec
import portside.ui.AddSailingSheetHost
import portside.ui.PortsideColors
import portside.ui.PortsideTheme
import portside.ui.components.LocalNativeSailingMenuHost
import portside.ui.components.LocalNativeProfileMenuHost
import portside.ui.components.NativeSailingMenuHost
import portside.ui.components.NativeProfileMenuHost
import platform.UIKit.UIViewController

/**
 * Entry points for the native-chrome (Liquid Glass) app: SwiftUI owns the tab
 * bar, search sheet, and long-press context menus; every screen — including
 * the in-sheet Home → Detail navigation, which behaves exactly like the
 * shared design (the map just updates behind the persistent sheet) — is the
 * same shared Compose code the full-Compose apps use.
 * Pattern per https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html
 */
/**
 * The Add Sailing sheet is presented INSIDE the Compose canvas (an M3 modal
 * sheet), not as a UIKit modal: on the current CMP beta a UIKit presentation
 * over a Compose canvas either snapshots the Metal layer blank (.sheet) or
 * permanently suspends its rendering (fullScreenCover). Swift triggers it via
 * [presentAddSailing]; every tab hosts it, so it appears over the current tab.
 */
private val addSailingVisible = mutableStateOf(false)
private var addSailingListener: ((Boolean) -> Unit)? = null

private fun setAddSailingVisible(visible: Boolean) {
    addSailingVisible.value = visible
    addSailingListener?.invoke(visible)
}

@Suppress("unused")
fun presentAddSailing() = setAddSailingVisible(true)

/** Swift registers to hide its tab bar while the sheet is up. */
@Suppress("unused")
fun setAddSailingVisibilityListener(listener: (Boolean) -> Unit) {
    addSailingListener = listener
}

private fun glassController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController {
        PortsideTheme {
            // Same hard-edge scroll policy as the full-Compose app.
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                content()
                if (addSailingVisible.value) {
                    AddSailingSheetHost(onDismiss = { setAddSailingVisible(false) })
                }
            }
        }
    }

/**
 * Row-bounds registry backing the native context menu: Compose reports where
 * each sailing row sits (window px), Swift hit-tests long-presses against it
 * and snapshots the row for the menu preview. Coordinates stored in points.
 */
private object GlassSailingMenu : NativeSailingMenuHost {
    private val rows = LinkedHashMap<String, DoubleArray>()
    var density: Float = 1f

    override fun updateRowBounds(
        sailing: Sailing,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        val d = density
        rows[sailing.id] = doubleArrayOf(
            (left / d).toDouble(),
            (top / d).toDouble(),
            (right / d).toDouble(),
            (bottom / d).toDouble(),
        )
    }

    override fun removeRow(sailingId: String) {
        rows.remove(sailingId)
    }

    fun sailingIdAt(x: Double, y: Double): String? = rows.entries.firstOrNull { (_, r) ->
        x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]
    }?.key

    /** [x, y, width, height] in points, or null if the row is gone. */
    fun rowRect(sailingId: String): DoubleArray? = rows[sailingId]?.let { r ->
        doubleArrayOf(r[0], r[1], r[2] - r[0], r[3] - r[1])
    }
}

/**
 * Bridges the profile avatar's on-screen position to Swift, which parks an
 * invisible system-menu button over it. One anchor per tab controller.
 */
class ProfileMenuAnchor : NativeProfileMenuHost {
    var listener: ((Double, Double, Double, Double) -> Unit)? = null
    internal var density: Float = 1f

    override fun updateAvatarBounds(left: Float, top: Float, right: Float, bottom: Float) {
        val d = density
        listener?.invoke(
            (left / d).toDouble(),
            (top / d).toDouble(),
            ((right - left) / d).toDouble(),
            ((bottom - top) / d).toDouble(),
        )
    }

    override fun clearAvatar() {
        listener?.invoke(0.0, 0.0, 0.0, 0.0)
    }
}

@Suppress("unused")
fun profileDisplayName(): String = AppGraph.sailingRepository.profile().name

// The shared menu specs, so the UIKit menus render the exact same items,
// icons, and submenus as every other platform's renderer.
@Suppress("unused")
fun sailingMenuSpec(sailingId: String): MenuSpec? =
    AppGraph.sailingRepository.sailingById(sailingId)?.let { PortsideMenus.sailing(it) }

@Suppress("unused")
fun profileMenuSpec(): MenuSpec = PortsideMenus.profile(AppGraph.sailingRepository.profile())

@Suppress("unused", "FunctionName")
fun SailingsTabController(
    onDetailShown: (String?) -> Unit,
    profileAnchor: ProfileMenuAnchor,
): UIViewController = glassController {
    GlassSailingMenu.density = LocalDensity.current.density
    profileAnchor.density = LocalDensity.current.density
    CompositionLocalProvider(
        LocalNativeSailingMenuHost provides GlassSailingMenu,
        LocalNativeProfileMenuHost provides profileAnchor,
    ) {
        SailingsFlow(onDetailShown = onDetailShown)
    }
}

@Suppress("unused")
fun sailingMenuSailingIdAt(x: Double, y: Double): String? = GlassSailingMenu.sailingIdAt(x, y)

@Suppress("unused")
fun sailingMenuRowRect(sailingId: String): DoubleArray? = GlassSailingMenu.rowRect(sailingId)

/** Origin and destination port codes for the menu's Open in Maps submenu. */
@Suppress("unused")
fun sailingMenuPorts(sailingId: String): List<String> =
    AppGraph.sailingRepository.sailingById(sailingId)
        ?.let { listOf(it.origin.code, it.destination.code) }
        ?: emptyList()

/**
 * The Profile tab: Instagram-style header + logbook dashboard, with the
 * friends list sliding in when the friend count is tapped — the same
 * in-sheet navigation the full-Compose app uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused", "FunctionName")
fun ProfileTabController(profileAnchor: ProfileMenuAnchor): UIViewController = glassController {
    profileAnchor.density = LocalDensity.current.density
    CompositionLocalProvider(LocalNativeProfileMenuHost provides profileAnchor) {
        ProfileFlow()
    }
}

/** Full-Compose fallback for iOS versions without Liquid Glass (< 26). */
@Suppress("unused", "FunctionName")
fun FullComposeController(): UIViewController = ComposeUIViewController { App() }
