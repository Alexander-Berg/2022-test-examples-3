package ru.yandex.market.ext.marketplace.integrator.app.stocks

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.retrofit.ExecuteCall
import ru.yandex.market.common.retrofit.RetryStrategy
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.ext.marketplace.integrator.app.AbstractFunctionalTest
import ru.yandex.mj.generated.client.ozon_seller_api.model.StockInfo
import ru.yandex.mj.generated.client.ozon_seller_api.model.UpdateStocksRequest
import ru.yandex.mj.generated.client.ozon_seller_api.model.UpdateStocksResponse
import java.util.concurrent.CompletableFuture

internal class StocksPusherTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var stocksPusher: StocksPusher

    @Test
    @DbUnitDataSet(before = ["StocksPusherTest.before.csv"])
    fun pushStocks() {
        val executeStub: ExecuteCall<UpdateStocksResponse, RetryStrategy> = getExecuteCallStub()
        Mockito.`when`(
            ozonStocksApiClient.updateStocks(
                any(), any(), any(), any()
            )
        ).thenReturn(executeStub)

        stocksPusher.pushStocks(
            listOf(
                PartnerStockInfo(1, "offer1", 1),
                PartnerStockInfo(2, "offer2.1", 2),
                PartnerStockInfo(2, "offer2.2", 3),
            )
        )

        Mockito.verify(ozonStocksApiClient).updateStocks(
            "22",
            "api_key_2",
            UpdateStocksRequest()
                .stocks(
                    listOf(
                        StockInfo()
                            .offerId("offer2.1")
                            .stock(2),
                        StockInfo()
                            .offerId("offer2.2")
                            .stock(3)
                    )
                ),
            "https://api-seller.ozon.ru/v1/product/import/stocks"
        )
    }

    private fun <T> getExecuteCallStub(): ExecuteCall<T, RetryStrategy> {
        val futureMock = Mockito.mock(CompletableFuture::class.java) as CompletableFuture<T>
        val callMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<T, RetryStrategy>
        Mockito.`when`(callMock.schedule())
            .thenReturn(futureMock)
        return callMock
    }
}
