import SwiftUI
import KotlinModules

/// The sailing detail sheet, in SwiftUI: status pill, the crossing hero card,
/// Where's My Ship, on-time performance, Good to Know, booking tiles, and the
/// timetable — mirroring the Compose apps section for section.
struct SailingDetailView: View {
    let sailing: Sailing
    let onClose: () -> Void
    @AppStorage("distanceUnit") private var distanceUnit = "nm"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                Text(sailing.cityRoute)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(Portside.textDark)
                    .padding(.top, 6)
                statusRow.padding(.top, 8)

                crossingHero.padding(.top, 12)

                if let note = sailing.inboundNote {
                    VesselCard(sailing: sailing, note: note).padding(.top, 10)
                }
                if let sea = sailing.seaState {
                    CrossingConditionsCard(sailing: sailing, sea: sea).padding(.top, 10)
                }
                if !sailing.goodToKnow.isEmpty {
                    GoodToKnowCard(items: sailing.goodToKnow).padding(.top, 10)
                }

                HStack(spacing: 10) {
                    InfoTile(icon: "doc.text", title: "BOOKING CODE", value: sailing.bookingCode ?? "—")
                    InfoTile(icon: "chair", title: "CABIN", value: sailing.cabin ?? "—")
                }
                .padding(.top, 10)

                DetailedTimetableCard(sailing: sailing).padding(.top, 10)

                Text("Mock data · Kotlin Multiplatform + SwiftUI")
                    .font(.system(size: 11))
                    .foregroundStyle(Portside.textGray)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 16)
                    .padding(.bottom, 96)
            }
            .padding(.horizontal, CGFloat(PortsideMetrics.shared.SheetHorizontalPadding))
        }
        .scrollIndicators(.hidden)
    }

    private var header: some View {
        HStack(spacing: 0) {
            LineBadge(line: sailing.line, size: 32)
            Text("\(sailing.number) · \(sailing.dateLabel)")
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.6)
                .foregroundStyle(Portside.textGray)
                .padding(.leading, 8)
            Spacer()
            Button(action: onClose) {
                Circle()
                    .fill(Portside.chipBg)
                    .frame(width: 30, height: 30)
                    .overlay {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Portside.textDark)
                    }
            }
            .buttonStyle(.plain)
        }
        .padding(.top, 10)
    }

    /// Status as a pill + inline countdown rather than a banner strip.
    private var statusRow: some View {
        let banner = SailingPresentation.shared.banner(sailing: sailing)
        return HStack(spacing: 8) {
            StatusPill(status: sailing.status)
            (Text(banner.prefix).foregroundColor(Portside.textGray)
                + Text(banner.value)
                    .fontWeight(.semibold)
                    .foregroundColor(Color(argb: banner.tint)))
                .font(.system(size: 13))
        }
    }

    /// The hero card: a large crossing bar with a shore block under each end.
    private var crossingHero: some View {
        VStack(spacing: 0) {
            CrossingBarView(
                progress: CGFloat(SailingPresentation.shared.crossingProgress(sailing: sailing)),
                barHeight: 26,
                boatSize: 18
            )
            HStack(alignment: .top, spacing: 14) {
                ShoreBlock(
                    code: sailing.origin.code,
                    harbour: sailing.origin.name,
                    time: sailing.departTime,
                    strikethrough: sailing.scheduledDepartTime,
                    note: sailing.departNote,
                    tint: sailing.timeTint,
                    chips: originChips,
                    alignEnd: false
                )
                Spacer(minLength: 0)
                ShoreBlock(
                    code: sailing.destination.code,
                    harbour: sailing.destination.name,
                    time: sailing.arriveTime + (sailing.arrivesNextDay ? " ⁺¹" : ""),
                    strikethrough: nil,
                    note: sailing.arriveNote,
                    tint: sailing.timeTint,
                    chips: destinationChips,
                    alignEnd: true
                )
            }
            .padding(.top, 10)
            Text(SailingPresentation.shared.routeSummary(sailing: sailing, unit: Units.unit(from: distanceUnit)))
                .font(.system(size: 11))
                .foregroundStyle(Portside.textGray)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
            if sailing.isAtSea, let kn = sailing.speedKn {
                Text(verbatim: "\(kn.int32Value) kn · heading \(sailing.headingDeg?.int32Value ?? 0)°")
                    .font(.system(size: 11))
                    .foregroundStyle(Portside.textGray)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 3)
            }
        }
        .padding(16)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
    }

    private var originChips: [String] {
        var chips = ["Berth \(sailing.departBerth)", "Terminal \(sailing.departTerminal)"]
        if let deck = sailing.carDeck { chips.append("Deck \(deck)") }
        return chips
    }

    private var destinationChips: [String] {
        var chips: [String] = []
        if let berth = sailing.arriveBerth { chips.append("Berth \(berth)") }
        chips.append("Terminal \(sailing.arriveTerminal)")
        return chips
    }
}

