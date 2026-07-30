import SwiftUI
import KotlinModules

// The Portside look, in SwiftUI. Colors and metrics mirror the shared Kotlin
// theme (PortsideColors) so this app and the Compose ones stay identical.

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension Color {
    /// Every color in this app comes from the shared Kotlin palette, so it
    /// cannot drift from the Compose apps.
    init(argb: Int64) { self.init(hex: UInt32(truncatingIfNeeded: argb)) }
}

enum Portside {
    private static let p = PortsidePalette.shared
    static let sea = Color(argb: p.Sea)
    static let sheetBg = Color(argb: p.SheetBg)
    static let cardBg = Color(argb: p.CardBg)
    static let divider = Color(argb: p.Divider)
    static let raised = Color(argb: p.Raised)
    static let raisedStroke = Color(argb: p.RaisedStroke)
    static let chipBg = Color(argb: p.ChipBg)
    static let textDark = Color(argb: p.TextDark)
    static let textGray = Color(argb: p.TextGray)
    static let green = Color(argb: p.GreenTime)
    static let red = Color(argb: p.RedTime)
    static let accent = Color(argb: p.Accent)
    static let berthChip = Color(argb: p.BerthChip)
    static let berthChipText = Color(argb: p.BerthChipText)
    static let avatarTop = Color(argb: p.AvatarTop)
    static let avatarEnd = Color(argb: p.AvatarEnd)
    static let route = Color(argb: p.Route)
    static let logTop = Color(argb: p.LogTop)
    static let logBottom = Color(argb: p.LogBottom)
}

extension SailingStatus {
    /// Kotlin's `AtSea` entry arrives lowercased; this keeps call sites readable.
    static var atSea: SailingStatus { SailingStatus.atsea }
}

extension Sailing {
    /// Big departure/arrival times: red while late, green otherwise.
    var timeTint: Color { Color(argb: SailingPresentation.shared.timeTint(sailing: self)) }
    var isAtSea: Bool { status == SailingStatus.atSea }
    var isDocked: Bool { status == SailingStatus.docked }
}

// MARK: - Small shared pieces

/// The signed-in traveler's gradient initials bubble.
struct AvatarView: View {
    let profile: Profile
    var size: CGFloat = 34

    var body: some View {
        Circle()
            .fill(
                LinearGradient(
                    colors: [Portside.avatarTop, Portside.avatarEnd],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                )
            )
            .frame(width: size, height: size)
            .overlay {
                Text(profile.initials)
                    .font(.system(size: size * 0.36, weight: .bold))
                    .foregroundStyle(.white)
            }
    }
}

/// Two-letter line mark in the carrier's brand color.
struct LineBadge: View {
    let line: Line
    var size: CGFloat = 15

    var body: some View {
        Circle()
            .fill(Color(hex: UInt32(truncatingIfNeeded: line.color)))
            .frame(width: size, height: size)
            .overlay {
                Text(line.code)
                    .font(.system(size: size * 0.44, weight: .bold))
                    .foregroundStyle(.white)
            }
    }
}

/// Small status caption pill ("AT SEA", "DELAYED") in the status tint —
/// label and tint from the shared presentation rules.
struct StatusPill: View {
    let status: SailingStatus

    var body: some View {
        let tint = Color(argb: SailingPresentation.shared.pillTint(status: status))
        Text(SailingPresentation.shared.statusStyle(status: status).label)
            .font(.system(size: 10, weight: .semibold))
            .tracking(0.6)
            .foregroundStyle(tint)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(tint.opacity(0.12), in: Capsule())
    }
}

/// A simple wrapping row: children keep their intrinsic size and flow onto
/// new lines as whole units.
struct ChipFlow: Layout {
    var spacing: CGFloat = 5

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, width: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0, x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            width = max(width, x - spacing)
        }
        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

/// A pier-sign chip: "Berth 7", "Terminal 2", "Deck 5".
struct BerthChipView: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.system(size: 11, weight: .medium))
            .fixedSize()
            .foregroundStyle(Portside.berthChipText)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(Portside.berthChip, in: RoundedRectangle(cornerRadius: 6))
    }
}

/// Title row every tab shares: bold title and the settings button.
struct ScreenHeader: View {
    let title: String
    let profile: Profile

    var body: some View {
        HStack(spacing: 0) {
            Text(title)
                .font(.system(size: 26, weight: .bold))
                .foregroundStyle(Portside.textDark)
            Spacer()
            SettingsMenuButton()
        }
    }
}

/// Distance-unit choice persisted app-side; formatting comes from the shared
/// Kotlin rules so the numbers match the Compose apps exactly.
enum Units {
    static func unit(from raw: String) -> DistanceUnit {
        switch raw {
        case "km": return .kilometres
        case "mi": return .miles
        default: return .nauticalmiles
        }
    }

    static func format(nm: Int32, raw: String) -> String {
        SailingPresentation.shared.formatDistance(nm: nm, unit: unit(from: raw))
    }
}

/// The settings button: sliders glyph in a soft chip, opening the account
/// menu plus the distance-unit picker.
struct SettingsMenuButton: View {
    @AppStorage("distanceUnit") private var distanceUnit = "nm"

    var body: some View {
        Menu {
            SpecMenuContent(spec: IosNativeHostKt.profileMenuSpec())
            Picker("Distance", selection: $distanceUnit) {
                Text("Nautical miles").tag("nm")
                Text("Kilometres").tag("km")
                Text("Miles").tag("mi")
            }
        } label: {
            Image(systemName: "slider.horizontal.3")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Portside.textDark)
                .frame(width: 34, height: 34)
                .background(Color.white.opacity(0.10), in: Circle())
        }
    }
}

/// A white rounded card, the app's basic surface.
struct Card<Content: View>: View {
    var cornerRadius: CGFloat = 18
    @ViewBuilder var content: Content

    var body: some View {
        content
            .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: cornerRadius))
    }
}
