package portside

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import portside.data.AppGraph
import portside.ui.DetailActionBar
import portside.ui.SailingDetailScreen
import portside.ui.SailingsScreen
import portside.ui.PortsideColors
import portside.ui.PortsideShell
import portside.vm.AppViewModel
import portside.vm.SailingDetailViewModel
import portside.vm.SailingsViewModel

/**
 * The My Voyages experience: one
 * persistent map/sheet shell, with Home → Detail navigation happening inside
 * the sheet while the backdrop map updates behind it. Used by hosts that own
 * their own tab chrome (the native-chrome iOS app); the full-Compose [App]
 * inlines the same structure with its tab switcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SailingsFlow(
    modifier: Modifier = Modifier,
    /**
     * Lets a native host react to in-sheet navigation with the open sailing's
     * id (null when back on the list) — e.g. hide its tab bar and show its
     * own detail action bar. When provided, the Compose action bar is not
     * drawn; the host owns that chrome.
     */
    onDetailShown: ((String?) -> Unit)? = null,
) {
    val appViewModel = viewModel { AppViewModel(AppGraph.sailingRepository) }
    val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }

    val detailSailing = (backStack.lastOrNull() as? AppScreen.SailingDetail)
        ?.let { appViewModel.sailingById(it.sailingId) }

    if (onDetailShown != null) {
        LaunchedEffect(detailSailing?.id) {
            onDetailShown(detailSailing?.id)
        }
    }

    val sailingsScrollState = rememberScrollState()
    val detailScrollState = rememberScrollState()

    PortsideShell(
        backdropSailing = detailSailing ?: appViewModel.liveSailing,
        detail = detailSailing != null,
        modifier = modifier,
        bottomOverlay = {
            if (detailSailing != null && onDetailShown == null) {
                DetailActionBar(
                    sailing = detailSailing,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { innerScrollEnabled ->
        NavDisplay(
            backStack = backStack,
            // Opaque backing + alpha-free slides: see App.kt — fading layers
            // composite against the black Metal backing on iOS.
            modifier = Modifier.background(PortsideColors.SheetBg),
            transitionSpec = {
                slideInHorizontally(tween(400)) { it } togetherWith
                    slideOutHorizontally(tween(400)) { -it / 3 }
            },
            popTransitionSpec = {
                slideInHorizontally(tween(400)) { -it / 3 } togetherWith
                    slideOutHorizontally(tween(400)) { it }
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(tween(400)) { -it / 3 } togetherWith
                    slideOutHorizontally(tween(400)) { it }
            },
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<AppScreen.Home> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PortsideColors.SheetBg),
                    ) {
                        val sailingsViewModel =
                            viewModel { SailingsViewModel(AppGraph.sailingRepository) }
                        val state by sailingsViewModel.uiState.collectAsState()
                        SailingsScreen(
                            state = state,
                            scrollState = sailingsScrollState,
                            scrollEnabled = innerScrollEnabled,
                            onSailingClick = {
                                backStack.add(AppScreen.SailingDetail(it.id))
                            },
                        )
                    }
                }
                entry<AppScreen.SailingDetail> { key ->
                    val detailViewModel = viewModel(key = "detail-${key.sailingId}") {
                        SailingDetailViewModel(AppGraph.sailingRepository, key.sailingId)
                    }
                    val state by detailViewModel.uiState.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PortsideColors.SheetBg),
                    ) {
                        state.sailing?.let { sailing ->
                            SailingDetailScreen(
                                sailing = sailing,
                                scrollState = detailScrollState,
                                scrollEnabled = innerScrollEnabled,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                    }
                }
            },
        )
    }
}
