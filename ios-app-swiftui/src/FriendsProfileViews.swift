import SwiftUI
import KotlinModules

/// Friends: filter chips and a card per friend, from the shared ViewModel.
/// Pushed from the Profile tab's friend count, so it leads with a back chip.
struct FriendsView: View {
    let state: FriendsUiState
    var onBack: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            if let onBack {
                HStack(spacing: 12) {
                    Button(action: onBack) {
                        Circle()
                            .fill(Portside.chipBg)
                            .frame(width: 30, height: 30)
                            .overlay {
                                Image(systemName: "arrow.left")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(Portside.textDark)
                            }
                    }
                    .buttonStyle(.plain)
                    Text("Friends")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(Portside.textDark)
                    Spacer()
                }
                .padding(.horizontal, 14)
                .padding(.top, 12)
            } else {
                ScreenHeader(title: "Friends", profile: state.profile)
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
            }

            HStack(spacing: 18) {
                SegmentLabel(label: "Everyone", selected: true)
                SegmentLabel(label: "Today", selected: false)
                Spacer()
                Text("+ Add Friend")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Portside.accent)
            }
            .padding(.horizontal, 20)
            .padding(.top, 14)

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(Array(state.friends.enumerated()), id: \.offset) { _, friend in
                        FriendCard(friend: friend)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 12)
                .padding(.bottom, 96)
            }
            .scrollIndicators(.hidden)
        }
    }
}

struct FriendCard: View {
    let friend: Friend

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color(hex: UInt32(truncatingIfNeeded: friend.color)))
                .frame(width: 38, height: 38)
                .overlay {
                    Text(friend.initials)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                }
            VStack(alignment: .leading, spacing: 1) {
                Text(friend.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Portside.textDark)
                Text("\(friend.route) · \(friend.sailingLabel)")
                    .font(.system(size: 12))
                    .foregroundStyle(Portside.textGray)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 3) {
                StatusPill(status: friend.status)
                Text(friend.statusNote)
                    .font(.system(size: 11))
                    .foregroundStyle(Portside.textGray)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
    }
}

/// The Profile tab: an Instagram-style header — big avatar, name, and a
/// counts row whose Friends column opens the friends list — with the logbook
/// dashboard below it. Mirrors the Compose ProfileScreen.
struct ProfileView: View {
    let state: ProfileUiState
    let onOpenFriends: () -> Void

    @AppStorage("distanceUnit") private var distanceUnit = "nm"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // No title bar: the avatar block IS the header, with the
                // settings button anchored after the name.
                HStack(spacing: 14) {
                    AvatarView(profile: state.profile, size: 64)
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(state.profile.name)
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(Portside.textDark)
                            Spacer()
                            SettingsMenuButton()
                        }
                        HStack(spacing: 0) {
                            CountColumn(value: "\(state.stats.sailings)", label: "Voyages")
                            CountColumn(value: "\(state.friendCount)", label: "Friends")
                                .contentShape(Rectangle())
                                .onTapGesture(perform: onOpenFriends)
                            CountColumn(value: "\(state.stats.ports)", label: "Ports")
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.top, 18)

                heroCard.padding(.top, 18)

                HStack(spacing: 10) {
                    StatTile(label: "VOYAGES", value: "\(state.stats.sailings)",
                             sub: "\(state.stats.overnight) Overnight")
                    StatTile(label: "TIME AT SEA", value: state.stats.timeAtSea, sub: nil)
                }
                .padding(.top, 10)
                HStack(spacing: 10) {
                    StatTile(label: "PORTS", value: "\(state.stats.ports)", sub: nil)
                    StatTile(label: "LINES", value: "\(state.stats.lines)", sub: nil)
                }
                .padding(.top, 10)

                monthCard.padding(.top, 10)
                allStatsRow.padding(.top, 10)

                Text("Running on \(state.runningOn)")
                    .font(.system(size: 11))
                    .foregroundStyle(Portside.textGray)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 16)
                    .padding(.bottom, 96)
            }
            .padding(.horizontal, 16)
        }
        .scrollIndicators(.hidden)
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("ALL-TIME SEA LOGBOOK")
                    .font(.system(size: 11, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(.white.opacity(0.8))
                Spacer()
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 13))
                    .foregroundStyle(.white.opacity(0.85))
            }
            Text(Units.format(nm: state.stats.distanceNm, raw: distanceUnit))
                .font(.system(size: 36, weight: .semibold))
                .foregroundStyle(Portside.route)
                .padding(.top, 10)
            Text(state.stats.comparison)
                .font(.system(size: 12))
                .foregroundStyle(.white.opacity(0.7))
                .padding(.top, 2)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [Portside.logTop, Portside.logBottom],
                           startPoint: .top, endPoint: .bottom),
            in: RoundedRectangle(cornerRadius: 18)
        )
    }

    private var monthCard: some View {
        HStack(alignment: .center) {
            Text(verbatim: "\(state.stats.monthSailings)")
                .font(.system(size: 36, weight: .semibold))
                .foregroundStyle(Portside.route)
            Text(state.stats.monthLabel)
                .font(.system(size: 13))
                .foregroundStyle(Portside.textGray)
                .padding(.leading, 12)
            Spacer()
            Image(systemName: "square.and.arrow.up")
                .font(.system(size: 14))
                .foregroundStyle(Portside.textGray)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 18))
    }

    private var allStatsRow: some View {
        HStack {
            Text("All Voyage Stats")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Portside.textDark)
            Spacer()
            Text("›")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Portside.textGray)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 14))
    }

}

/// An underline segment: quiet text with a champagne bar under the active one.
struct SegmentLabel: View {
    let label: String
    let selected: Bool

    var body: some View {
        VStack(spacing: 3) {
            Text(label)
                .font(.system(size: 13, weight: selected ? .semibold : .regular))
                .foregroundStyle(selected ? Portside.textDark : Portside.textGray)
            RoundedRectangle(cornerRadius: 1)
                .fill(selected ? Portside.accent : .clear)
                .frame(width: 18, height: 2)
        }
    }
}

/// One column of the profile header's counts row.
struct CountColumn: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 1) {
            Text(value)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Portside.textDark)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Portside.textGray)
        }
        .frame(maxWidth: .infinity)
    }
}

/// A white stat tile: small caps label, big value, optional sub line.
struct StatTile: View {
    let label: String
    let value: String
    let sub: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(label)
                .font(.system(size: 10))
                .tracking(0.8)
                .foregroundStyle(Portside.textGray)
            Text(value)
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(Portside.textDark)
                .padding(.top, 4)
            if let sub {
                Text(sub)
                    .font(.system(size: 10))
                    .foregroundStyle(Portside.textGray)
                    .padding(.top, 1)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 14))
    }
}
