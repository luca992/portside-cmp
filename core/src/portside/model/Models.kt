package portside.model

data class Line(
    val code: String,
    val name: String,
    /** Brand color as 0xAARRGGBB. */
    val color: Long,
)

data class Port(
    val code: String,
    val city: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

enum class SailingStatus {
    Scheduled,
    Delayed,
    AtSea,
    Docked,
}

data class Sailing(
    val id: String,
    val line: Line,
    val number: String,
    val origin: Port,
    val destination: Port,
    /** Section header the sailing is grouped under, e.g. "TODAY · MON, JUL 20". */
    val dayLabel: String,
    /** Short date shown in card/detail headers, e.g. "MON, 20 JUL". */
    val dateLabel: String,
    val departTime: String,
    val arriveTime: String,
    val arrivesNextDay: Boolean = false,
    /** Original schedule, only set when [status] is [SailingStatus.Delayed]. */
    val scheduledDepartTime: String? = null,
    val status: SailingStatus,
    /** 0f..1f, only meaningful when [status] is [SailingStatus.AtSea]. */
    val progress: Float = 0f,
    val dockingIn: String? = null,
    /** Running late — endpoint times render red even while under way. */
    val late: Boolean = false,
    /** Small line under the departure time, e.g. "On Time · Departed 7:05 AM". */
    val departNote: String,
    /** Small line under the arrival time, e.g. "Docking in 1h 58m". */
    val arriveNote: String,
    val departTerminal: String,
    val departBerth: String,
    val arriveTerminal: String,
    val arriveBerth: String? = null,
    /** Vehicle deck the car is parked on, when the booking includes one. */
    val carDeck: String? = null,
    val bookingCode: String? = null,
    val cabin: String? = null,
    val vessel: String,
    /** Registry and age line, e.g. "IMO 9354284 · Maiden voyage Aug 2009". */
    val vesselInfo: String? = null,
    val speedKn: Int? = null,
    /** Compass heading while under way, degrees true. */
    val headingDeg: Int? = null,
    val distanceNm: Int,
    val duration: String,
    /** Sea conditions forecast along the route, when available. */
    val seaState: SeaState? = null,
    /** "Where's my ship?" — status of the inbound vessel. */
    val inboundNote: String? = null,
    /** The detail screen's "Good to Know" list: weather, boarding, port notes. */
    val goodToKnow: List<GoodToKnowItem> = emptyList(),
) {
    val isPast: Boolean get() = status == SailingStatus.Docked
    val routeLabel: String get() = "${origin.code} → ${destination.code}"
    val cityRoute: String get() = "${origin.city} to ${destination.city}"
}

/** A row in the detail screen's "Good to Know" section (icon + title + note). */
data class GoodToKnowItem(
    /** SF Symbol name for the leading glyph. */
    val icon: String,
    val title: String,
    val note: String,
)

/** Sea conditions forecast along a crossing. */
data class SeaState(
    /** Wave height sampled at even intervals from departure to arrival, metres. */
    val waveCurveM: List<Float>,
    /** Prevailing wind: compass direction and speed in knots. */
    val windDir: String,
    val windKn: Int,
    /** Surface water temperature in °C. */
    val waterC: Int,
)

data class Friend(
    val name: String,
    val initials: String,
    /** Avatar color as 0xAARRGGBB. */
    val color: Long,
    val sailingLabel: String,
    val route: String,
    val status: SailingStatus,
    val statusNote: String,
)

/** The signed-in traveler shown in screen headers and the avatar menu. */
data class Profile(
    val name: String,
    val initials: String,
)

/** A well-travelled route offered on the Add Voyage sheet. */
data class Crossing(
    val origin: Port,
    val destination: Port,
    val duration: String,
    val overnight: Boolean,
)

data class VoyageStats(
    val sailings: Int,
    val overnight: Int,
    val distanceNm: Int,
    /** e.g. "8x the length of the Baltic" */
    val comparison: String,
    /** e.g. "4d 17h" */
    val timeAtSea: String,
    val ports: Int,
    val lines: Int,
    val monthSailings: Int,
    val monthLabel: String,
)
