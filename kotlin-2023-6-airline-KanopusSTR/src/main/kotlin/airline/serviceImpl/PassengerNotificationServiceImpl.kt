package airline.serviceImpl

import airline.api.Call
import airline.service.EmailService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel

class PassengerNotificationServiceImpl(private val emailService: EmailService) {

    private val channel = Channel<Call>()

    suspend fun send(call: Call) {
        channel.send(call)
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run() {
        while (!channel.isClosedForReceive) {
            when (val call = channel.receive()) {
                is Call.DelayFlightNotification -> {
                    for (man in call.set) {
                        emailService.send(
                            man.first,
                            "Dear, ${man.second}. Unfortunately, your flight ${call.flightId} delayed " +
                                "from ${call.departureTime} to ${call.actualDepartureTime}.",
                        )
                    }
                }

                is Call.CancelFlightNotification -> {
                    for (man in call.set) {
                        emailService.send(
                            man.first,
                            "Dear, ${man.second}. Unfortunately, your flight ${call.flightId} was cancelled.",
                        )
                    }
                }

                is Call.SetCheckInNumberNotification -> {
                    for (man in call.set) {
                        emailService.send(
                            man.first,
                            "Dear, ${man.second}. Unfortunately, your flight ${call.flightId} " +
                                "check-in Number was change to ${call.newCheckInNumber}.",
                        )
                    }
                }

                is Call.SetGateNumberNotification -> {
                    for (man in call.set) {
                        emailService.send(
                            man.first,
                            "Dear, ${man.second}. Unfortunately, your flight ${call.flightId} " +
                                "gate Number was change to ${call.newGateNumber}.",
                        )
                    }
                }
            }
        }
    }
}
