package ru.yandex.market.b2b.clients;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockServerConfig {

    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer mockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        mockServer.start();
        return mockServer;
    }
}