/// A shore end of the crossing hero: port code and harbour name, the time
/// (with the original schedule struck through on delays), the status note,
/// and the pier signs (berth / terminal / deck) as chips.
struct ShoreBlock: View {
    let code: String
    let harbour: String
    let time: String
    let strikethrough: String?
    let note: String
    let tint: Color
    let chips: [String]
    let alignEnd: Bool

    /// Only the leading "7min Late" fragment carries the status tint, as in
    /// the shared rules; the rest of the note stays gray.
    private var noteText: some View {
        let parts = SailingPresentation.shared.noteParts(note: note)
        let head = (parts.first as? String) ?? note
        let tail = (parts.second as? String) ?? ""
        return (Text(head).foregroundColor(tint) + Text(tail).foregroundColor(Portside.textGray))
            .font(.system(size: 11))
    }

    var body: some View {
        VStack(alignment: alignEnd ? .trailing : .leading, spacing: 0) {
            Text(code)
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(Portside.textDark)
            Text(harbour)
                .font(.system(size: 11))
                .foregroundStyle(Portside.textGray)
                .multilineTextAlignment(alignEnd ? .trailing : .leading)
                .padding(.top, 1)
            HStack(alignment: .lastTextBaseline, spacing: 6) {
                if alignEnd, let strikethrough {
                    Text(strikethrough)
                        .font(.system(size: 12))
                        .strikethrough()
                        .foregroundStyle(Portside.textGray)
                }
                Text(time)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(tint)
                if !alignEnd, let strikethrough {
                    Text(strikethrough)
                        .font(.system(size: 12))
                        .strikethrough()
                        .foregroundStyle(Portside.textGray)
                }
            }
            .padding(.top, 6)
            noteText
                .multilineTextAlignment(alignEnd ? .trailing : .leading)
                .padding(.top, 2)
            // Chips reflow as whole units — a chip never breaks mid-word.
            ChipFlow(spacing: 5) {
                ForEach(chips, id: \.self) { BerthChipView(label: $0) }
            }
            .padding(.top, 8)
        }
    }
}

struct InfoTile: View {
    let icon: String
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 13))
                    .foregroundStyle(Portside.textGray)
                Text(title)
                    .font(.system(size: 9))
                    .tracking(0.8)
                    .foregroundStyle(Portside.textGray)
                Spacer()
            }
            .padding(.bottom, 6)
            Text(value)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Portside.textDark)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 14))
    }
}

