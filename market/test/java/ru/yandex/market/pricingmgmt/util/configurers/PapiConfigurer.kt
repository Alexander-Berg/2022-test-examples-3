package ru.yandex.market.pricingmgmt.util.configurers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.junit.Stubbing
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.google.common.net.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class PapiConfigurer {

    @Autowired
    private lateinit var papiWireMockServer: Stubbing

    fun mockPapiResponseForAxUpdatePrices(resultStatus: Int, resultXml: String) {
        papiWireMockServer.givenThat(
            WireMock.post(WireMock.urlMatching("/campaigns/\\d+/offers/stored/updates"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(resultStatus)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                        .withBody(resultXml)
                )
        )
    }

    fun verifyPapiRequestBody(expectedBody: String) {
        papiWireMockServer.verify(
            1,
            RequestPatternBuilder.newRequestPattern(
                RequestMethod.POST,
                WireMock.urlMatching("/campaigns/\\d+/offers/stored/updates"))
                .withRequestBody(WireMock.equalToXml(expectedBody))
        )
    }
}
