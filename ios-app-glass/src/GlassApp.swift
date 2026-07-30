import SwiftUI
import KotlinModules

// Compose destinations, one UIViewControllerRepresentable each. All UI comes
// from the shared Kotlin module; Swift only supplies the system chrome —
// TabView, the search sheet, the detail action bar, and the long-press /
// ellipsis sailing menus — which picks up Liquid Glass on iOS 26
// automatically. Detail navigation happens inside the Compose sheet (the map
// updates behind it), matching the Compose app.

// The Compose canvas is a Metal layer that can blank momentarily while UIKit
// animates the hosting controller (e.g. the sheet presentation's card-stack
// scale-down). Match the app's space background so those moments read as the
// backdrop instead of a white flash.
private let spaceBackground = UIColor(red: 0.016, green: 0.024, blue: 0.047, alpha: 1)

// Invisible native button parked over the Compose avatar: tapping it presents
// the system account menu. The anchor streams the avatar's frame from Kotlin.
enum ProfileMenu {
    static func attach(to vc: UIViewController, anchor: ProfileMenuAnchor) {
        let button = UIButton(type: .custom)
        button.showsMenuAsPrimaryAction = true
        button.menu = SpecUIMenu.menu(from: GlassControllersKt.profileMenuSpec())
        button.isHidden = true
        vc.view.addSubview(button)
        anchor.listener = { [weak vc] x, y, w, h in
            let frame = CGRect(
                x: CGFloat(truncating: x), y: CGFloat(truncating: y),
                width: CGFloat(truncating: w), height: CGFloat(truncating: h)
            )
            button.frame = frame
            button.isHidden = frame.isEmpty
            // Compose adds its canvas view after us; stay on top so taps
            // reach the button instead of the canvas.
            if let view = vc?.view, view.subviews.last !== button {
                view.bringSubviewToFront(button)
            }
        }
    }
}

struct SailingsTabView: UIViewControllerRepresentable {
    let onDetailShown: (String?) -> Void

    func makeCoordinator() -> SailingMenuCoordinator { SailingMenuCoordinator() }

