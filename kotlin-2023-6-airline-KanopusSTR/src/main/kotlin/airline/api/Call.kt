package airline.api

import kotlinx.datetime.Instant

sealed class Call {
    data class DelayFlightNotification(
        val set: MutableSet<Pair<String, String>>,
        val flightId: String,
        val departureTime: Instant,
        val actualDepartureTime: Instant,
    ) : Call()

    data class CancelFlightNotification(
        val set: MutableSet<Pair<String, String>>,
        val flightId: String,
    ) : Call()

    data class SetCheckInNumberNotification(
        val set: MutableSet<Pair<String, String>>,
        val flightId: String,
        val newCheckInNumber: String,
    ) : Call()

    data class SetGateNumberNotification(
        val set: MutableSet<Pair<String, String>>,
        val flightId: String,
        val newGateNumber: String,
    ) : Call()
}
