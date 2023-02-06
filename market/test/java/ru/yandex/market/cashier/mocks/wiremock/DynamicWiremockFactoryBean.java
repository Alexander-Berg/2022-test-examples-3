package ru.yandex.market.cashier.mocks.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Extension;

public class DynamicWiremockFactoryBean {

    public static WireMockServer create(Extension... extensions) {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration()
                .dynamicPort()
                .maxRequestJournalEntries(90)
                .notifier(new Slf4jNotifier(false));

        return new WireMockServer(wireMockConfiguration.extensions(extensions));
    }
}