    func makeUIViewController(context: Context) -> UIViewController {
        let anchor = ProfileMenuAnchor()
        let vc = GlassControllersKt.SailingsTabController(
            onDetailShown: onDetailShown,
            profileAnchor: anchor
        )
        // Real system context menus for the sailing rows: Compose reports row
        // bounds, this interaction hit-tests them and previews the row.
        vc.view.addInteraction(UIContextMenuInteraction(delegate: context.coordinator))
        ProfileMenu.attach(to: vc, anchor: anchor)
        vc.view.backgroundColor = spaceBackground
        return vc
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

// The sailing and profile menus, rendered from the SHARED Kotlin MenuSpec —
// the same source every other platform's renderer uses — as a UIMenu
// (long-press context menu, avatar button) and as SwiftUI Menu content (the
// detail action bar's ellipsis button).

/// Renders a Kotlin `MenuSpec` as a UIKit `UIMenu`. Menu glyphs are
/// label-colored (the app's window tint would
/// otherwise color them); destructive items keep the system red.
enum SpecUIMenu {
    static func menu(from spec: MenuSpec?) -> UIMenu {
        UIMenu(children: (spec?.sections ?? []).map { section in
            UIMenu(options: .displayInline, children: section.items.map(element))
        })
    }

    private static func element(for item: MenuItem) -> UIMenuElement {
        if !item.submenu.isEmpty {
            return UIMenu(
                title: item.title,
                image: labelImage(item.icon),
                children: item.submenu.map(element)
            )
        }
        return UIAction(
            title: item.title,
            subtitle: item.subtitle,
            image: item.destructive ? UIImage(systemName: item.icon) : labelImage(item.icon),
            attributes: item.destructive ? .destructive : []
        ) { _ in }
    }

    private static func labelImage(_ name: String) -> UIImage? {
        UIImage(systemName: name)?.withTintColor(.label, renderingMode: .alwaysOriginal)
    }
}

enum SailingMenu {
    static func uiMenu(sailingId: String) -> UIMenu {
        SpecUIMenu.menu(from: GlassControllersKt.sailingMenuSpec(sailingId: sailingId))
    }

    /**
     * When a menu presents upward from a bottom anchor, iOS places the first
     * declared item nearest the anchor — declare reversed so the visual order
     * still reads Share → Delete, like the demo (no Get Pro — the Expo
     * no paywall items).
     */
    @ViewBuilder
    static func menuContent(sailingId: String, bottomAnchored: Bool = false) -> some View {
        if let spec = GlassControllersKt.sailingMenuSpec(sailingId: sailingId) {
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
}

/// One spec item as SwiftUI menu content. A struct rather than a function:
/// submenus recurse, which an opaque-return function cannot.
struct SpecMenuItemView: View {
    let item: MenuItem

    var body: some View {
        if item.submenu.isEmpty {
            Button(role: item.destructive ? .destructive : nil) { } label: {
                Label(item.title, systemImage: item.icon)
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

final class SailingMenuCoordinator: NSObject, UIContextMenuInteractionDelegate {
    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        configurationForMenuAtLocation location: CGPoint
    ) -> UIContextMenuConfiguration? {
        guard let sailingId = GlassControllersKt.sailingMenuSailingIdAt(x: location.x, y: location.y) else {
            return nil
        }
        return UIContextMenuConfiguration(identifier: sailingId as NSString, previewProvider: nil) { _ in
            SailingMenu.uiMenu(sailingId: sailingId)
        }
    }

    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        previewForHighlightingMenuWithConfiguration configuration: UIContextMenuConfiguration
    ) -> UITargetedPreview? {
        guard
            let container = interaction.view,
            let sailingId = configuration.identifier as? String,
            let r = GlassControllersKt.sailingMenuRowRect(sailingId: sailingId)
        else { return nil }
        let rect = CGRect(
            x: CGFloat(r.get(index: 0)),
            y: CGFloat(r.get(index: 1)),
            width: CGFloat(r.get(index: 2)),
            height: CGFloat(r.get(index: 3))
        )
        guard let snapshot = container.resizableSnapshotView(
            from: rect, afterScreenUpdates: false, withCapInsets: .zero
        ) else { return nil }
        let parameters = UIPreviewParameters()
        parameters.visiblePath = UIBezierPath(
            roundedRect: CGRect(origin: .zero, size: rect.size), cornerRadius: 16
        )
        let target = UIPreviewTarget(
            container: container,
            center: CGPoint(x: rect.midX, y: rect.midY)
        )
        return UITargetedPreview(view: snapshot, parameters: parameters, target: target)
    }
}

// Native detail action bar: one rounded bar carrying share / alerts /
// ellipsis on the left and the primary Book Return button on the right,
// floating over the sheet bottom while a sailing is open. The ellipsis
// presents the sailing menu as a real system menu.
struct DetailActionBarView: View {
    let sailingId: String

    /// Teal brand accent, matching PortsidePalette.Accent (0x0E7C7B).
    private let accent = Color(red: 0.851, green: 0.702, blue: 0.416)

    var body: some View {
        HStack {
            barIcon("square.and.arrow.up")
            barIcon("bell.slash")
            Menu {
                SailingMenu.menuContent(sailingId: sailingId, bottomAnchored: true)
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Color(red: 0.914, green: 0.949, blue: 0.937))
                    .frame(width: 37, height: 44)
            }
            Spacer()
            Button { } label: {
                Text("Book Return")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(red: 0.071, green: 0.086, blue: 0.114))
                    .padding(.horizontal, 18)
                    .frame(height: 40)
                    .background(accent, in: Capsule())
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(Color(red: 0.137, green: 0.169, blue: 0.220), in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Color(red: 0.227, green: 0.271, blue: 0.337), lineWidth: 1))
        .shadow(color: .black.opacity(0.15), radius: 8, y: 2)
        .padding(.horizontal, 16)
        .padding(.bottom, 10)
    }

    private func barIcon(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(Color(red: 0.914, green: 0.949, blue: 0.937))
            .frame(width: 37, height: 44)
    }
}

struct ProfileTabView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let anchor = ProfileMenuAnchor()
        let vc = GlassControllersKt.ProfileTabController(profileAnchor: anchor)
        ProfileMenu.attach(to: vc, anchor: anchor)
        vc.view.backgroundColor = spaceBackground
        return vc
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

struct FullComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        GlassControllersKt.FullComposeController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

@available(iOS 26.0, *)
struct GlassContentView: View {
    @State private var selectedTab = 0
    @State private var showAddSailing = false
    @State private var detailSailingId: String? = nil

    var body: some View {
        // The search tab never actually activates — selecting it just
        // presents the Add Sailing sheet over the current tab, so the map
        // stays visible behind the sheet instead of a blank page.
        TabView(selection: Binding(
            get: { selectedTab },
            set: { newValue in
                if newValue == 2 {
                    GlassControllersKt.presentAddSailing()
                } else {
                    selectedTab = newValue
                }
            }
        )) {
            Tab("My Voyages", systemImage: "ferry", value: 0) {
                SailingsTabView(onDetailShown: { id in
                    withAnimation { detailSailingId = id }
                })
                .ignoresSafeArea()
                // The demo replaces the tab bar with the detail action bar
                // while a sailing is open.
                .toolbarVisibility(
                    (detailSailingId == nil && !showAddSailing) ? .automatic : .hidden,
                    for: .tabBar
                )
                .overlay(alignment: .bottom) {
                    if let sailingId = detailSailingId {
                        DetailActionBarView(sailingId: sailingId)
                    }
                }
            }
            Tab("Profile", systemImage: "person.crop.circle", value: 1) {
                ProfileTabView().ignoresSafeArea()
                    .toolbarVisibility(showAddSailing ? .hidden : .automatic, for: .tabBar)
            }
            Tab("Search", systemImage: "magnifyingglass", value: 2, role: .search) {
                // Never shown: selection is intercepted above.
                Color.clear
            }
        }
        .tabBarMinimizeBehavior(.automatic)
        .tint(.blue)
        // The hosting layer must never show systemBackground white: during
        // sheet presentation there is one frame where both Compose canvases
        // are blank and whatever is behind them fills the screen.
        .background(Color(red: 0.016, green: 0.024, blue: 0.047).ignoresSafeArea())
        // The Add Sailing sheet lives inside the Compose canvas (see
        // GlassControllers.kt) — hide the tab bar while it is up.
        .onAppear {
            GlassControllersKt.setAddSailingVisibilityListener { visible in
                withAnimation { showAddSailing = visible.boolValue }
            }
        }
    }
}

struct RootView: View {
    var body: some View {
        if #available(iOS 26.0, *) {
            GlassContentView()
        } else {
            // Older iOS: the full-Compose app, unchanged.
            FullComposeView().ignoresSafeArea()
        }
    }
}

@main
struct GlassApp: App {
    init() {
        // Same reason as the TabView background: the window itself is the
        // last fallback layer during presentation transitions.
        UIWindow.appearance().backgroundColor = UIColor(
            red: 0.016, green: 0.024, blue: 0.047, alpha: 1
        )
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