/// Sea conditions along the crossing: waves / wind / water stats over a
/// smooth champagne wave-height curve, with the vessel's live position marked.
struct CrossingConditionsCard: View {
    let sailing: Sailing
    let sea: SeaState

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Crossing Conditions")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Portside.textDark)
            Text("Forecast along the route")
                .font(.system(size: 11))
                .foregroundStyle(Portside.textGray)
                .padding(.top, 2)
            HStack(alignment: .top, spacing: 20) {
                ForEach(
                    Array(SailingPresentation.shared.seaStats(state: sea).enumerated()),
                    id: \.offset
                ) { _, stat in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(stat.label)
                            .font(.system(size: 9))
                            .tracking(0.6)
                            .foregroundStyle(Portside.textGray)
                        HStack(spacing: 5) {
                            Image(systemName: stat.icon)
                                .font(.system(size: 11))
                                .foregroundStyle(Portside.route)
                            Text(stat.value)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(Portside.textDark)
                        }
                    }
                }
            }
            .padding(.top, 12)

            WaveCurveView(
                curve: sea.waveCurveM.map { CGFloat(truncating: $0) },
                progress: CGFloat(SailingPresentation.shared.crossingProgress(sailing: sailing)),
                live: sailing.isAtSea
            )
            .frame(height: 64)
            .padding(.top, 14)

            HStack {
                Text(sailing.departTime)
                Spacer()
                Text("wave height").tracking(0.6)
                Spacer()
                Text(sailing.arriveTime)
            }
            .font(.system(size: 10))
            .foregroundStyle(Portside.textGray)
            .padding(.top, 4)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct WaveCurveView: View {
    let curve: [CGFloat]
    let progress: CGFloat
    let live: Bool

    var body: some View {
        GeometryReader { geo in
            let maxWave = max((curve.max() ?? 1) * 1.25, 0.5)
            let stepX = geo.size.width / CGFloat(curve.count - 1)
            let ys = curve.map { geo.size.height - ($0 / maxWave) * geo.size.height }

            let path = Path { p in
                p.move(to: CGPoint(x: 0, y: ys[0]))
                for i in 0..<(curve.count - 1) {
                    let x0 = CGFloat(i) * stepX
                    let x1 = CGFloat(i + 1) * stepX
                    p.addQuadCurve(
                        to: CGPoint(x: (x0 + x1) / 2, y: (ys[i] + ys[i + 1]) / 2),
                        control: CGPoint(x: x0, y: ys[i])
                    )
                }
                p.addLine(to: CGPoint(x: geo.size.width, y: ys[curve.count - 1]))
            }

            ZStack {
                path
                    .strokedPath(StrokeStyle())
                    .fill(Color.clear)
                Path { p in
                    p.addPath(path)
                    p.addLine(to: CGPoint(x: geo.size.width, y: geo.size.height))
                    p.addLine(to: CGPoint(x: 0, y: geo.size.height))
                    p.closeSubpath()
                }
                .fill(
                    LinearGradient(
                        colors: [Portside.route.opacity(0.22), Portside.route.opacity(0.02)],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                Path { p in
                    p.move(to: CGPoint(x: 0, y: geo.size.height))
                    p.addLine(to: CGPoint(x: geo.size.width, y: geo.size.height))
                }
                .stroke(Portside.divider, lineWidth: 1)
                path.stroke(Portside.route, style: StrokeStyle(lineWidth: 2, lineCap: .round))

                if live {
                    let px = geo.size.width * min(max(progress, 0), 1)
                    let i = min(max(Int(progress * CGFloat(curve.count - 1)), 0), curve.count - 1)
                    Circle().fill(Portside.cardBg).frame(width: 12, height: 12)
                        .position(x: px, y: ys[i])
                    Circle().fill(Portside.route).frame(width: 7, height: 7)
                        .position(x: px, y: ys[i])
                }
            }
        }
    }
}

struct VesselCard: View {
    let sailing: Sailing
    let note: String

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .topLeading) {
                // A real ship at sea (CC0, Wikimedia Commons), pre-cropped to
                // the card's ratio; same asset the Compose apps bundle.
                if let image = UIImage(named: "vessel-photo") {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                }

                // Ink scrim so the title band reads over the sky.
                VStack(spacing: 0) {
                    LinearGradient(
                        colors: [Color(hex: 0x0A0E14).opacity(0.7), .clear],
                        startPoint: .top, endPoint: .bottom
                    )
                    .frame(height: 74)
                    Spacer(minLength: 0)
                }

                VStack(alignment: .leading, spacing: 0) {
                    Text("Your Vessel")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(.white)
                    Text(vesselLine)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.85))
                }
                .padding(.horizontal, 14)
                .padding(.top, 12)
            }
            .aspectRatio(1.9, contentMode: .fit)
            .clipped()

            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("INBOUND STATUS")
                        .font(.system(size: 9))
                        .tracking(0.8)
                        .foregroundStyle(Portside.textGray)
                    Spacer()
                    Text(departedLine)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(sailing.late ? Portside.red : Portside.green)
                }
                .padding(.top, 12)
                Text(note)
                    .font(.system(size: 12))
                    .foregroundStyle(Portside.textGray)
                    .padding(.top, 6)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
        }
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private var vesselLine: String {
        guard let info = sailing.vesselInfo else { return sailing.vessel }
        let head = info.components(separatedBy: " ·").first ?? info
        return "\(sailing.vessel) · \(head)"
    }

    private var departedLine: String {
        guard sailing.late else { return "Departed on time" }
        let head = sailing.departNote.components(separatedBy: " ·").first ?? sailing.departNote
        return "Departed \(head.lowercased())"
    }
}

