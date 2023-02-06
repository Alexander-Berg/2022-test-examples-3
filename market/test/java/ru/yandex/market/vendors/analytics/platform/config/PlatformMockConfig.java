package ru.yandex.market.vendors.analytics.platform.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.vendors.analytics.core.security.AnalyticsTvmClient;
import ru.yandex.market.vendors.analytics.core.service.startrek.StartrekClient;

@Configuration
public class PlatformMockConfig {

    @Bean
    public AnalyticsTvmClient analyticsTvmClient() { return Mockito.mock(AnalyticsTvmClient.class); }
}
