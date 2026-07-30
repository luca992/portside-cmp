package portside.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import portside.model.Sailing

/**
 * When a host provides this, sailing rows report their on-screen bounds and
 * suppress the Compose long-press menu: the host presents a native context
 * menu instead (a real UIContextMenuInteraction on the native-chrome iOS
 * app). Bounds are in window coordinates, in pixels.
 */
interface NativeSailingMenuHost {
    fun updateRowBounds(sailing: Sailing, left: Float, top: Float, right: Float, bottom: Float)
    fun removeRow(sailingId: String)
}

val LocalNativeSailingMenuHost = staticCompositionLocalOf<NativeSailingMenuHost?> { null }

/**
 * Same idea for the profile avatar in the screen headers: the host places an
 * invisible native button over the reported bounds whose tap presents the
 * system account menu, and the Compose dropdown is suppressed.
 */
interface NativeProfileMenuHost {
    fun updateAvatarBounds(left: Float, top: Float, right: Float, bottom: Float)
    fun clearAvatar()
}

val LocalNativeProfileMenuHost = staticCompositionLocalOf<NativeProfileMenuHost?> { null }
