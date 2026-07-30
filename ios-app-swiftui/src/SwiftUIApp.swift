import SwiftUI
import KotlinModules

// The all-SwiftUI iOS variant. Every screen, sheet, row, and menu here is
// SwiftUI; Kotlin supplies the domain layer and its ViewModels, the shared
// menu spec, and the one thing SwiftUI has no equivalent for — the Compose
// map/globe backdrop (a custom orthographic globe shader plus MapLibre).

private let spaceBackgroundColor = UIColor(red: 0.016, green: 0.024, blue: 0.047, alpha: 1)

// MARK: - Shared menu spec, rendered as SwiftUI

/// Renders a Kotlin `MenuSpec` as SwiftUI menu content: one `Section` per
/// spec section (SwiftUI draws the separators), `Menu` for submenus, and a
/// two-`Text` label where the spec carries a subtitle.
struct SpecMenuContent: View {
    let spec: MenuSpec
    /// When a menu opens upward from a bottom anchor, iOS puts the first
    /// declared item nearest the anchor — declare reversed so the visual
    /// order still reads top-down like the Compose apps.
    var bottomAnchored: Bool = false

    var body: some View {
        let sections = bottomAnchored ? spec.sections.reversed().map { $0 } : spec.sections
        ForEach(Array(sections.enumerated()), id: \.offset) { _, section in
            Section {
                let items = bottomAnchored ? section.items.reversed().map { $0 } : section.items
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    SpecMenuItemView(item: item)
                }
            }
        }
    }
}

struct SpecMenuItemView: View {
    let item: MenuItem

    var body: some View {
        if item.submenu.isEmpty {
            Button(role: item.destructive ? .destructive : nil) {
                // Mock data: the menus are for show, like the rest of the app.
            } label: {
                if let subtitle = item.subtitle {
                    // A second Text in a menu label becomes the subtitle line.
                    Text(item.title)
                    Text(subtitle)
                    Image(systemName: item.icon)
                } else {
                    Label(item.title, systemImage: item.icon)
                }
            }
            // Menu glyphs are label-colored; without this the
            // app's blue accent tints them.
            .tint(item.destructive ? Color.red : Color.primary)
        } else {
            Menu {
                ForEach(Array(item.submenu.enumerated()), id: \.offset) { _, child in
                    SpecMenuItemView(item: child)
                }
            } label: {
                Label(item.title, systemImage: item.icon)
            }
            .tint(Color.primary)
        }
    }
}

// MARK: - The Compose backdrop

/// The only Compose surface in this app: space, globe, route arc, and the
/// vector map on the detail screen.
struct BackdropView: UIViewControllerRepresentable {
    let controller: BackdropController

    func makeUIViewController(context: Context) -> UIViewController {
        let vc = IosNativeHostKt.BackdropView(controller: controller)
        vc.view.backgroundColor = spaceBackgroundColor
        return vc
    }

    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

// MARK: - The persistent sheet

/// Portside's signature layout: a light sheet floating over the map that drags
/// between a peek height and (nearly) full screen. It is a plain SwiftUI
/// overlay rather than `.sheet` — a UIKit modal presentation over the Compose
/// canvas blanks its Metal layer, and a persistent non-dismissable sheet is
/// not what `.sheet` models anyway.
struct PortsideSheet<Content: View>: View {
    /// Fraction of the screen the sheet covers when collapsed.
    let peekFraction: CGFloat
    @ViewBuilder var content: Content

    @State private var expanded = false
    @GestureState private var dragOffset: CGFloat = 0

    private var sheetShape: UnevenRoundedRectangle {
        UnevenRoundedRectangle(
            topLeadingRadius: 24, bottomLeadingRadius: 0,
            bottomTrailingRadius: 0, topTrailingRadius: 24
        )
    }

