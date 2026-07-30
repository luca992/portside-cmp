package portside

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.foundation.BorderStroke
import portside.data.AppGraph
import portside.model.PortsidePalette
import portside.ui.AddSailingSheetHost
import portside.ui.DetailActionBar
import portside.ui.SailingDetailScreen
import portside.ui.SailingsScreen
import portside.ui.PortsideColors
import portside.ui.PortsideShell
import portside.ui.PortsideTheme
import portside.ui.FriendsScreen
import portside.ui.ProfileScreen
import portside.ui.components.AppIcons
import portside.vm.AddSailingViewModel
import portside.vm.AppViewModel
import portside.vm.SailingDetailViewModel
import portside.vm.SailingsViewModel
import portside.vm.FriendsViewModel
import portside.vm.ProfileViewModel

private enum class Tab(val title: String) {
    Sailings("My Voyages"),
    Profile("Profile"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    PortsideTheme {
    // Hard edges everywhere: no rubber-band on list ends or on the sheet's own
    // min/max bounds (the band's spring-back re-rasterizes the whole sheet per
    // frame on iOS and reads as jerky).
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        val appViewModel = viewModel { AppViewModel(AppGraph.sailingRepository) }
        val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }
        var tab by remember { mutableStateOf(Tab.Sailings) }
        var showAddSailing by remember { mutableStateOf(false) }

        val detailSailing = (backStack.lastOrNull() as? AppScreen.SailingDetail)
            ?.let { appViewModel.sailingById(it.sailingId) }

        // Scroll positions are hoisted so the shell's desktop wheel handler can
        // tell whether the visible content sits at its top. Also keeps each
        // tab's scroll position across tab switches.
        val sailingsScrollState = rememberScrollState()
        val friendsScrollState = rememberScrollState()
        val profileScrollState = rememberScrollState()
        val detailScrollState = rememberScrollState()

        PortsideShell(
            backdropSailing = detailSailing
                ?: if (tab == Tab.Sailings) appViewModel.liveSailing else null,
            detail = detailSailing != null,
            onSearch = { showAddSailing = true },
            bottomOverlay = {
                if (detailSailing == null) {
                    PortsideTabBar(
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 10.dp),
                    )
                } else {
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
                // Opaque backing: nav transitions render entries into
                // graphics layers, and any transparent region shows
                // the black Metal backing layer on iOS.
                modifier = Modifier.background(PortsideColors.SheetBg),
                // Alpha-free slides instead of the default 700ms
                // cross-fade: on iOS the fading layers composite
                // against the black Metal backing and the whole
                // sheet flashes dark during navigation.
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
                    // Each entry gets an opaque sheet-colored panel:
                    // the screens themselves have no background, so
                    // without it the slide layers are transparent
                    // and the outgoing screen shows through.
                    entry<AppScreen.Home> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PortsideColors.SheetBg),
                        ) {
                            when (tab) {
                                Tab.Sailings -> {
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
                                Tab.Profile -> {
                                    val profileViewModel =
                                        viewModel { ProfileViewModel(AppGraph.sailingRepository) }
                                    val state by profileViewModel.uiState.collectAsState()
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

        if (showAddSailing) {
            AddSailingSheetHost(onDismiss = { showAddSailing = false })
        }
    }
    }
}

/**
 * A floating dock: one dark rounded card where the selected tab expands into
 * an accent pill carrying its label and the others collapse to bare glyphs;
 * Search rides along behind a hairline divider instead of floating separately.
 */
@Composable
private fun PortsideTabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(PortsidePalette.Raised),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(PortsidePalette.RaisedStroke)),
        shadowElevation = 10.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Tab.entries.forEach { entry ->
                DockItem(
                    icon = when (entry) {
                        Tab.Sailings -> AppIcons.Ship
                        Tab.Profile -> AppIcons.Person
                    },
                    title = entry.title,
                    selected = selected == entry,
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) PortsideColors.Accent else PortsideColors.TextGray
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) PortsideColors.Accent.copy(alpha = 0.14f) else Color.Transparent,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
        if (selected) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}
