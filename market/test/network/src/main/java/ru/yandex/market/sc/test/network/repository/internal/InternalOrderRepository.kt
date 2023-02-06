package ru.yandex.market.sc.test.network.repository.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.test.data.internal.courier.InternalCourier
import ru.yandex.market.sc.test.network.api.SortingCenterInternalService
import ru.yandex.market.sc.test.network.data.internal.courier.ClientReturnRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InternalOrderRepository @Inject constructor(
    private val sortingCenterInternalService: SortingCenterInternalService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun clientReturn(
        barcode: String,
        returnDate: String,
        sortingCenterId: Long,
        courierId: Long,
        token: String,
    ) = withContext(ioDispatcher) {
        val courierDto = InternalCourier(
            id = courierId,
            name = "Хидео Кадзима",
            carDescription = null,
            carNumber = null,
            companyName = null,
            phone = null
        )
        sortingCenterInternalService.clientReturn(
            ClientReturnRequest(barcode, returnDate, courierDto, sortingCenterId, token)
        )
    }
}