    var body: some View {
        GeometryReader { geo in
            let peekTop = geo.size.height * (1 - peekFraction)
            let expandedTop: CGFloat = 60
            let base = expanded ? expandedTop : peekTop
            let top = min(max(base + dragOffset, expandedTop), peekTop)

            VStack(spacing: 0) {
                // Only the grabber drags the sheet: a gesture on the whole
                // sheet fights the scroll view inside it, which makes both
                // feel unreliable.
                Capsule()
                    .fill(Portside.divider)
                    .frame(width: 36, height: 4)
                    .padding(.top, 8)
                    .padding(.bottom, 4)
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 4)
                            .updating($dragOffset) { value, state, _ in
                                state = value.translation.height
                            }
                            .onEnded { value in
                                // The grabber only toggles expand/collapse; it never
                                // dismisses (the detail closes on its own X). A fling
                                // that also dismissed made the map-reveal pull drop the
                                // whole sheet instead of collapsing it to the peek.
                                let travel = value.predictedEndTranslation.height
                                if travel < -60 { expanded = true }
                                else if travel > 60 { expanded = false }
                            }
                    )
                content
            }
            .frame(width: geo.size.width, height: geo.size.height - expandedTop, alignment: .top)
            .background(Portside.sheetBg, in: sheetShape)
            // Clip content to the sheet: the Home↔Detail push uses a horizontal
            // `.move` transition, and without clipping the outgoing view (e.g.
            // the My Sailings header with its LS avatar) slides out past the
            // sheet's edge and floats over the map.
            .clipShape(sheetShape)
            .offset(y: top)
            .animation(.spring(response: 0.35, dampingFraction: 0.86), value: expanded)
            // Like the Compose apps: dragging the sheet content upward while it
            // is collapsed grows the sheet (the scroll view keeps its own
            // gesture, so content still scrolls once expanded).
            .simultaneousGesture(
                DragGesture(minimumDistance: 12)
                    .onEnded { value in
                        let travel = value.predictedEndTranslation.height
                        if !expanded && travel < -40 {
                            expanded = true
                        } else if expanded && travel > 80 && value.startLocation.y < 140 {
                            expanded = false
                        }
                    }
            )
        }
        .ignoresSafeArea()
    }
}

/// The map's instrument rail: one vertical capsule with hairline separators —
/// chart layers, wind overlay, and a compass — matching the Compose shell.
struct MapInstrumentRail: View {
    var body: some View {
        VStack(spacing: 0) {
            railIcon("square.3.layers.3d.down.right")
            separator
            railIcon("wind")
            separator
            railIcon("safari")
        }
        .background(Color(hex: 0x1A202A).opacity(0.7), in: RoundedRectangle(cornerRadius: 16))
    }

    private var separator: some View {
        Rectangle().fill(Color.white.opacity(0.2)).frame(width: 16, height: 1)
    }

    private func railIcon(_ name: String) -> some View {
        Image(systemName: name)
            .font(.system(size: 13))
            .foregroundStyle(.white.opacity(0.92))
            .frame(width: 34, height: 32)
    }
}

// MARK: - Tabs

/// My Sailings, with in-sheet Home → Detail navigation: the map updates behind
/// the persistent sheet, exactly like the Compose apps.
struct SailingsTab: View {
    let backdrop: BackdropController
    @Binding var detailSailingId: String?

    var body: some View {
        ZStack {
            BackdropView(controller: backdrop).ignoresSafeArea()

            VStack(spacing: 8) {
                MapInstrumentRail()
                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
            .padding(.top, 6)
            .padding(.trailing, 10)

            PortsideSheet(peekFraction: detailSailingId == nil ? 0.66 : 0.55) {
                if let id = detailSailingId,
                   let sailing = IosNativeHostKt.detailState(sailingId: id).sailing {
                    SailingDetailView(sailing: sailing) {
                        withAnimation(.easeInOut(duration: 0.35)) { detailSailingId = nil }
                    }
                    .transition(.move(edge: .trailing))
                } else {
                    SailingsListView(state: IosNativeHostKt.sailingsState()) { sailing in
                        withAnimation(.easeInOut(duration: 0.35)) { detailSailingId = sailing.id }
                    }
                    .transition(.move(edge: .leading))
                }
            }

            if let id = detailSailingId {
                VStack {
                    Spacer()
                    DetailActionBar(sailingId: id)
                }
            }
        }
        .onAppear { backdrop.show(sailingId: detailSailingId, detail: detailSailingId != nil) }
        .onChange(of: detailSailingId) { id in
            if id != nil {
                backdrop.show(sailingId: id, detail: true)
            } else {
                // Defer the flat-map → globe handoff until the sheet's close
                // animation has finished: the interop swap stalls the main
                // thread ~0.5s, which froze the slide mid-sailing when fired
                // immediately with it.
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) {
                    if detailSailingId == nil { backdrop.show(sailingId: nil, detail: false) }
                }
            }
        }
    }
}

