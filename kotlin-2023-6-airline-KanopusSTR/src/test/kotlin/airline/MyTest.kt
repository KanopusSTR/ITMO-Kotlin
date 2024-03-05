package airline

import airline.api.*
import airline.service.EmailService
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MyTest {
    @Test
    fun testManagement() {
        val config = AirlineConfig(
            audioAlertsInterval = 1.seconds,
            displayUpdateInterval = 10.milliseconds,
            ticketSaleEndTime = 1.hours - 1.minutes,
        )
        val airlineApplication = AirlineApplication(config, emailService)
        val plane1 = Plane("A1", setOf("1A", "1B", "2A", "2B"))
        val flightId = "1"
        val flightTime = Clock.System.now() + 1.hours

        testAndCancel {
            launch { airlineApplication.run() }
            sleep()

            val management = airlineApplication.managementService
            val booking = airlineApplication.bookingService

            management.scheduleFlight(flightId, flightTime, plane1)
            sleep(100.milliseconds)
            booking.buyTicket(flightId, flightTime, "1A", "1", "Rynk Artur", "gg@itmo.ru")
            sleep(100.milliseconds)

            checkEmails("gg@itmo.ru", flightId, "1A", "successfully bought")

            management.delayFlight(flightId, flightTime, flightTime + 1.hours)
            sleep()
            checkEmails("gg@itmo.ru", flightId, "delayed")

            management.setGateNumber(flightId, flightTime, "1A")
            sleep()
            checkEmails("gg@itmo.ru", flightId, "gate")

            management.setCheckInNumber(flightId, flightTime, "53")
            sleep()
            checkEmails("gg@itmo.ru", flightId, "check-in")

            management.cancelFlight(flightId, flightTime)
            sleep()
            checkEmails("gg@itmo.ru", flightId, "cancel")
        }
    }

    @Test
    fun testBooking() {
        val config = AirlineConfig(
            audioAlertsInterval = 1.seconds,
            displayUpdateInterval = 100.milliseconds,
            ticketSaleEndTime = 1.hours - 2.seconds,
        )
        val airlineApplication = AirlineApplication(config, emailService)
        val plane1 = Plane("A1", setOf("1A", "1B", "2A", "2B"))
        val plane2 = Plane("A2", setOf("1A", "1B", "2A", "2B", "3A", "3B"))
        val flightId1 = "1"
        val flightId2 = "2"
        val flightTime = Clock.System.now() + 1.hours

        testAndCancel {
            launch { airlineApplication.run() }
            sleep()

            val booking = airlineApplication.bookingService
            val management = airlineApplication.managementService

            management.scheduleFlight(flightId1, flightTime, plane1)
            management.scheduleFlight(flightId2, flightTime, plane2)
            sleep()

            assertContains(booking.flightSchedule, FlightShortInfo(flightId1, flightTime))
            assertContains(booking.flightSchedule, FlightShortInfo(flightId2, flightTime))

            booking.buyTicket(flightId1, flightTime, "1A", "1", "Rynk Artur", "gg@itmo.ru")
            sleep()

            checkEmails("gg@itmo.ru", flightId1, "1A", "successfully bought")
            Assertions.assertEquals(setOf("1B", "2A", "2B"), booking.freeSeats(flightId1, flightTime))

            booking.buyTicket(flightId1, flightTime, "1A", "1", "Petrov Petr", "test@example.com")
            sleep()

            checkEmails("test@example.com", flightId1, "1A", "seat is busy")
            Assertions.assertEquals(setOf("1B", "2A", "2B"), booking.freeSeats(flightId1, flightTime))

            booking.buyTicket(flightId1, flightTime, "3A", "1", "Rynk Artur", "gg@itmo.ru")
            sleep()

            checkEmails("gg@itmo.ru", flightId1, "this plane doesn't have this seat number")
            Assertions.assertEquals(setOf("1B", "2A", "2B"), booking.freeSeats(flightId1, flightTime))

            booking.buyTicket(flightId2, flightTime, "1A", "1", "Rynk Artur", "gg@itmo.ru")
            sleep()

            checkEmails("gg@itmo.ru", flightId2, "successfully bought")
            Assertions.assertEquals(setOf("1B", "2A", "2B", "3A", "3B"), booking.freeSeats(flightId2, flightTime))

            management.cancelFlight(flightId2, flightTime)

            booking.buyTicket(flightId2, flightTime, "1A", "2", "Any Person", "test@hello.com")
            sleep()
            checkEmails("test@hello.com", "this flight is cancelled")

            sleep(3.seconds)
            Assertions.assertEquals(0, booking.flightSchedule.size)

            booking.buyTicket(flightId1, flightTime, "1A", "2", "Ivan Petrov", "ipetrov@mail.ru")
            sleep()
            checkEmails("ipetrov@mail.ru", "time is ended")

            Assertions.assertEquals(0, booking.flightSchedule.size)
            Assertions.assertEquals(setOf("1B", "2A", "2B"), booking.freeSeats(flightId1, flightTime))
        }
    }

    @Test
    fun testInformationDisplay() {
        val config = AirlineConfig(
            displayUpdateInterval = 100.milliseconds,
        )
        val airlineApplication = AirlineApplication(config, emailService)
        val plane1 = Plane("A1", setOf("1A", "1B", "2A", "2B"))
        val flightId = "1"
        val flightTime = Clock.System.now() + 1.hours

        testAndCancel {
            launch { airlineApplication.run() }
            sleep()

            val management = airlineApplication.managementService
            val display = airlineApplication.airportInformationDisplay(this)

            management.scheduleFlight(flightId, flightTime, plane1)
            sleep(200.milliseconds)
            assertContains(display, FlightShortInfo(flightId, flightTime))

            management.delayFlight(flightId, flightTime, flightTime + 1.minutes)
            sleep(200.milliseconds)
            assertContains(
                display,
                FlightShortInfo(flightId, flightTime, actualDepartureTime = flightTime + 1.minutes),
                actualTime = true,
            )

            management.setCheckInNumber(flightId, flightTime, "checkin")
            sleep(200.milliseconds)
            assertContains(
                display,
                FlightShortInfo(
                    flightId,
                    flightTime,
                    checkInNumber = "checkin",
                    actualDepartureTime = flightTime + 1.minutes,
                ),
                actualTime = true,
                checkIn = true,
            )

            management.setGateNumber(flightId, flightTime, "gate")
            sleep(200.milliseconds)
            assertContains(
                display,
                FlightShortInfo(
                    flightId,
                    flightTime,
                    checkInNumber = "checkin",
                    gateNumber = "gate",
                    actualDepartureTime = flightTime + 1.minutes,
                ),
                actualTime = true,
                checkIn = true,
                gate = true,
            )

            management.cancelFlight(flightId, flightTime)
            sleep(200.milliseconds)

            assertContains(
                display,
                FlightShortInfo(
                    flightId,
                    flightTime,
                    isCancelled = true,
                ),
                cancellation = true,
            )
        }
    }

    @Test
    fun testAudioAlerts() {
        val config = AirlineConfig(
            audioAlertsInterval = 1.seconds,
            registrationOpeningTime = 3.hours,
            registrationClosingTime = 40.minutes,
            boardingOpeningTime = 35.minutes,
            boardingClosingTime = 20.minutes,
        )
        val airlineApplication = AirlineApplication(config, emailService)

        val plane = Plane("A1", setOf("1A", "1B", "2A", "2B"))
        val flightId = "1"
        val flightTime = Clock.System.now() + config.registrationOpeningTime
        val flightId2 = "2"
        val flightTime2 = Clock.System.now() + config.registrationClosingTime + 2.minutes
        val flightId3 = "3"
        val flightTime3 = Clock.System.now() + config.boardingOpeningTime
        val flightId4 = "4"
        val flightTime4 = Clock.System.now() + config.boardingClosingTime + 2.minutes

        testAndCancel {
            launch { airlineApplication.run() }
            sleep()

            val management = airlineApplication.managementService
            val audioAlerts = airlineApplication.airportAudioAlerts

            management.scheduleFlight(flightId, flightTime, plane)
            management.scheduleFlight(flightId2, flightTime2, plane)
            management.scheduleFlight(flightId3, flightTime3, plane)
            management.scheduleFlight(flightId4, flightTime4, plane)
            management.setCheckInNumber(flightId, flightTime, "checkin1")
            management.setCheckInNumber(flightId2, flightTime2, "checkin2")
            management.setGateNumber(flightId3, flightTime3, "gate3")
            management.setGateNumber(flightId4, flightTime4, "gate4")

            sleep()

            val previousEvent = mutableMapOf<String, Instant>()
            fun assertEventsInterval(flightId: String) {
                val now = Clock.System.now()
                previousEvent[flightId]?.let { p ->
                    Assertions.assertTrue(now - p in 1.seconds..2.seconds)
                }
                previousEvent[flightId] = now
            }

            audioAlerts.take(10).collect {
                when (it) {
                    is AudioAlerts.RegistrationOpen -> {
                        Assertions.assertEquals("1", it.flightNumber)
                        Assertions.assertEquals("checkin1", it.checkInNumber)
                        assertEventsInterval(it.flightNumber)
                    }

                    is AudioAlerts.RegistrationClosing -> {
                        Assertions.assertEquals("2", it.flightNumber)
                        Assertions.assertEquals("checkin2", it.checkInNumber)
                        assertEventsInterval(it.flightNumber)
                    }

                    is AudioAlerts.BoardingOpened -> {
                        Assertions.assertEquals("3", it.flightNumber)
                        Assertions.assertEquals("gate3", it.gateNumber)
                        assertEventsInterval(it.flightNumber)
                    }

                    is AudioAlerts.BoardingClosing -> {
                        Assertions.assertEquals("4", it.flightNumber)
                        Assertions.assertEquals("gate4", it.gateNumber)
                        assertEventsInterval(it.flightNumber)
                    }
                }
            }
        }
    }

    private fun testAndCancel(block: suspend CoroutineScope.() -> Unit) {
        try {
            runBlocking(Dispatchers.Default) {
                block()
                cancel()
            }
        } catch (ignore: CancellationException) {
        }
    }

    private suspend fun sleep(interval: Duration = 50.milliseconds) {
        delay(interval)
    }

    private data class FlightShortInfo(
        val flightId: String,
        val departureTime: Instant,
        val checkInNumber: String? = null,
        val gateNumber: String? = null,
        val actualDepartureTime: Instant? = null,
        val isCancelled: Boolean? = null,
    )

    private fun List<FlightInfo>.short(
        checkIn: Boolean = false,
        gate: Boolean = false,
        actualTime: Boolean = false,
        cancellation: Boolean = false,
    ) = map {
        FlightShortInfo(
            it.flightId,
            it.departureTime,
            it.checkInNumber.takeIf { checkIn },
            it.gateNumber.takeIf { gate },
            it.actualDepartureTime.takeIf { actualTime },
            it.isCancelled.takeIf { cancellation },
        )
    }.toSet()

    private fun assertContains(
        flights: List<FlightInfo>,
        shortInfo: FlightShortInfo,
        checkIn: Boolean = false,
        gate: Boolean = false,
        actualTime: Boolean = false,
        cancellation: Boolean = false,
    ) {
        val actual = flights.short(checkIn, gate, actualTime, cancellation)
        Assertions.assertTrue(shortInfo in actual) {
            "expected: <$shortInfo> in display departing flights, but it actual <$actual>"
        }
    }

    private fun assertContains(
        display: StateFlow<InformationDisplay>,
        shortInfo: FlightShortInfo,
        checkIn: Boolean = false,
        gate: Boolean = false,
        actualTime: Boolean = false,
        cancellation: Boolean = false,
    ) {
        assertContains(display.value.departing, shortInfo, checkIn, gate, actualTime, cancellation)
    }

    private class InChannelEmailService : EmailService {
        val messages = ConcurrentHashMap<String, Channel<String>>()

        override suspend fun send(to: String, text: String) {
            messages.computeIfAbsent(to) { Channel(100) }
            messages[to]?.send(text)
        }
    }

    private var emailService = InChannelEmailService()

    @BeforeEach
    fun initEmailService() {
        emailService = InChannelEmailService()
    }

    private suspend fun checkEmails(email: String, vararg text: String) {
        val serviceText = emailService.messages[email]?.receive() ?: Assertions.fail("No such email for $email")
        for (part in text) {
            Assertions.assertTrue(part in serviceText) { "expected <$part> in <$serviceText> of email message" }
        }
    }
}
