package portside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import portside.data.AppGraph
import portside.model.Sailing
import portside.model.PortsideMenus
import portside.model.MenuSpec
import portside.ui.components.MapBackdrop
import portside.vm.AddSailingUiState
import portside.vm.AddSailingViewModel
import portside.vm.SailingDetailUiState
import portside.vm.SailingDetailViewModel
import portside.vm.SailingsUiState
import portside.vm.SailingsViewModel
import portside.vm.FriendsUiState
import portside.vm.FriendsViewModel
import portside.vm.ProfileUiState
import portside.vm.ProfileViewModel
import platform.UIKit.UIViewController

/**
 * What the all-SwiftUI iOS app uses Kotlin for: the shared domain layer and
 * its ViewModels, the shared menu spec, and the one piece of Compose it keeps
 * — the map/globe backdrop, which has no SwiftUI equivalent (a custom
 * orthographic globe shader plus a MapLibre route map).
 *
 * Every screen, sheet, row, and menu in that app is SwiftUI; nothing here
 * renders app chrome.
 */

// ------------------------------------------------------------- View models

private val repository = AppGraph.sailingRepository
private val sailingsViewModel = SailingsViewModel(repository)
private val friendsViewModel = FriendsViewModel(repository)
private val profileViewModel = ProfileViewModel(repository)
private val addSailingViewModel = AddSailingViewModel(repository)

/**
 * The mock repository is a fixed snapshot, so each ViewModel's StateFlow only
 * ever holds one value — Swift reads it directly instead of subscribing.
 */
@Suppress("unused")
fun sailingsState(): SailingsUiState = sailingsViewModel.uiState.value

@Suppress("unused")
fun friendsState(): FriendsUiState = friendsViewModel.uiState.value

@Suppress("unused")
fun profileState(): ProfileUiState = profileViewModel.uiState.value

@Suppress("unused")
fun addSailingState(): AddSailingUiState = addSailingViewModel.uiState.value

@Suppress("unused")
fun detailState(sailingId: String): SailingDetailUiState =
    SailingDetailViewModel(repository, sailingId).uiState.value

@Suppress("unused")
fun liveSailing(): Sailing = repository.liveSailing

// ------------------------------------------------------------- Menu content

/** Both menus come from the spec every platform's renderer reads. */
@Suppress("unused")
fun sailingMenuSpec(sailingId: String): MenuSpec? =
    repository.sailingById(sailingId)?.let { PortsideMenus.sailing(it) }

@Suppress("unused")
fun profileMenuSpec(): MenuSpec = PortsideMenus.profile(repository.profile())

// ---------------------------------------------------------------- Backdrop

/**
 * Drives the backdrop from SwiftUI: which sailing's route to draw, and whether
 * to show the detail map (MapLibre) or the globe.
 */
class BackdropController {
    internal val sailing = mutableStateOf<Sailing?>(null)
    internal val detail = mutableStateOf(false)

    fun show(sailingId: String?, detail: Boolean) {
        this.sailing.value = sailingId?.let { repository.sailingById(it) }
        this.detail.value = detail
    }
}

/**
 * The one Compose surface the SwiftUI app hosts: space, globe, route arc, and
 * — on the detail screen — the real vector map. SwiftUI layers all of its own
 * chrome on top of this.
 */
@Suppress("unused", "FunctionName")
fun BackdropView(controller: BackdropController): UIViewController = ComposeUIViewController {
    PortsideTheme {
        val detail = controller.detail.value
        val sailing = controller.sailing.value ?: repository.liveSailing
        Box(modifier = Modifier.fillMaxSize().background(PortsideColors.Sea)) {
            MapBackdrop(
                sailing = sailing,
                detail = detail,
                mapHeightFraction = if (detail) 0.45f else 0.34f,
            )
        }
    }
}
