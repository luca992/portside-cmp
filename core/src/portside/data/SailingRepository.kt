package portside.data

import portside.model.Crossing
import portside.model.Line
import portside.model.Sailing
import portside.model.Friend
import portside.model.Profile
import portside.model.VoyageStats

/**
 * The data boundary of the app. ViewModels depend on this interface only, so
 * swapping the mock for a real backend never touches the presentation layer.
 */
interface SailingRepository {
    val liveSailing: Sailing
    fun upcomingSailings(): List<Sailing>
    fun pastSailings(): List<Sailing>
    fun sailingById(id: String): Sailing?
    fun friends(): List<Friend>
    fun voyageStats(): VoyageStats
    fun popularCrossings(): List<Crossing>
    fun lines(): List<Line>
    fun profile(): Profile
}

class MockSailingRepository : SailingRepository {
    override val liveSailing: Sailing = MockSailings.live
    override fun upcomingSailings(): List<Sailing> = MockSailings.upcoming
    override fun pastSailings(): List<Sailing> = MockSailings.past
    override fun sailingById(id: String): Sailing? = MockSailings.byId(id)
    override fun friends(): List<Friend> = MockSailings.friends
    override fun voyageStats(): VoyageStats = MockSailings.stats
    override fun popularCrossings(): List<Crossing> = MockSailings.popularCrossings
    override fun lines(): List<Line> = MockSailings.allLines
    override fun profile(): Profile = MockSailings.profile
}

/**
 * Composition root. Kept as a plain object while the graph is this small;
 * replace with proper DI when the dependency count justifies it.
 */
object AppGraph {
    val sailingRepository: SailingRepository = MockSailingRepository()
}
