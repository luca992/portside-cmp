import SwiftUI
import KotlinModules

/// The Add Voyage sheet, route-first: a From/To picker with a swap control,
/// the well-travelled crossings as tappable rows, and the operators as a chip
/// rail — the same departure-board flow as the Compose apps.
struct AddSailingView: View {
    let state: AddSailingUiState
    let onDismiss: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("Add Voyage")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Portside.textDark)
                    Spacer()
                    Button(action: onDismiss) {
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

                portPicker.padding(.top, 14)

                sectionLabel("WELL-TRAVELLED").padding(.top, 18)
                VStack(spacing: 8) {
                    ForEach(Array(state.crossings.enumerated()), id: \.offset) { _, crossing in
                        CrossingRowView(crossing: crossing)
                    }
                }
                .padding(.top, 8)

                sectionLabel("LINES").padding(.top, 18)
                // Pills reflow as whole units — a pill never breaks mid-name.
                ChipFlow(spacing: 8) {
                    ForEach(Array(state.lines.enumerated()), id: \.offset) { _, line in
                        LinePillView(line: line)
                    }
                }
                .padding(.top, 8)

                Spacer(minLength: 20)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
        .scrollIndicators(.hidden)
        .background(Portside.sheetBg.ignoresSafeArea())
    }

    /// From / To picker with a swap control riding the shared edge.
    private var portPicker: some View {
        HStack {
            VStack(alignment: .leading, spacing: 0) {
                portField(label: "FROM", value: "Helsinki · HEL", placeholder: false)
                Rectangle()
                    .fill(Portside.divider)
                    .frame(height: 1)
                    .padding(.leading, 14)
                portField(label: "TO", value: "Choose a port", placeholder: true)
            }
            Image(systemName: "arrow.up.arrow.down")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Portside.accent)
                .frame(width: 34, height: 34)
                .background(Portside.chipBg, in: Circle())
                .padding(.trailing, 12)
        }
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 16))
    }

    private func portField(label: String, value: String, placeholder: Bool) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.system(size: 9))
                .tracking(1)
                .foregroundStyle(Portside.textGray)
            Text(value)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(placeholder ? Portside.textGray : Portside.textDark)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .medium))
            .tracking(1.2)
            .foregroundStyle(Portside.textGray)
    }
}

/// One well-travelled route: codes joined by a tiny ferry mark, duration chip right.
struct CrossingRowView: View {
    let crossing: Crossing

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 8) {
                    Text(crossing.origin.code)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Portside.textDark)
                    Image(systemName: "ferry")
                        .font(.system(size: 11))
                        .foregroundStyle(Portside.accent)
                    Text(crossing.destination.code)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Portside.textDark)
                }
                Text("\(crossing.origin.city) – \(crossing.destination.city)")
                    .font(.system(size: 11))
                    .foregroundStyle(Portside.textGray)
            }
            Spacer()
            Text(crossing.duration + (crossing.overnight ? " · overnight" : ""))
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(Portside.textGray)
                .padding(.horizontal, 9)
                .padding(.vertical, 4)
                .background(Portside.chipBg, in: Capsule())
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(Portside.cardBg, in: RoundedRectangle(cornerRadius: 14))
    }
}

struct LinePillView: View {
    let line: Line

    var body: some View {
        HStack(spacing: 7) {
            LineBadge(line: line, size: 20)
            Text(line.name)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Portside.textDark)
                .fixedSize()
        }
        .padding(.leading, 6)
        .padding(.trailing, 12)
        .padding(.vertical, 5)
        .background(Portside.chipBg, in: Capsule())
    }
}
