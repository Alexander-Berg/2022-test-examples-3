package ru.yandex.cs.billing.tms.config;

import java.util.function.Supplier;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.logbroker.config.LogbrokerClusterProperties;
import ru.yandex.market.logbroker.config.Tvm2ClientCredentialProperties;
import ru.yandex.market.logbroker.model.LogbrokerCluster;

@Configuration
public class LbkxLogbrokerExternalTestConfig {
    @Bean
    public Tvm2ClientCredentialProperties clientCredential() {
        return Mockito.mock(Tvm2ClientCredentialProperties.class);
    }

    @Bean
    public LogbrokerClusterProperties lbkxClusterProperties() {
        return Mockito.mock(LogbrokerClusterProperties.class);
    }

    @Bean
    public Supplier<Credentials> tvmCredentialsSupplier() {
        return Mockito.mock(Supplier.class);
    }

    @Bean
    public LogbrokerCluster lbkxCluster() {
        return Mockito.mock(LogbrokerCluster.class);
    }

    @Bean
    public RetryTemplate lbkxRetryTemplate() {
        return Mockito.mock(RetryTemplate.class);
    }
}
