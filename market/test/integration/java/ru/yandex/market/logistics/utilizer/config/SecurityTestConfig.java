package ru.yandex.market.logistics.utilizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.util.client.tvm.TvmSecurityConfiguration;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.Mockito.mock;

@Import(TvmSecurityConfiguration.class)
public class SecurityTestConfig {

    @Bean
    @Primary
    public TvmClientApi tvmClientApi() {
        return mock(TvmClientApi.class);
    }

    @Bean
    @ConfigurationProperties("tvm.utilizer")
    public TvmTicketChecker tvmTicketChecker() {
        return new TvmTicketCheckerImpl();
    }
}
