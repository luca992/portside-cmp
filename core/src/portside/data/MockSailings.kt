package portside.data

import portside.model.Crossing
import portside.model.Line
import portside.model.Port
import portside.model.SeaState
import portside.model.Sailing
import portside.model.GoodToKnowItem
import portside.model.SailingStatus
import portside.model.Friend
import portside.model.Profile
import portside.model.VoyageStats

// All operators and vessels are fictional; the ports are real Baltic harbours.
object Lines {
    val Aurora = Line("AL", "Aurora Line", 0xFF126E82)
    val Botnia = Line("BF", "Botnia Ferries", 0xFFC96E2F)
    val Vega = Line("VG", "Vega Seaways", 0xFF3B3F8C)
    val Hudson = Line("HF", "Hudson Ferry Co.", 0xFF3E5C94)
    val IslandSound = Line("IS", "Island Sound Line", 0xFF2F7D6D)
    val BayState = Line("BS", "Bay State Ferries", 0xFF9C6A4A)
}

object Ports {
    // Baltic
    val HEL = Port("HEL", "Helsinki", "West Harbour", 60.1454, 24.9147)
    val STO = Port("STO", "Stockholm", "Värtahamnen", 59.3496, 18.1093)
    val TLL = Port("TLL", "Tallinn", "Old City Harbour", 59.4444, 24.7654)
    val TKU = Port("TKU", "Turku", "Linnansatama", 60.4351, 22.2196)
    val MHQ = Port("MHQ", "Mariehamn", "Västra Hamnen", 60.0932, 19.9310)

    // US Northeast
    val WHL = Port("WHL", "Manhattan", "Whitehall Terminal", 40.7013, -74.0134)
    val STG = Port("STG", "Staten Island", "St. George Terminal", 40.6437, -74.0733)
    val WHO = Port("WHO", "Woods Hole", "Steamship Terminal", 41.5236, -70.6720)
    val MVY = Port("MVY", "Vineyard Haven", "MV Terminal", 41.4544, -70.5978)
    val HYA = Port("HYA", "Hyannis", "Ocean Street Dock", 41.6362, -70.2831)
    val ACK = Port("ACK", "Nantucket", "Steamboat Wharf", 41.2854, -70.0964)
    val PTJ = Port("PTJ", "Point Judith", "Galilee State Pier", 41.3712, -71.4903)
    val BID = Port("BID", "Block Island", "Old Harbor Landing", 41.1727, -71.5561)
    val BOS = Port("BOS", "Boston", "Long Wharf", 42.3601, -71.0503)
    val PVC = Port("PVC", "Provincetown", "MacMillan Pier", 42.0517, -70.1811)

    // Indonesia
    val MRK = Port("MRK", "Merak", "Pelabuhan Merak", -5.8933, 106.0086)
    val BKH = Port("BKH", "Bakauheni", "Pelabuhan Bakauheni", -5.8711, 105.7519)
    val KTP = Port("KTP", "Ketapang", "Pelabuhan Ketapang", -8.1417, 114.4003)
    val GLM = Port("GLM", "Gilimanuk", "Pelabuhan Gilimanuk", -8.1631, 114.4326)
}

/**
 * Static mock data — a frozen snapshot of "Sun, 19 Jul": an Aurora Line
 * traveler with the AL 7 overnighter to Stockholm under way and running a few
 * minutes late.
 */
object MockSailings {

