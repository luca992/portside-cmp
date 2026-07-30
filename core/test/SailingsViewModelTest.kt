package portside

import portside.data.MockSailingRepository
import portside.vm.SailingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SailingsViewModelTest {

    @Test
    fun exposesUpcomingAndPastSailings() {
        val repository = MockSailingRepository()
        val viewModel = SailingsViewModel(repository)

        val state = viewModel.uiState.value
        assertEquals(repository.upcomingSailings(), state.upcoming)
        assertEquals(repository.pastSailings(), state.past)
        assertEquals(repository.profile(), state.profile)
        assertTrue(state.upcoming.none { it.isPast })
        assertTrue(state.past.all { it.isPast })
    }
}
