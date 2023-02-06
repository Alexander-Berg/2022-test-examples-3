package ru.yandex.market.pricingmgmt.util.configurers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.Stubbing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BlackBoxConfigurer {

    @Autowired
    private lateinit var blackboxApiWireMockServer: Stubbing

    fun mockBlackBoxResponse(resultXml: String) {
        blackboxApiWireMockServer.givenThat(
            WireMock.get(WireMock.urlMatching("/.method=sessionid.*"))
                .willReturn(WireMock.okTextXml(resultXml))
        )
    }
}
