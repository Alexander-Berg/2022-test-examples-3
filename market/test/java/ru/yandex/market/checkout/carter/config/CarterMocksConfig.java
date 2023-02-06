package ru.yandex.market.checkout.carter.config;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.carter.mock.MockFactory;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.utils.CarterHttpHelper;
import ru.yandex.market.checkout.carter.utils.serialization.TestSerializationService;
import ru.yandex.market.checkout.common.json.jackson.JacksonMessageConverter;
import ru.yandex.market.checkout.common.mock.WireMockServerFactory;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;

import static org.mockito.Mockito.mock;

@Configuration
public class CarterMocksConfig {

    @Bean
    public MemCachedAgentMockFactory memCachedAgentMockFactory() {
        return new MemCachedAgentMockFactory();
    }

    @Bean
    public MemCachedAgent memCachedAgent(MemCachedAgentMockFactory memCachedAgentMockFactory) {
        return memCachedAgentMockFactory.createMemCachedAgentMock();
    }

    @Bean
    public MemCachedServiceConfig memCachedServiceConfig() {
        MemCachedServiceConfig memCachedServiceConfig = new MemCachedServiceConfig();
        memCachedServiceConfig.setServiceName("carter");
        memCachedServiceConfig.setDefaultCacheTime(3600);
        return memCachedServiceConfig;
    }

    @Bean
    @Profile("client-mock")
    public MockFactory mockFactory() {
        return new MockFactory();
    }

    @Bean
    @Profile("client-mock")
    public MockMvc mockMvc(MockFactory mockFactory) {
        return mockFactory.getMockMvc();
    }

    @Bean
    public TestSerializationService serializationService(
            JacksonMessageConverter carterJsonMessageConverter,
            MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter,
            ObjectMapper objectMapper
    ) {
        return new TestSerializationService(List.of(carterJsonMessageConverter,
                mappingJackson2HttpMessageConverter
        ), objectMapper);
    }

    @Bean
    @Profile("client-mock")
    public CarterHttpHelper carterHttpHelper() {
        return new CarterHttpHelper();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer reportMock(WireMockServer reportMockWhite) {
        return reportMockWhite;
    }

    @Qualifier("reportMockWhite")
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer reportMockWhite() {
        return WireMockServerFactory.newServer();
    }

    @Bean
    public ReportMockConfigurer reportMockConfigurer(WireMockServer reportMockWhite) {
        return new ReportMockConfigurer(reportMockWhite);
    }

    @Bean("reportMockConfigurerWhite")
    public ReportMockConfigurer reportMockConfigurerWhite(WireMockServer reportMockWhite) {
        return new ReportMockConfigurer(reportMockWhite);
    }

    @Bean
    public Tvm2 tvm2() {
        return mock(Tvm2.class);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer abcMock() {
        return WireMockServerFactory.newServer();
    }

    @Profile("client-mock")
    @Bean
    public MockMvcClientHttpRequestFactory carterHttpRequestFactory(MockMvc mockMvc) {
        return new MockMvcClientHttpRequestFactory(mockMvc);
    }
}
