import SwiftUI
import KotlinModules

/// My Sailings: the shared ViewModel's state, rendered entirely in SwiftUI.
/// Each sailing is a crossing card, exactly as the Compose apps draw them.
struct SailingsListView: View {
    let state: SailingsUiState
    let onOpen: (Sailing) -> Void

    private var sailings: [Sailing] { state.upcoming + state.past }

    var body: some View {
        VStack(spacing: 0) {
            ScreenHeader(title: "My Voyages", profile: state.profile)
                .padding(.horizontal, 20)
                .padding(.top, 12)

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(sailings, id: \.id) { sailing in
                        SailingCardView(sailing: sailing)
                            .contentShape(Rectangle())
                            .onTapGesture { onOpen(sailing) }
                            // Pure SwiftUI context menu, contents from the
                            // shared spec, with the card itself as the preview.
                            .contextMenu {
                                if let spec = IosNativeHostKt.sailingMenuSpec(sailingId: sailing.id) {
                                    SpecMenuContent(spec: spec)
                                }
                            } preview: {
                                SailingCardView(sailing: sailing)
                                    .padding(.vertical, 4)
                                    .frame(width: UIScreen.main.bounds.width - 24)
                                    .background(Portside.sheetBg)
                            }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 10)
                .padding(.bottom, 96)
            }
            .scrollIndicators(.hidden)
        }
    }
}

/// One sailing as a crossing card: status strip on top, the crossing bar
/// (shore-to-shore water line with the vessel at its live position), and a
/// shore column under each end — port code, city, and time.
struct SailingCardView: View {
    let sailing: Sailing
    @AppStorage("distanceUnit") private var distanceUnit = "nm"

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 0) {
                Text(strip.label + strip.value)
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(0.6)
                    .foregroundStyle(Color(argb: strip.tint))
                Spacer(minLength: 8)
                LineBadge(line: sailing.line)
                Text(sailing.number)
                    .font(.system(size: 11, weight: .medium))
                    .tracking(0.6)
                    .foregroundStyle(Portside.textGray)
                    .padding(.leading, 6)
            }

            CrossingBarView(progress: CGFloat(SailingPresentation.shared.crossingProgress(sailing: sailing)))
                .padding(.top, 12)

            HStack(alignment: .top, spacing: 8) {
                ShoreColumn(
                    code: sailing.origin.code,
                    city: sailing.origin.city,
                    time: sailing.departTime,
                    tint: sailing.timeTint,
                    alignEnd: false
                )
                Spacer(minLength: 0)
                // The crossing's length sits in the open water between shores.
                Text(Units.format(nm: sailing.distanceNm, raw: distanceUnit))
                    .font(.system(size: 11))
                    .foregroundStyle(Portside.textGray)
                    .frame(maxHeight: .infinity, alignment: .center)
                Spacer(minLength: 0)
                ShoreColumn(
                    code: sailing.destination.code,
                    city: sailing.destination.city,
                    time: sailing.arriveTime + (sailing.arrivesNextDay ? " ⁺¹" : ""),
                    tint: sailing.timeTint,
                    alignEnd: true
                )
            }
            .padding(.top, 6)
        }
        .padding(14)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 16))
    }

    private var strip: LabeledValue { SailingPresentation.shared.statusStrip(sailing: sailing) }
}

/// Port code with the city beside it and the time under, hugging its shore.
struct ShoreColumn: View {
    let code: String
    let city: String
    let time: String
    let tint: Color
    let alignEnd: Bool

    var body: some View {
        VStack(alignment: alignEnd ? .trailing : .leading, spacing: 2) {
            HStack(alignment: .lastTextBaseline, spacing: 6) {
                if alignEnd {
                    Text(city)
                        .font(.system(size: 11))
                        .foregroundStyle(Portside.textGray)
                }
                Text(code)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Portside.textDark)
                if !alignEnd {
                    Text(city)
                        .font(.system(size: 11))
                        .foregroundStyle(Portside.textGray)
                }
            }
            Text(time)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(tint)
        }
    }
}

/// The app's signature mark: a crossing between two shores — a shore tick on
/// each side, the water line between (solid behind the vessel, dashed ahead),
/// and the boat at `progress` (0 = origin shore, 1 = docked).
struct CrossingBarView: View {
    let progress: CGFloat
    var barHeight: CGFloat = 22
    var boatSize: CGFloat = 16

    var body: some View {
        GeometryReader { geo in
            let y = geo.size.height / 2
            let tick: CGFloat = 3
            let tickH = geo.size.height * 0.8
            let inset = tick + 6
            let x0 = inset
            let x1 = geo.size.width - inset
            let frac = min(max(progress, 0), 1)
            let bx = x0 + (x1 - x0) * frac
            // The water line parts around the hull instead of running under it.
            let clearance = boatSize / 2 + 4

            ZStack {
                Path { p in
                    // A shore tick yields to the hull when the boat is parked on it.
                    if frac > 0.055 {
                        p.addRoundedRect(
                            in: CGRect(x: 0, y: y - tickH / 2, width: tick, height: tickH),
                            cornerSize: CGSize(width: tick / 2, height: tick / 2)
                        )
                    }
                    if frac < 0.945 {
                        p.addRoundedRect(
                            in: CGRect(x: geo.size.width - tick, y: y - tickH / 2, width: tick, height: tickH),
                            cornerSize: CGSize(width: tick / 2, height: tick / 2)
                        )
                    }
                }
                .fill(Portside.textGray.opacity(0.55))

                if bx - clearance - x0 > 1 {
                    Path { p in
                        p.move(to: CGPoint(x: x0, y: y))
                        p.addLine(to: CGPoint(x: bx - clearance, y: y))
                    }
                    .stroke(Portside.accent, style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                }
                if x1 - (bx + clearance) > 1 {
                    Path { p in
                        p.move(to: CGPoint(x: bx + clearance, y: y))
                        p.addLine(to: CGPoint(x: x1, y: y))
                    }
                    .stroke(
                        Portside.divider,
                        style: StrokeStyle(lineWidth: 2.5, lineCap: .round, dash: [5, 6])
                    )
                }

                Image(systemName: "ferry.fill")
                    .font(.system(size: boatSize * 0.85))
                    .foregroundStyle(Portside.accent)
                    .position(x: bx, y: y)
            }
        }
        .frame(height: barHeight)
    }
}