    val live = Sailing(
        id = "al7",
        line = Lines.Aurora,
        number = "AL 7",
        origin = Ports.HEL,
        destination = Ports.STO,
        dayLabel = "LIVE",
        dateLabel = "SUN, 19 JUL",
        departTime = "5:47 PM",
        arriveTime = "9:43 AM",
        arrivesNextDay = true,
        scheduledDepartTime = "5:40 PM",
        status = SailingStatus.AtSea,
        progress = 0.62f,
        dockingIn = "2h 31m",
        late = true,
        departNote = "7min Late · 10h 5min ago",
        arriveNote = "9min Late · Docking in 2h 31min",
        departTerminal = "2",
        departBerth = "7",
        arriveTerminal = "V",
        carDeck = "5",
        bookingCode = "AL7HEL",
        cabin = "A 6234",
        vessel = "MS Merituuli",
        vesselInfo = "IMO 9237589 · Maiden voyage Mar 2016 · 10 years old",
        speedKn = 21,
        headingDeg = 262,
        distanceNm = 240,
        duration = "15h 56m",
        seaState = SeaState(
            waveCurveM = listOf(
                0.3f, 0.4f, 0.5f, 0.7f, 0.9f, 1.1f, 1.2f, 1.3f, 1.2f,
                1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.4f,
            ),
            windDir = "SW",
            windKn = 14,
            waterC = 16,
        ),
        inboundNote = "The ship is en route with only very minor delays.",
        goodToKnow = listOf(
            GoodToKnowItem("cloud.sun", "Departure Weather", "64 °F and scattered clouds"),
            GoodToKnowItem("sun.max", "Current STO Weather", "58 °F and clear"),
            GoodToKnowItem("clock.arrow.2.circlepath", "-1 Hour Timezone Change",
                "9:43 AM arrival is 10:43 AM Helsinki time"),
        ),
    )

