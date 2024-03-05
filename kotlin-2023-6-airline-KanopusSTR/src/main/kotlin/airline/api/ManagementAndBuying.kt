package airline.api

import kotlinx.datetime.Instant

sealed class ManagementAndBuying {
    data class ScheduleFlight(
        val flightId: String,
        val departureTime: Instant,
        val plane: Plane,
    ) : ManagementAndBuying()
    data class CancelFlight(val flightId: String, val departureTime: Instant) : ManagementAndBuying()
    data class DelayFlight(
        val flightId: String,
        val departureTime: Instant,
        val actualDepartureTime: Instant,
    ) : ManagementAndBuying()
    data class SetCheckInNumber(
        val flightId: String,
        val departureTime: Instant,
        val checkInNumber: String,
    ) : ManagementAndBuying()
    data class SetGateNumber(
        val flightId: String,
        val departureTime: Instant,
        val gateNumber: String,
    ) : ManagementAndBuying()

    data class BuyTicket(
        val flightId: String,
        val departureTime: Instant,
        val seatNo: String,
        val passengerId: String,
        val passengerName: String,
        val passengerEmail: String,
    ) : ManagementAndBuying()
}
