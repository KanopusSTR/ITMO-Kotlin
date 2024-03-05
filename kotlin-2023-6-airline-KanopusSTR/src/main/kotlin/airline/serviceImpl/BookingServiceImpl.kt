package airline.serviceImpl

import airline.api.AirlineConfig
import airline.api.Flight
import airline.api.FlightInfo
import airline.api.ManagementAndBuying
import airline.service.BookingService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class BookingServiceImpl(
    private var flights: MutableStateFlow<List<Flight>>,
    private val config: AirlineConfig,
    private val updatesFlow: MutableSharedFlow<ManagementAndBuying>,
) : BookingService {

    override val flightSchedule: List<FlightInfo>
        get() {
            val flightsList: List<Flight> = flights.value
            val list = mutableListOf<FlightInfo>()
            for (flight in flightsList) {
                if (!flight.isCancelled &&
                    (Clock.System.now() < (flight.actualDepartureTime - config.ticketSaleEndTime))
                ) {
                    list.add(
                        FlightInfo(
                            flight.flightId,
                            flight.departureTime,
                            flight.isCancelled,
                            flight.actualDepartureTime,
                            flight.checkInNumber,
                            flight.gateNumber,
                            flight.plane,
                        ),
                    )
                }
            }
            return list
        }

    override fun freeSeats(flightId: String, departureTime: Instant): Set<String> {
        for (flight in flights.value) {
            val tickets = flight.tickets.values
            if (flight.flightId == flightId && flight.departureTime == departureTime) {
                return flight.plane.seats.minus(tickets.map { it.seatNo }.toSet())
            }

        }
        return emptySet()
    }

    override suspend fun buyTicket(
        flightId: String,
        departureTime: Instant,
        seatNo: String,
        passengerId: String,
        passengerName: String,
        passengerEmail: String,
    ) {
        updatesFlow.emit(
            ManagementAndBuying.BuyTicket(flightId, departureTime, seatNo, passengerId, passengerName, passengerEmail),
        )
    }

}
