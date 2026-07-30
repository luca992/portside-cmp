package portside

import portside.data.MockSailings
import portside.model.SailingStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockSailingsTest {

    @Test
    fun liveSailingHasValidProgress() {
        assertEquals(SailingStatus.AtSea, MockSailings.live.status)
        assertTrue(MockSailings.live.progress in 0f..1f)
        assertTrue(MockSailings.live.dockingIn != null)
    }

    @Test
    fun upcomingAndPastPartitionAllSailings() {
        assertEquals(
            MockSailings.all.size,
            MockSailings.upcoming.size + MockSailings.past.size,
        )
        assertTrue(MockSailings.past.all { it.status == SailingStatus.Docked })
        assertTrue(MockSailings.upcoming.none { it.status == SailingStatus.Docked })
    }

    @Test
    fun delayedSailingsCarryTheirOriginalSchedule() {
        MockSailings.all.filter { it.status == SailingStatus.Delayed }.forEach {
            assertTrue(it.scheduledDepartTime != null, "${it.number} is delayed but has no original time")
        }
    }
}
