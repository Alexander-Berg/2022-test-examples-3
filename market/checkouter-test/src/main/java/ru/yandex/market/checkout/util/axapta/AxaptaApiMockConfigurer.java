package ru.yandex.market.checkout.util.axapta;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.log.CheckouterLogs;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static ru.yandex.market.checkout.checkouter.log.LoggingSystem.ACTUALIZATION;

@TestComponent
public class AxaptaApiMockConfigurer {
    private static final Logger LOG = CheckouterLogs.getMainLog(ACTUALIZATION, AxaptaApiMockConfigurer.class);

    public static final String POST_REFUND_ACCEPT = "RefundAccept";
    public static final String REFUND_ACCEPT_URL = "/Refund/Accept";

    @Autowired
    private WireMockServer axaptaMock;

    public void mockAcceptRefund() {
        MappingBuilder builder = post(urlPathMatching(REFUND_ACCEPT_URL))
                .withName(POST_REFUND_ACCEPT)
                .willReturn(ok());
        axaptaMock.stubFor(builder);
    }

    public void resetAll() {
        axaptaMock.resetAll();
    }
}
