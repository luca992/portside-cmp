package portside.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import portside.model.DistanceUnit

/**
 * App settings. A plain shared StateFlow (not persisted — mock app): every
 * renderer observes [distanceUnit] so a change re-renders all distances.
 */
object Settings {
    private val _distanceUnit = MutableStateFlow(DistanceUnit.NauticalMiles)
    val distanceUnit: StateFlow<DistanceUnit> = _distanceUnit

    fun setDistanceUnit(unit: DistanceUnit) {
        _distanceUnit.value = unit
    }
}
