package ru.yandex.market.fulfillment.stockstorage.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.util.client.tvm.TvmSecurityConfiguration;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(TvmSecurityConfiguration.class)
public class SecurityTestConfig {

    @Bean
    @Primary
    public TvmClientApi tvmClientApi() {
        return mock(TvmClientApi.class);
    }

    @Bean
    @ConfigurationProperties("stockstorage.tvm")
    public TvmTicketChecker tvmTicketChecker() {
        return new TvmTicketCheckerImpl();
    }

    @Bean
    public TvmClient tvmClient() {
        TvmClient tvmClient = mock(TvmClient.class);
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        return tvmClient;
    }
}