    val all: List<Sailing> = listOf(
        live,
        Sailing(
            id = "bf205",
            line = Lines.Botnia,
            number = "BF 205",
            origin = Ports.HEL,
            destination = Ports.TLL,
            dayLabel = "TOMORROW · MON, JUL 20",
            dateLabel = "MON, 20 JUL",
            departTime = "9:15 AM",
            arriveTime = "11:32 AM",
            scheduledDepartTime = "8:00 AM",
            status = SailingStatus.Delayed,
            late = true,
            departNote = "1h 15min Late",
            arriveNote = "1h 2min Late",
            departTerminal = "2",
            departBerth = "4",
            arriveTerminal = "D",
            carDeck = "3",
            bookingCode = "BFQW3Z",
            vessel = "MS Viima",
            vesselInfo = "IMO 9781426 · Maiden voyage Jun 2021 · 5 years old",
            distanceNm = 50,
            duration = "2h 17m",
            seaState = SeaState(
                waveCurveM = listOf(0.5f, 0.7f, 0.9f, 1.0f, 0.8f),
                windDir = "W",
                windKn = 18,
                waterC = 15,
            ),
            inboundNote = "Inbound vessel still in Tallinn · departure at risk.",
        ),
        Sailing(
            id = "al8",
            line = Lines.Aurora,
            number = "AL 8",
            origin = Ports.STO,
            destination = Ports.HEL,
            dayLabel = "SAT, JUL 25",
            dateLabel = "SAT, 25 JUL",
            departTime = "4:45 PM",
            arriveTime = "10:10 AM",
            arrivesNextDay = true,
            status = SailingStatus.Scheduled,
            departNote = "On Time",
            arriveNote = "On Time",
            departTerminal = "V",
            departBerth = "2",
            arriveTerminal = "2",
            bookingCode = "AL7HEL",
            cabin = "B 4102",
            vessel = "MS Norrsken",
            vesselInfo = "IMO 9187508 · Maiden voyage Feb 2013 · 13 years old",
            distanceNm = 240,
            duration = "16h 25m",
            seaState = SeaState(
                waveCurveM = listOf(
                    0.3f, 0.3f, 0.4f, 0.6f, 0.8f, 0.9f, 1.0f, 0.9f, 0.8f,
                    0.6f, 0.5f, 0.4f, 0.3f, 0.3f, 0.2f, 0.2f, 0.2f,
                ),
                windDir = "NW",
                windKn = 9,
                waterC = 17,
            ),
            inboundNote = "Vessel assignment expected 24h before departure.",
        ),
        Sailing(
            id = "al11",
            line = Lines.Aurora,
            number = "AL 11",
            origin = Ports.HEL,
            destination = Ports.TLL,
            dayLabel = "TODAY · SUN, JUL 19",
            dateLabel = "SUN, 19 JUL",
            departTime = "10:30 AM",
            arriveTime = "12:33 PM",
            status = SailingStatus.Docked,
            departNote = "On Time · Departed 10:30 AM",
            arriveNote = "Docked 12:26 PM · Berth arrival 12:33 PM",
            departTerminal = "2",
            departBerth = "5",
            arriveTerminal = "D",
            arriveBerth = "9",
            carDeck = "4",
            bookingCode = "AL9TLL",
            cabin = "L 214",
            vessel = "MS Ulappa",
            vesselInfo = "IMO 9143544 · Maiden voyage Aug 2009 · 17 years old",
            distanceNm = 50,
            duration = "2h 3m",
            inboundNote = "Looking good! The vessel arrived with only very minor delays.",
        ),
        Sailing(
            id = "hf21",
            line = Lines.Hudson,
            number = "HF 21",
            origin = Ports.WHL,
            destination = Ports.STG,
            dayLabel = "TODAY · SUN, JUL 19",
            dateLabel = "SUN, 19 JUL",
            departTime = "6:30 PM",
            arriveTime = "6:55 PM",
            status = SailingStatus.Scheduled,
            departNote = "On Time",
            arriveNote = "On Time",
            departTerminal = "W",
            departBerth = "2",
            arriveTerminal = "S",
            bookingCode = "HF21NY",
            vessel = "MS Narrows",
            vesselInfo = "IMO 9673044 · Maiden voyage May 2015 · 11 years old",
            distanceNm = 5,
            duration = "25m",
            seaState = SeaState(
                waveCurveM = listOf(0.2f, 0.3f, 0.3f, 0.2f),
                windDir = "S",
                windKn = 8,
                waterC = 22,
            ),
            inboundNote = "The vessel is shuttling on schedule.",
        ),
        Sailing(
            id = "is8",
            line = Lines.IslandSound,
            number = "IS 8",
            origin = Ports.WHO,
            destination = Ports.MVY,
            dayLabel = "WED, JUL 15",
            dateLabel = "WED, 15 JUL",
            departTime = "9:30 AM",
            arriveTime = "10:15 AM",
            status = SailingStatus.Docked,
            departNote = "On Time · Departed 9:30 AM",
            arriveNote = "Arrived 10:15 AM",
            departTerminal = "1",
            departBerth = "3",
            arriveTerminal = "1",
            arriveBerth = "2",
            carDeck = "2",
            bookingCode = "IS8MVY",
            vessel = "MS Katama",
            distanceNm = 6,
            duration = "45m",
        ),
        Sailing(
            id = "is3",
            line = Lines.IslandSound,
            number = "IS 3",
            origin = Ports.PTJ,
            destination = Ports.BID,
            dayLabel = "TOMORROW · MON, JUL 20",
            dateLabel = "MON, 20 JUL",
            departTime = "11:00 AM",
            arriveTime = "11:55 AM",
            status = SailingStatus.Scheduled,
            departNote = "On Time",
            arriveNote = "On Time",
            departTerminal = "G",
            departBerth = "1",
            arriveTerminal = "O",
            bookingCode = "IS3BID",
            carDeck = "1",
            vessel = "MS Mohegan",
            distanceNm = 9,
            duration = "55m",
            seaState = SeaState(
                waveCurveM = listOf(0.4f, 0.6f, 0.7f, 0.6f, 0.4f),
                windDir = "SW",
                windKn = 12,
                waterC = 20,
            ),
            inboundNote = "The vessel is on its regular island rotation.",
        ),
        Sailing(
            id = "bs9",
            line = Lines.BayState,
            number = "BS 9",
            origin = Ports.BOS,
            destination = Ports.PVC,
            dayLabel = "FRI, JUL 24",
            dateLabel = "FRI, 24 JUL",
            departTime = "2:00 PM",
            arriveTime = "3:30 PM",
            status = SailingStatus.Scheduled,
            departNote = "On Time",
            arriveNote = "On Time",
            departTerminal = "L",
            departBerth = "4",
            arriveTerminal = "M",
            bookingCode = "BS9PVC",
            vessel = "MS Pilgrim",
            distanceNm = 44,
            duration = "1h 30m",
            seaState = SeaState(
                waveCurveM = listOf(0.3f, 0.5f, 0.8f, 0.9f, 0.8f, 0.6f, 0.4f),
                windDir = "E",
                windKn = 11,
                waterC = 18,
            ),
            inboundNote = "Fast ferry — seas outside the harbor can add chop.",
        ),
        Sailing(
            id = "is14",
            line = Lines.IslandSound,
            number = "IS 14",
            origin = Ports.HYA,
            destination = Ports.ACK,
            dayLabel = "SAT, JUL 11",
            dateLabel = "SAT, 11 JUL",
            departTime = "8:15 AM",
            arriveTime = "9:20 AM",
            status = SailingStatus.Docked,
            departNote = "On Time · Departed 8:15 AM",
            arriveNote = "Arrived 9:20 AM",
            departTerminal = "O",
            departBerth = "2",
            arriveTerminal = "S",
            arriveBerth = "1",
            bookingCode = "IS14CK",
            vessel = "MS Sconset",
            distanceNm = 26,
            duration = "1h 5m",
        ),
        Sailing(
            id = "vg306",
            line = Lines.Vega,
            number = "VG 306",
            origin = Ports.TKU,
            destination = Ports.STO,
            dayLabel = "SUN, JUL 12",
            dateLabel = "SUN, 12 JUL",
            departTime = "8:15 AM",
            arriveTime = "7:10 PM",
            status = SailingStatus.Docked,
            departNote = "On Time · Departed 8:15 AM",
            arriveNote = "Arrived 7:10 PM",
            departTerminal = "L",
            departBerth = "1",
            arriveTerminal = "V",
            arriveBerth = "6",
            carDeck = "3",
            vessel = "MS Skärgård",
            distanceNm = 160,
            duration = "10h 55m",
        ),
    )

