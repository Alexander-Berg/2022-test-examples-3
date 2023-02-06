package ru.yandex.market.sc.test.network.repository.manual

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import ru.yandex.market.sc.test.network.data.manual.route.ShipRouteRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualRouteRepository @Inject constructor(
    private val sortingCenterManualService: SortingCenterManualService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun shipRoute(
        routeId: Long,
        cellId: Int,
        comment: String = "",
        force: Boolean = false,
        orderShipped: String,
        placeShipped: String? = null,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.shipRoute(
            id = routeId,
            request = ShipRouteRequest(cellId, comment, force, orderShipped, placeShipped),
        )
    }
}