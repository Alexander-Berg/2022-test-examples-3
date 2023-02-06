package ru.yandex.market.checkout.pushapi.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.checkout.common.mock.WireMockServerFactory;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;

@Configuration
public class TestMockConfig {

    @Bean
    public LogbrokerClientFactory logbrokerClientFactory() {
        return Mockito.mock(LogbrokerClientFactory.class);
    }

    @Bean
    public MemCachedAgentMockFactory memCachedAgentMockFactory() {
        return new MemCachedAgentMockFactory();
    }

    @Primary
    @Bean
    public MemCachedAgent memCachedAgent(MemCachedAgentMockFactory factory) {
        return factory.createMemCachedAgentMock();
    }

    @Bean
    public Tvm2 tvm2() {
        return Mockito.mock(Tvm2.class, Mockito.RETURNS_MOCKS);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer shopadminStubMock() {
        return WireMockServerFactory.newServer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer checkouterMock() {
        return WireMockServerFactory.newServer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer abcMock() {
        return WireMockServerFactory.newServer();
    }

    @Bean(initMethod = "start")
    public WireMockServer svnMock() {
        return WireMockServerFactory.newServer();
    }
}