/// Times as a small grid: SCHEDULED and ACTUAL columns per port call.
struct DetailedTimetableCard: View {
    let sailing: Sailing

    var body: some View {
        let tint = sailing.late ? Portside.red : Portside.green
        VStack(alignment: .leading, spacing: 8) {
            Text("Timetable")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Portside.textDark)
            TimetableGridRow(label: "", scheduled: "SCHEDULED", actual: "ACTUAL",
                             tint: Portside.textGray, header: true)
            Rectangle().fill(Portside.divider).frame(height: 1)
            TimetableGridRow(
                label: "Depart \(sailing.origin.code)",
                scheduled: sailing.scheduledDepartTime ?? sailing.departTime,
                actual: sailing.departTime,
                tint: tint, header: false
            )
            TimetableGridRow(
                label: "Arrive \(sailing.destination.code)",
                scheduled: sailing.arriveTime,
                actual: sailing.arriveTime,
                tint: tint, header: false
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct TimetableGridRow: View {
    let label: String
    let scheduled: String
    let actual: String
    let tint: Color
    let header: Bool

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: header ? 9 : 13))
                .tracking(header ? 0.8 : 0)
                .foregroundStyle(header ? Portside.textGray : Portside.textDark)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(scheduled)
                .font(.system(size: header ? 9 : 13))
                .tracking(header ? 0.8 : 0)
                .foregroundStyle(Portside.textGray)
                .frame(width: 90, alignment: .trailing)
            Text(actual)
                .font(.system(size: header ? 9 : 13, weight: header ? .regular : .medium))
                .tracking(header ? 0.8 : 0)
                .foregroundStyle(header ? Portside.textGray : tint)
                .frame(width: 80, alignment: .trailing)
        }
    }
}

/// Practical notes for the day of travel, one card with hairline rows.
struct GoodToKnowCard: View {
    let items: [GoodToKnowItem]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Before You Board")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Portside.textDark)
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 4)
            ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                if index > 0 {
                    Rectangle()
                        .fill(Portside.divider)
                        .frame(height: 1)
                        .padding(.leading, 48)
                }
                HStack(spacing: 14) {
                    Image(systemName: item.icon)
                        .font(.system(size: 15))
                        .foregroundStyle(Portside.route)
                        .frame(width: 20)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(item.title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Portside.textDark)
                        Text(item.note)
                            .font(.system(size: 12))
                            .foregroundStyle(Portside.textGray)
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
        }
        .padding(.bottom, 6)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
    }
}

struct InfoLine: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top) {
            Text(label)
                .font(.system(size: 13))
                .foregroundStyle(Portside.textGray)
            Spacer()
            Text(value)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Portside.textDark)
                .multilineTextAlignment(.trailing)
        }
    }
}
