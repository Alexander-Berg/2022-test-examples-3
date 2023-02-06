package ru.yandex.market.sc.test.network.api

import retrofit2.http.Body
import retrofit2.http.POST
import ru.yandex.market.sc.test.network.data.internal.courier.ClientReturnRequest

/**
 * [Swagger](https://sc-int.tst.vs.market.yandex.net/swagger-ui.html#/)
 */
interface SortingCenterInternalService {
    @POST("courier/clientReturn")
    suspend fun clientReturn(
        @Body clientReturn: ClientReturnRequest,
    )
}