/// Profile, with the friends list navigating inside the persistent sheet —
/// same in-sheet push the Compose apps use.
struct ProfileTab: View {
    let backdrop: BackdropController
    @State private var showFriends = false

    var body: some View {
        ZStack {
            BackdropView(controller: backdrop).ignoresSafeArea()

            VStack(spacing: 8) {
                MapInstrumentRail()
                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
            .padding(.top, 6)
            .padding(.trailing, 10)

            PortsideSheet(peekFraction: 0.66) {
                if showFriends {
                    FriendsView(state: IosNativeHostKt.friendsState()) {
                        withAnimation(.easeInOut(duration: 0.35)) { showFriends = false }
                    }
                    .transition(.move(edge: .trailing))
                } else {
                    ProfileView(state: IosNativeHostKt.profileState()) {
                        withAnimation(.easeInOut(duration: 0.35)) { showFriends = true }
                    }
                    .transition(.move(edge: .leading))
                }
            }
        }
    }
}

/// The detail screen's floating action bar. The ellipsis opens the same
/// shared sailing menu as the row long-press, as a SwiftUI `Menu`.
struct DetailActionBar: View {
    let sailingId: String

    var body: some View {
        HStack {
            barIcon("square.and.arrow.up")
            barIcon("bell.slash")
            Menu {
                if let spec = IosNativeHostKt.sailingMenuSpec(sailingId: sailingId) {
                    SpecMenuContent(spec: spec, bottomAnchored: true)
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Portside.textDark)
                    .frame(width: 37, height: 44)
            }
            Spacer()
            Button { } label: {
                Text("Book Return")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Portside.sheetBg)
                    .padding(.horizontal, 18)
                    .frame(height: 40)
                    .background(Portside.accent, in: Capsule())
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(Portside.raised, in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Portside.raisedStroke, lineWidth: 1))
        .shadow(color: .black.opacity(0.15), radius: 8, y: 2)
        .padding(.horizontal, 16)
        .padding(.bottom, 10)
    }

    private func barIcon(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(Portside.textDark)
            .frame(width: 37, height: 44)
    }
}

// MARK: - App chrome

@available(iOS 26.0, *)
struct SwiftUIContentView: View {
    @State private var selectedTab = 0
    @State private var showAddSailing = false
    @State private var detailSailingId: String? = nil
    private let sailingsBackdrop = BackdropController()
    private let profileBackdrop = BackdropController()

    var body: some View {
        TabView(selection: Binding(
            get: { selectedTab },
            set: { newValue in
                // The search tab never activates: selecting it presents Add
                // Sailing over the current tab, so the map stays behind it.
                if newValue == 2 {
                    withAnimation { showAddSailing = true }
                } else {
                    selectedTab = newValue
                }
            }
        )) {
            Tab("My Voyages", systemImage: "ferry", value: 0) {
                SailingsTab(backdrop: sailingsBackdrop, detailSailingId: $detailSailingId)
                    .toolbarVisibility(
                        (detailSailingId == nil && !showAddSailing) ? .automatic : .hidden,
                        for: .tabBar
                    )
            }
            Tab("Profile", systemImage: "person.crop.circle", value: 1) {
                ProfileTab(backdrop: profileBackdrop)
                    .toolbarVisibility(showAddSailing ? .hidden : .automatic, for: .tabBar)
            }
            Tab("Search", systemImage: "magnifyingglass", value: 2, role: .search) {
                Color.clear
            }
        }
        .tabBarMinimizeBehavior(.automatic)
        .tint(.blue)
        .background(Portside.sea.ignoresSafeArea())
        .overlay {
            if showAddSailing {
                AddSailingView(state: IosNativeHostKt.addSailingState()) {
                    withAnimation { showAddSailing = false }
                }
                .transition(.move(edge: .bottom))
                .zIndex(1)
            }
        }
    }
}

struct RootView: View {
    var body: some View {
        if #available(iOS 26.0, *) {
            SwiftUIContentView()
        } else {
            Color.black.ignoresSafeArea()
        }
    }
}

@main
struct PortsideSwiftUIApp: App {
    init() {
        UIWindow.appearance().backgroundColor = spaceBackgroundColor
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
