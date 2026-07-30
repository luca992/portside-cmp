package portside.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import portside.model.Sailing

internal actual val hasNativeRouteMap: Boolean = true

@Composable
internal actual fun NativeRouteMap(sailing: Sailing, mapHeightFraction: Float, modifier: Modifier) {
    MaplibreRouteMap(sailing, mapHeightFraction, modifier)
}
