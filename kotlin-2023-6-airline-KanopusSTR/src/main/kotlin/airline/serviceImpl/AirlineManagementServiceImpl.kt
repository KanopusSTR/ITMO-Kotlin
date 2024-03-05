package airline.serviceImpl

import airline.api.ManagementAndBuying
import airline.api.Plane
import airline.service.AirlineManagementService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant

class AirlineManagementServiceImpl(
    private val updatesFlow: MutableSharedFlow<ManagementAndBuying>,
) : AirlineManagementService {
    override suspend fun scheduleFlight(flightId: String, departureTime: Instant, plane: Plane) {
        updatesFlow.emit(ManagementAndBuying.ScheduleFlight(flightId, departureTime, plane))
    }

    override suspend fun delayFlight(flightId: String, departureTime: Instant, actualDepartureTime: Instant) {
        updatesFlow.emit(ManagementAndBuying.DelayFlight(flightId, departureTime, actualDepartureTime))
    }

    override suspend fun cancelFlight(flightId: String, departureTime: Instant) {
        updatesFlow.emit(ManagementAndBuying.CancelFlight(flightId, departureTime))
    }

    override suspend fun setCheckInNumber(flightId: String, departureTime: Instant, checkInNumber: String) {
        updatesFlow.emit(ManagementAndBuying.SetCheckInNumber(flightId, departureTime, checkInNumber))
    }

    override suspend fun setGateNumber(flightId: String, departureTime: Instant, gateNumber: String) {
        updatesFlow.emit(ManagementAndBuying.SetGateNumber(flightId, departureTime, gateNumber))
    }

}
