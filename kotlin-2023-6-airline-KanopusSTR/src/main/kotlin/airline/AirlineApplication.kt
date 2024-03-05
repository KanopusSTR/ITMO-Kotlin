package airline

import airline.api.*
import airline.service.AirlineManagementService
import airline.service.BookingService
import airline.service.EmailService
import airline.serviceImpl.AirlineManagementServiceImpl
import airline.serviceImpl.BookingServiceImpl
import airline.serviceImpl.BufferedEmailService
import airline.serviceImpl.PassengerNotificationServiceImpl
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class AirlineApplication(private val config: AirlineConfig, private val emailService: EmailService) {

    private val flights = MutableStateFlow<List<Flight>>(emptyList())

    private val updatesFlow = MutableSharedFlow<ManagementAndBuying>()

    val bookingService: BookingService = BookingServiceImpl(flights, config, updatesFlow)

    val managementService: AirlineManagementService = AirlineManagementServiceImpl(updatesFlow)

    private val bufferedEmailService = BufferedEmailService(emailService)

    private val notificationService = PassengerNotificationServiceImpl(bufferedEmailService)

    @OptIn(FlowPreview::class)
    fun airportInformationDisplay(coroutineScope: CoroutineScope): StateFlow<InformationDisplay> {
        return flights.map {
            val list = mutableListOf<FlightInfo>()
            for (flight in it) {
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
            return@map InformationDisplay(list)
        }.sample(config.displayUpdateInterval).stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            InformationDisplay(
                emptyList(),
            ),
        )
    }

    val airportAudioAlerts: Flow<AudioAlerts>
        get() {
            return flow {
                for (flight in flights.value) {
                    val registrationOpeningTime = flight.actualDepartureTime - config.registrationOpeningTime
                    val registrationClosingTime = flight.actualDepartureTime - config.registrationClosingTime
                    val boardingOpeningTime = flight.actualDepartureTime - config.boardingOpeningTime
                    val boardingClosingTime = flight.actualDepartureTime - config.boardingClosingTime
                    if (Clock.System.now() >= registrationOpeningTime &&
                        Clock.System.now() <= registrationOpeningTime + 3.minutes
                    ) {
                        emit(AudioAlerts.RegistrationOpen(flight.flightId, flight.checkInNumber!!))
                    }
                    if (Clock.System.now() <= registrationClosingTime &&
                        Clock.System.now() >= registrationClosingTime - 3.minutes
                    ) {
                        emit(AudioAlerts.RegistrationClosing(flight.flightId, flight.checkInNumber!!))
                    }
                    if (Clock.System.now() >= boardingOpeningTime &&
                        Clock.System.now() <= boardingOpeningTime + 3.minutes
                    ) {
                        emit(AudioAlerts.BoardingOpened(flight.flightId, flight.gateNumber!!))
                    }
                    if (Clock.System.now() <= boardingClosingTime &&
                        Clock.System.now() >= boardingClosingTime - 3.minutes
                    ) {
                        emit(AudioAlerts.BoardingClosing(flight.flightId, flight.gateNumber!!))
                    }
                }
                delay(config.audioAlertsInterval)
            }
        }

    suspend fun run() {
        coroutineScope {
            launch {
                notificationService.run()
            }
            launch {
                bufferedEmailService.run()
            }
            launch {
                updatesFlow.collect { msg ->
                    when (msg) {
                        is ManagementAndBuying.ScheduleFlight -> scheduleFlight(
                            msg.flightId,
                            msg.departureTime,
                            msg.plane,
                        )

                        is ManagementAndBuying.DelayFlight -> delayFlight(
                            msg.flightId,
                            msg.departureTime,
                            msg.actualDepartureTime,
                        )

                        is ManagementAndBuying.CancelFlight -> cancelFlight(msg.flightId, msg.departureTime)
                        is ManagementAndBuying.SetCheckInNumber -> setCheckInNumber(
                            msg.flightId,
                            msg.departureTime,
                            msg.checkInNumber,
                        )

                        is ManagementAndBuying.SetGateNumber -> setGateNumber(
                            msg.flightId,
                            msg.departureTime,
                            msg.gateNumber,
                        )

                        is ManagementAndBuying.BuyTicket -> tryToBuyTicket(
                            msg.flightId,
                            msg.departureTime,
                            msg.seatNo,
                            msg.passengerId,
                            msg.passengerName,
                            msg.passengerEmail,
                        )
                    }
                }
            }
        }
    }

    private suspend fun scheduleFlight(flightId: String, departureTime: Instant, plane: Plane) {
        val list = flights.value
        for (flight in list) {
            if (flight.flightId == flightId && flight.departureTime == departureTime) {
                return
            }
        }
        flights.emit(list + Flight(flightId, departureTime, plane = plane))
    }

    private suspend fun delayFlight(flightId: String, departureTime: Instant, actualDepartureTime: Instant) {
        val list = mutableListOf<Flight>()
        val names = mutableSetOf<Pair<String, String>>()
        for (flight in flights.value) {
            if (flight.flightId == flightId && flight.departureTime == departureTime) {
                list.add(flight.copy(actualDepartureTime = actualDepartureTime))
                for (ticket in flight.tickets.values) {
                    names.add(Pair(ticket.passengerEmail, ticket.passengerName))
                }
            } else {
                list.add(flight)
            }
        }
        notificationService.send(
            Call.DelayFlightNotification(
                names,
                flightId,
                departureTime,
                actualDepartureTime,
            ),
        )
        flights.emit(list)
    }

    private suspend fun cancelFlight(flightId: String, departureTime: Instant) {
        val names = changeOneField(flightId, departureTime, true) { bool, flight ->
            flight.copy(isCancelled = bool)
        }
        notificationService.send(
            Call.CancelFlightNotification(
                names,
                flightId,
            ),
        )
    }

    private suspend fun setCheckInNumber(flightId: String, departureTime: Instant, checkInNumber: String) {
        val names = changeOneField(flightId, departureTime, checkInNumber) { str, flight ->
            flight.copy(checkInNumber = str)
        }
        notificationService.send(
            Call.SetCheckInNumberNotification(
                names,
                flightId,
                checkInNumber,
            ),
        )
    }

    private suspend fun setGateNumber(flightId: String, departureTime: Instant, gateNumber: String) {
        val names = changeOneField(flightId, departureTime, gateNumber) { gate, flight ->
            flight.copy(gateNumber = gate)
        }
        notificationService.send(
            Call.SetGateNumberNotification(
                names,
                flightId,
                gateNumber,
            ),
        )
    }

    private suspend fun <T> changeOneField(
        flightId: String,
        departureTime: Instant,
        v: T,
        f: (T, Flight) -> Flight,
    ): MutableSet<Pair<String, String>> {
        val names = mutableSetOf<Pair<String, String>>()
        val list = mutableListOf<Flight>()
        for (flight in flights.value) {
            if (flight.flightId == flightId && flight.departureTime == departureTime) {
                for (ticket in flight.tickets.values) {
                    names.add(Pair(ticket.passengerEmail, ticket.passengerName))
                }
                list.add(f(v, flight))
            } else {
                list.add(flight)
            }
        }
        flights.emit(list)
        return names
    }

    private suspend fun tryToBuyTicket(
        flightId: String,
        departureTime: Instant,
        seatNo: String,
        passengerId: String,
        passengerName: String,
        passengerEmail: String,
    ) {
        val list = mutableListOf<Flight>()
        var foundTicket = false

        for (flight in flights.value) {
            if (!foundTicket && flight.flightId == flightId && flight.departureTime == departureTime) {
                foundTicket = true
                if (Clock.System.now() < (flight.actualDepartureTime - config.ticketSaleEndTime)) {
                    if (flight.isCancelled) {
                        bufferedEmailService.send(
                            passengerEmail,
                            "$flightId $seatNo Failed to buy (this flight is cancelled)",
                        )
                    } else if (seatNo !in flight.plane.seats) {
                        bufferedEmailService.send(
                            passengerEmail,
                            "$flightId $seatNo Failed to buy (this plane doesn't have this seat number)",
                        )
                    } else {
                        var found = false
                        for (ticket in flight.tickets) {
                            if (ticket.value.seatNo == seatNo) {
                                found = true
                            }
                        }
                        if (found) {
                            bufferedEmailService.send(passengerEmail, "$flightId $seatNo Failed to buy (seat is busy)")
                        } else {
                            flight.tickets += (
                                seatNo to Ticket(
                                    flightId, departureTime, seatNo, passengerId, passengerName, passengerEmail,
                                )
                                )
                            bufferedEmailService.send(passengerEmail, "$flightId $seatNo successfully bought")
                        }
                    }
                } else {
                    bufferedEmailService.send(passengerEmail, "$flightId $seatNo Failed to buy (time is ended)")
                }
            }
            list.add(flight)
        }
        flights.emit(list)
    }
}
