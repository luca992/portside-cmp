package portside.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import portside.model.Sailing
import portside.model.PortsideMenus
import portside.model.MenuSpec
import portside.ui.PortsideColors

/**
 * The long-press context menu on a sailing, mirroring Portside's: share and
 * line actions on top, destructive actions at the bottom. All mock.
 */
@Composable
fun SailingContextMenu(
    sailing: Sailing,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = PortsideColors.MenuGlass,
        shape = RoundedCornerShape(14.dp),
    ) {
        SailingMenuItems(sailing, onDismiss)
    }
}

/**
 * The same menu presented from the detail screen's bottom action bar.
 * DropdownMenu cannot sit flush against a bottom-of-screen anchor — when the
 * flipped-up menu violates its built-in 48dp window margin it falls back to a
 * fixed window-relative position that ignores the anchor entirely — so this
 * hosts the items in a Popup with an exact "bottom sits above the anchor"
 * position provider.
 */
@Composable
fun SailingMenuAboveAnchor(
    sailing: Sailing,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    if (!expanded) return
    val gapPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    // A named class, not an anonymous object: the toolchain's incremental JVM
    // compile occasionally repackaged the enclosing function without its
    // synthetic `$1$1` class, crashing the run with NoClassDefFoundError.
    val positionProvider = remember(gapPx) { AboveAnchorPositionProvider(gapPx) }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        // Mirror DropdownMenu's entrance so the popup swap is invisible,
        // growing from the bottom-left corner where the anchor sits.
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = shown,
            enter = fadeIn(tween(120)) +
                scaleIn(tween(120), initialScale = 0.85f, transformOrigin = TransformOrigin(0f, 1f)),
            exit = fadeOut(tween(90)) + scaleOut(tween(90)),
        ) {
            Surface(
                color = PortsideColors.MenuGlass,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    SailingMenuItems(sailing, onDismiss)
                }
            }
        }
    }
}

/** Pins the popup's bottom edge [gapPx] above the anchor's top edge. */
private class AboveAnchorPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left
            .coerceAtMost(windowSize.width - popupContentSize.width)
            .coerceAtLeast(0)
        val y = (anchorBounds.top - popupContentSize.height - gapPx)
            .coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
private fun SailingMenuItems(sailing: Sailing, onDismiss: () -> Unit) {
    // Items, order, and grouping come from the shared spec, so this menu and
    // the SwiftUI one are the same menu rendered twice.
    MenuSpecItems(PortsideMenus.sailing(sailing), onDismiss)
}

@Composable
internal fun MenuSpecItems(spec: MenuSpec, onDismiss: () -> Unit) {
    spec.sections.forEachIndexed { index, section ->
        if (index > 0) HorizontalDivider(color = PortsideColors.Divider)
        section.items.forEach { item ->
            MenuItem(
                label = item.title,
                icon = sfSymbolIcon(item.icon),
                tint = if (item.destructive) PortsideColors.RedTime else PortsideColors.TextDark,
                trailing = if (item.submenu.isNotEmpty()) AppIcons.Back else null,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: Color = PortsideColors.TextDark,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(text = label, fontSize = 15.sp, color = tint)
        },
        leadingIcon = icon?.let { vector ->
            {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        trailingIcon = trailing?.let { vector ->
            {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = PortsideColors.TextGray,
                    modifier = Modifier.size(14.dp).rotate(180f),
                )
            }
        },
        onClick = onClick,
    )
}
