package ru.yandex.market.fintech.creditbroker.integration.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class IntTestMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer personalMock() {
        return abstractMock();
    }

    private WireMockServer abstractMock() {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration()
                .dynamicPort()
                .maxRequestJournalEntries(90)
                .notifier(new Slf4jNotifier(false));

        return new WireMockServer(wireMockConfiguration);
    }

}
