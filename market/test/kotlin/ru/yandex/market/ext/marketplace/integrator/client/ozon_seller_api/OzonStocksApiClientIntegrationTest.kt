package ru.yandex.market.ext.marketplace.integrator.client.ozon_seller_api

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.ext.marketplace.integrator.app.settings.ozon.OzonIntegrationEntity
import ru.yandex.market.ext.marketplace.integrator.app.settings.ozon.OzonIntegrationSettingsRepository
import ru.yandex.market.ext.marketplace.integrator.app.stocks.clients.OzonStocksClientProvider
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.mj.generated.client.ozon_seller_api.api.OzonStocksApiClient
import ru.yandex.mj.generated.client.ozon_seller_api.api.OzonStocksApiConfig
import ru.yandex.mj.generated.client.ozon_seller_api.model.StockInfo
import ru.yandex.mj.generated.client.ozon_seller_api.model.UpdateStocksRequest
import ru.yandex.mj.generated.client.ozon_seller_api.model.UpdateStocksResponse

@Disabled
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SpringApplicationConfig::class, OzonStocksApiConfig::class])
@ActiveProfiles(profiles = ["local"])
@TestPropertySource(
    properties = [
        "client_secret=tvm_secret",
        "ozon.seller_api.url=https://api-seller.ozon.ru"
    ]
)
class OzonStocksApiClientIntegrationTest {

    @Autowired
    private lateinit var ozonClient: OzonStocksApiClient

    @Autowired
    private lateinit var ozonStocksClientProvider: OzonStocksClientProvider

    @Autowired
    private lateinit var settingsRepository: OzonIntegrationSettingsRepository

    private val clientId: Long = 0
    private val apiKey: String = "api key"

    @Test
    fun updateStocksByApiClient() {
        val result: UpdateStocksResponse = ozonClient.updateStocks(
            clientId.toString(),
            apiKey,
            UpdateStocksRequest()
                .addStocksItem(
                    StockInfo()
                        .offerId("14840822")
                        .stock(7)
                ),
            "https://api-seller.ozon.ru/v1/product/import/stocks"
        ).schedule().get()
        println(result)
    }

    @Test
    fun updateStocksByClient() {
        val partnerId = 12345L

        settingsRepository.upsertSettings(
            OzonIntegrationEntity(
                partnerId = partnerId,
                clientId = clientId,
                apiKey = apiKey,
                enabled = true
            )
        )

        val client = ozonStocksClientProvider.getClient(partnerId)
        val result = client?.updateStocks(
            UpdateStocksRequest()
                .addStocksItem(
                    StockInfo()
                        .offerId("14840822")
                        .stock(6)
                )
        )
        println(result)
    }
}
