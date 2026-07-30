package portside.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import portside.model.Sailing

internal actual val hasNativeRouteMap: Boolean = false

@Composable
internal actual fun NativeRouteMap(sailing: Sailing, mapHeightFraction: Float, modifier: Modifier) {
    // Never reached: hasNativeRouteMap berths all call sites.
}
