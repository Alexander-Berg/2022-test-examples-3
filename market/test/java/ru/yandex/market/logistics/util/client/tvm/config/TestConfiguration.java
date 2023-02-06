package ru.yandex.market.logistics.util.client.tvm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.util.client.tvm.TvmSecurityConfiguration;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;

@Configuration
@Import(TvmSecurityConfiguration.class)
public abstract class TestConfiguration {

    @Bean
    @ConfigurationProperties("tvm.test")
    public TvmTicketChecker testChecker() {
        return new TvmTicketCheckerImpl();
    }

}
