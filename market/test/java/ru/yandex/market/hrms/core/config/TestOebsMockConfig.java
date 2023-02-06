package ru.yandex.market.hrms.core.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(value = "market.hrms.oebs.mock.enabled", matchIfMissing = true)
@Configuration
public class TestOebsMockConfig {

    @Value("${market.hrms.oebs.wiremock.port:0}")
    private int wireMockPort;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer oebsWireMockServer() {
        return new WireMockServer(WireMockConfiguration.options()
                .usingFilesUnderClasspath("oebs")
                .port(wireMockPort));
    }
}
