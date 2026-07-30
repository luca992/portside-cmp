package portside.model

/**
 * Presentation rules shared by every renderer: the palette, how a status
 * looks, and the little strings that decorate a sailing row. Keeping these in
 * common code is what lets the Compose apps and the all-SwiftUI app show the
 * same thing without either side re-deriving it.
 *
 * Colors are 0xAARRGGBB longs (Compose wraps them in `Color`, SwiftUI in
 * `Color(hex:)`); icons are named with SF Symbols, which each renderer maps
 * onto its own icon set.
 */
object PortsidePalette {
    // Midnight backdrop: ink night sky, silver-blue horizon, champagne route.
    const val Sea = 0xFF0A0E14L
    const val SeaGlow = 0xFF1A2433L
    const val Horizon = 0xFF6B84A8L
    const val Route = 0xFFE3B562L
    const val Shimmer = 0xFFC4D3E8L

    // "Midnight & champagne" sheet: ink-navy surfaces with light ink text
    // and a single warm metallic accent.
    const val SheetBg = 0xFF12161DL
    const val CardBg = 0xFF1B212BL
    const val Divider = 0xFF2A3140L
    /** Floating chrome (dock, action bar): lifted above CardBg with a stroke. */
    const val Raised = 0xFF232B38L
    const val RaisedStroke = 0xFF3A4556L
    const val ChipBg = 0xFF242C39L

    /**
     * Menus float over content with a hint of translucency, like iOS. Kept
     * near-opaque on purpose: at lower alpha the content behind each row
     * shows through differently and reads as per-item backgrounds.
     */
    const val MenuGlass = 0xF01A202AL
    const val TextDark = 0xFFF0F2F6L
    const val TextGray = 0xFF98A2B3L

    // Semantics: emerald = on time, soft coral = late, champagne = brand.
    const val GreenTime = 0xFF53C08CL
    const val RedTime = 0xFFE0736CL
    const val Accent = 0xFFD9B36AL
    const val BerthChip = 0xFF39321FL
    const val BerthChipText = 0xFFE2CB92L
    const val AvatarTop = 0xFF5A6E94L
    const val AvatarEnd = 0xFF39435CL

    // Logbook hero: a barely-there navy gradient; the numerals carry the
    // champagne accent instead of the card carrying a loud fill.
    const val LogTop = 0xFF1D2634L
    const val LogBottom = 0xFF151B25L
}

/** Distance units the rider can pick from the settings menu. */
enum class DistanceUnit(val label: String, val suffix: String) {
    NauticalMiles("Nautical miles", "nm"),
    Kilometres("Kilometres", "km"),
    Miles("Miles", "mi"),
}

/** The badge at the left of a sailing row: caption, glyph, and glyph tint. */
data class StatusStyle(val label: String, val icon: String, val tint: Long)

/** A "Docking in 2h 31m" style fragment: a gray lead-in and a tinted value. */
data class LabeledValue(val label: String, val value: String, val tint: Long)

/** A forecast summary block: gray label, icon, and value. */
data class ForecastStat(val icon: String, val label: String, val value: String)

/** The detail screen's status banner. */
data class BannerStyle(
    val prefix: String,
    val value: String,
    val tint: Long,
    val subtitle: String?,
)

object SailingPresentation {
    /**
     * Row times are tinted by status but the status glyph itself stays
     * neutral — only delays and arrivals get color.
     */
    fun statusStyle(status: SailingStatus): StatusStyle = when (status) {
        SailingStatus.AtSea -> StatusStyle("AT SEA", "ferry", PortsidePalette.TextDark)
        SailingStatus.Delayed -> StatusStyle("DELAYED", "exclamationmark", PortsidePalette.RedTime)
        SailingStatus.Docked -> StatusStyle("ARRIVED", "checkmark", PortsidePalette.GreenTime)
        SailingStatus.Scheduled -> StatusStyle("ON TIME", "ferry", PortsidePalette.TextGray)
    }

    /** Big times and endpoint markers: red while late, green otherwise. */
    fun timeTint(sailing: Sailing): Long =
        if (sailing.late || sailing.status == SailingStatus.Delayed) {
            PortsidePalette.RedTime
        } else {
            PortsidePalette.GreenTime
        }

    /** Tint for the status pill / strip (unlike glyphs, pills always carry color). */
    fun pillTint(status: SailingStatus): Long = when (status) {
        SailingStatus.AtSea -> PortsidePalette.Accent
        SailingStatus.Delayed -> PortsidePalette.RedTime
        SailingStatus.Scheduled -> PortsidePalette.GreenTime
        SailingStatus.Docked -> PortsidePalette.TextGray
    }

    /**
     * The card's top strip: "AT SEA · Docking in " + "2h 31m". Label ends with
     * the separator so renderers just concatenate label + value.
     */
    fun statusStrip(sailing: Sailing): LabeledValue = when (sailing.status) {
        SailingStatus.AtSea -> LabeledValue(
            "AT SEA · DOCKING IN ",
            sailing.dockingIn.orEmpty().uppercase(),
            if (sailing.late) PortsidePalette.RedTime else PortsidePalette.Accent,
        )
        SailingStatus.Delayed -> LabeledValue(
            "DELAYED · NOW DEPARTS ", sailing.departTime.uppercase(), PortsidePalette.RedTime,
        )
        SailingStatus.Scheduled -> LabeledValue(
            "ON TIME · DEPARTS ", sailing.departTime.uppercase(), PortsidePalette.GreenTime,
        )
        SailingStatus.Docked -> LabeledValue(
            "ARRIVED · ", sailing.arriveTime.uppercase(), PortsidePalette.TextGray,
        )
    }