    val upcoming: List<Sailing> = all.filter { !it.isPast }
    val past: List<Sailing> = all.filter { it.isPast }

    fun byId(id: String): Sailing? = all.firstOrNull { it.id == id }

    val popularCrossings: List<Crossing> = listOf(
        Crossing(Ports.HEL, Ports.STO, "16h", overnight = true),
        Crossing(Ports.HEL, Ports.TLL, "2h", overnight = false),
        Crossing(Ports.TKU, Ports.STO, "11h", overnight = false),
        Crossing(Ports.WHO, Ports.MVY, "45m", overnight = false),
        Crossing(Ports.HYA, Ports.ACK, "1h", overnight = false),
        Crossing(Ports.PTJ, Ports.BID, "55m", overnight = false),
        Crossing(Ports.BOS, Ports.PVC, "1h 30m", overnight = false),
        Crossing(Ports.MRK, Ports.BKH, "2h", overnight = false),
        Crossing(Ports.KTP, Ports.GLM, "45m", overnight = false),
    )

    val allLines: List<Line> = listOf(Lines.Aurora, Lines.Botnia, Lines.Vega, Lines.Hudson, Lines.IslandSound, Lines.BayState)

    val friends: List<Friend> = listOf(
        Friend("Sarah Chen", "SC", 0xFF6B5AE0, "AL 3", "HEL → TLL", SailingStatus.AtSea, "Docking in 42m"),
        Friend("Marcus Webb", "MW", 0xFF0FA96B, "BF 202", "TLL → HEL", SailingStatus.Scheduled, "Departs 6:20 PM"),
        Friend("Priya Patel", "PP", 0xFFE0722F, "AL 5", "HEL → STO", SailingStatus.Docked, "Docked 2:14 PM"),
        Friend("Tom Okafor", "TO", 0xFFD14B8F, "VG 310", "MHQ → TKU", SailingStatus.Delayed, "Now departs 8:05 PM"),
    )

    val profile = Profile(name = "Luca Spinazzola", initials = "LS")

    val stats = VoyageStats(
        sailings = 10,
        overnight = 2,
        distanceNm = 1_374,
        comparison = "6.3x across the Baltic",
        timeAtSea = "2d 14h",
        ports = 14,
        lines = 6,
        monthSailings = 10,
        monthLabel = "voyages in July",
    )
}
