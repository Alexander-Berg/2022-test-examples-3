package ru.yandex.market.checkout.util.marketoms;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@TestComponent
public class MarketOmsMockConfigurer {

    @Autowired
    private WireMockServer marketOmsMock;
    @Autowired
    private TestSerializationService testSerializationService;

    public void mockOrdersReserve() {
        MappingBuilder builder = post(urlPathEqualTo("/orders/reserve"))
                .willReturn(okJson(testSerializationService.serializeCheckouterObject(new MultiOrder())));

        marketOmsMock.stubFor(builder);
    }
}
