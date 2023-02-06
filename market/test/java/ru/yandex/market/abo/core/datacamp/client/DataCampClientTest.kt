package ru.yandex.market.abo.core.datacamp.client

import org.apache.http.impl.client.CloseableHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * @author i-shunkevich
 * @date 25.11.2021
 */
class DataCampClientTest {

    private val dataCampUrl: String = "http://datacamp.blue.vs.market.yandex.net"
    private val httpClient: CloseableHttpClient = mock()

    private val dataCampClient: DataCampClient = DataCampClient(dataCampUrl, httpClient)

    @Test
    fun buildUrl() {
        val url = dataCampClient.buildUrl("/v1/partners/111111/offers",
            mapOf(
                "offer_id" to setOf("23423", "FFFF"),
                "shop_id" to emptySet(),
                "result_status" to setOf(1, "2")
            )
        )
        val expected = "http://datacamp.blue.vs.market.yandex.net" +
                "/v1/partners/111111/offers?offer_id=23423&offer_id=FFFF&result_status=1&result_status=2"
        assertEquals(expected, url)
    }
}
