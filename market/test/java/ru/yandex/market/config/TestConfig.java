package ru.yandex.market.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mbi.partner_tvm.tvm_client.FakeTvmClient;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
public class TestConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    WireMockServer staffMockServer() {
        return createWiremockServer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    WireMockServer blackboxMockServer() {
        return createWiremockServer();
    }

    private WireMockServer createWiremockServer() {
        return new WireMockServer(new WireMockConfiguration().dynamicPort().notifier(new ConsoleNotifier(true)));
    }

    @Bean
    TvmClient tvmClient() {
        return new FakeTvmClient();
    }
}