    /** Vessel position along the crossing bar, 0 (origin shore) to 1 (docked). */
    fun crossingProgress(sailing: Sailing): Float = when (sailing.status) {
        SailingStatus.AtSea -> sailing.progress
        SailingStatus.Docked -> 1f
        else -> 0f
    }

    /** The trailing "Docking in 2h 31m" / "Arrived 7:33 PM" fragment. */
    fun rowTrailing(sailing: Sailing): LabeledValue = when (sailing.status) {
        SailingStatus.AtSea -> LabeledValue(
            "Docking in ",
            sailing.dockingIn.orEmpty(),
            if (sailing.late) PortsidePalette.RedTime else PortsidePalette.GreenTime,
        )
        SailingStatus.Docked -> LabeledValue("Arrived ", sailing.arriveTime, PortsidePalette.TextDark)
        SailingStatus.Delayed -> LabeledValue("Now departs ", sailing.departTime, PortsidePalette.RedTime)
        SailingStatus.Scheduled -> LabeledValue("Departs ", sailing.departTime, PortsidePalette.GreenTime)
    }

    /** The detail banner spells the unit out where the row abbreviates it. */
    private fun spelledOut(duration: String): String =
        if (duration.endsWith("m") && !duration.endsWith("min")) duration + "in" else duration

    fun banner(sailing: Sailing): BannerStyle = when (sailing.status) {
        SailingStatus.AtSea -> BannerStyle(
            "Docking in ",
            spelledOut(sailing.dockingIn.orEmpty()),
            if (sailing.late) PortsidePalette.RedTime else PortsidePalette.GreenTime,
            null,
        )
        SailingStatus.Scheduled -> BannerStyle(
            "Departs at ", sailing.departTime, PortsidePalette.GreenTime, null,
        )
        SailingStatus.Delayed -> BannerStyle(
            "Now departs at ", sailing.departTime, PortsidePalette.RedTime, sailing.departNote,
        )
        SailingStatus.Docked -> BannerStyle(
            "Arrived ", sailing.arriveTime, PortsidePalette.GreenTime, sailing.arriveNote,
        )
    }

    /**
     * Only the leading "7min Late" fragment of an endpoint note is tinted;
     * the rest ("4h 16min ago") stays gray.
     */
    fun noteParts(note: String): Pair<String, String?> {
        val i = note.indexOf(" · ")
        return if (i < 0) note to null else note.substring(0, i + 3) to note.substring(i + 3)
    }

    /** Departure/arrival markers: an arrow in a filled circle. */
    fun endpointIcon(departure: Boolean): String =
        if (departure) "arrow.up.right" else "arrow.down.right"

    /** Convert a stored nautical-mile distance into [unit], formatted. */
    fun formatDistance(nm: Int, unit: DistanceUnit): String {
        val value = when (unit) {
            DistanceUnit.NauticalMiles -> nm.toDouble()
            DistanceUnit.Kilometres -> nm * 1.852
            DistanceUnit.Miles -> nm * 1.15078
        }
        return "${groupThousands(kotlin.math.round(value).toInt())} ${unit.suffix}"
    }

    /** "15h 56min · 240 nm · Overnight" — the line between the endpoints. */
    fun routeSummary(sailing: Sailing, unit: DistanceUnit = DistanceUnit.NauticalMiles): String {
        val duration = spelledOut(sailing.duration)
        val tail = if (sailing.arrivesNextDay) "Overnight" else sailing.vessel
        return "$duration · ${formatDistance(sailing.distanceNm, unit)} · $tail"
    }

    fun groupThousands(value: Int): String {
        val digits = value.toString()
        return digits.reversed().chunked(3).joinToString(",").reversed()
    }

    /** The conditions card's summary blocks: waves, wind, and water. */
    fun seaStats(state: SeaState): List<ForecastStat> {
        val peak = state.waveCurveM.maxOrNull() ?: 0f
        val wave = (kotlin.math.round(peak * 10) / 10.0).toString().removeSuffix(".0")
        return listOf(
            ForecastStat("water.waves", "Waves up to", "$wave m"),
            ForecastStat("wind", "Wind", "${state.windDir} ${state.windKn} kn"),
            ForecastStat("thermometer", "Water", "${state.waterC} °C"),
        )
    }
}

/** Layout constants shared by both renderers. */
object PortsideMetrics {
    const val SheetHorizontalPadding = 12
}

/** UI strings, so every app words things identically. */
object PortsideStrings {
    const val MyVoyages = "My Voyages"
    const val Friends = "Friends"
    const val Profile = "Profile"
    const val Search = "Search"
    const val AddVoyage = "Add Voyage"
    const val AddVoyageSubtitle = "Enter line, port, or voyage"
    const val AddVoyagePlaceholder = "Aurora, HEL, or AL7"
    const val FrequentlyUsed = "FREQUENTLY USED"
    const val More = "MORE"
    const val FindByRoute = "Find by Route"
    const val BookReturn = "Book Return"
    const val AddFriend = "+ Add Friend"
    const val Everyone = "Everyone"
    const val Today = "Today"
    const val AllTime = "All-Time"
    const val DetailedTimetable = "DETAILED TIMETABLE"
    const val TimetableSubtitle = "Scheduled, Estimated, Predicted, and Actual"
    const val GoodToKnow = "GOOD TO KNOW"
    const val YourVessel = "Your Vessel"
    const val CrossingConditions = "Crossing Conditions"
    const val AllVoyageStats = "All Voyage Stats"
    const val LogbookCardTitle = "ALL-TIME SEA LOGBOOK"
    const val LogbookCardSubtitle = " LOGBOOK · LOGG · LOKIKIRJA"
    const val BookingCode = "BOOKING CODE"
    const val Cabin = "CABIN"
    const val TapToEdit = "Tap to Edit"
}
