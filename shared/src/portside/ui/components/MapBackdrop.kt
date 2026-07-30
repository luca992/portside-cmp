package portside.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import portside.model.Sailing
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * The backdrop behind the sheet, matching Portside's two moods:
 * - Sailing detail → a real MapLibre vector map with the route (Android/iOS;
 *   desktop MapLibre support is still early, so it keeps the globe).
 * - Tabs → the orthographic globe seen from space, with the live route.
 */
@Composable
fun MapBackdrop(
    sailing: Sailing?,
    detail: Boolean,
    mapHeightFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (detail && sailing != null && hasNativeRouteMap) {
        // The native map view only mounts once the detail entrance animation
        // has settled: its surface creation and style load land on the UI
        // thread, and mounting mid-transition visibly janks the slide-in
        // (worst on Android). The globe keeps rendering underneath meanwhile.
        var mountMap by remember(sailing.id) { mutableStateOf(false) }
        LaunchedEffect(sailing.id) {
            delay(500.milliseconds)
            mountMap = true
        }
        if (mountMap) {
            NativeRouteMap(sailing, mapHeightFraction, modifier)
        } else {
            OceanBackdrop(sailing, mapHeightFraction, modifier)
        }
    } else {
        OceanBackdrop(sailing, mapHeightFraction, modifier)
    }
}
