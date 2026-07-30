package portside.vm

import androidx.lifecycle.ViewModel
import portside.data.SailingRepository
import portside.model.Crossing
import portside.model.Line
import portside.model.Sailing
import portside.model.Friend
import portside.model.Profile
import portside.model.VoyageStats
import portside.platformName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Each screen has one ViewModel exposing a single immutable [StateFlow] of its
 * UiState (state down) and plain functions for events (events up). Screens in
 * the UI module receive the UiState and callbacks — never the ViewModel.
 */

/** App-level data the scaffolding needs: the backdrop route and id lookups. */
class AppViewModel(private val repository: SailingRepository) : ViewModel() {
    val liveSailing: Sailing = repository.liveSailing
    fun sailingById(id: String): Sailing? = repository.sailingById(id)
}

data class SailingsUiState(
    val upcoming: List<Sailing>,
    val past: List<Sailing>,
    val profile: Profile,
)

class SailingsViewModel(repository: SailingRepository) : ViewModel() {
    val uiState: StateFlow<SailingsUiState> = MutableStateFlow(
        SailingsUiState(
            upcoming = repository.upcomingSailings(),
            past = repository.pastSailings(),
            profile = repository.profile(),
        ),
    )
}

data class SailingDetailUiState(val sailing: Sailing?)

class SailingDetailViewModel(
    repository: SailingRepository,
    sailingId: String,
) : ViewModel() {
    val uiState: StateFlow<SailingDetailUiState> =
        MutableStateFlow(SailingDetailUiState(repository.sailingById(sailingId)))
}

data class FriendsUiState(val friends: List<Friend>, val profile: Profile)

class FriendsViewModel(repository: SailingRepository) : ViewModel() {
    val uiState: StateFlow<FriendsUiState> =
        MutableStateFlow(FriendsUiState(repository.friends(), repository.profile()))
}

data class ProfileUiState(
    val stats: VoyageStats,
    val runningOn: String,
    val profile: Profile,
    val friendCount: Int,
)

class ProfileViewModel(repository: SailingRepository) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = MutableStateFlow(
        ProfileUiState(
            repository.voyageStats(),
            platformName(),
            repository.profile(),
            repository.friends().size,
        ),
    )
}

data class AddSailingUiState(
    val crossings: List<Crossing>,
    val lines: List<Line>,
)

class AddSailingViewModel(repository: SailingRepository) : ViewModel() {
    val uiState: StateFlow<AddSailingUiState> = MutableStateFlow(
        AddSailingUiState(
            crossings = repository.popularCrossings(),
            lines = repository.lines(),
        ),
    )
}
