package ru.yandex.market.pricingmgmt.util.configurers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.Stubbing
import com.google.common.net.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class DataCampConfigurer {

    @Autowired
    private lateinit var dataCampWireMockServer: Stubbing

    fun mockDataCampResponse(resultStatus: Int, resultJson: String) {
        dataCampWireMockServer.givenThat(
            WireMock.get(WireMock.urlPathEqualTo("/v1/partners/104/offers/"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(resultStatus)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(resultJson)
                )
        )
    }
}
