package portside

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import portside.data.AppGraph
import portside.ui.FriendsScreen
import portside.ui.PortsideColors
import portside.ui.PortsideShell
import portside.ui.ProfileScreen
import portside.vm.FriendsViewModel
import portside.vm.ProfileViewModel

/**
 * The Profile experience for hosts that own their own tab chrome (the
 * native-chrome iOS app): one persistent shell over the idle globe, with the
 * profile header + logbook dashboard and the friends list navigating inside
 * the sheet. The full-Compose [App] inlines the same structure into its own
 * back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFlow(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }
    val profileScrollState = rememberScrollState()
    val friendsScrollState = rememberScrollState()

    PortsideShell(
        backdropSailing = null,
        detail = false,
        modifier = modifier,
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
                    val profileViewModel =
                        viewModel { ProfileViewModel(AppGraph.sailingRepository) }
                    val state by profileViewModel.uiState.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PortsideColors.SheetBg),
                    ) {
                        ProfileScreen(
                            stats = state.stats,
                            runningOn = state.runningOn,
                            profile = state.profile,
                            friendCount = state.friendCount,
                            onOpenFriends = { backStack.add(AppScreen.Friends) },
                            scrollState = profileScrollState,
                            scrollEnabled = innerScrollEnabled,
                        )
                    }
                }
                entry<AppScreen.Friends> {
                    val friendsViewModel =
                        viewModel { FriendsViewModel(AppGraph.sailingRepository) }
                    val state by friendsViewModel.uiState.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PortsideColors.SheetBg),
                    ) {
                        FriendsScreen(
                            friends = state.friends,
                            profile = state.profile,
                            scrollState = friendsScrollState,
                            scrollEnabled = innerScrollEnabled,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                }
            },
        )
    }
}
