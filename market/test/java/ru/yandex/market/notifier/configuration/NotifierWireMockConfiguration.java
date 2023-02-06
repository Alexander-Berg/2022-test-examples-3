package ru.yandex.market.notifier.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotifierWireMockConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer pushApiMock() {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration()
                .dynamicPort()
                .maxRequestJournalEntries(90)
                .notifier(new Slf4jNotifier(false));

        return new WireMockServer(wireMockConfiguration);
    }
}
