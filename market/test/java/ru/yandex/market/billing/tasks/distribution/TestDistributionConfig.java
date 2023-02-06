package ru.yandex.market.billing.tasks.distribution;

import java.util.Collections;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.mbi.web.converter.JsonHttpMessageConverter;
import ru.yandex.market.request.trace.Module;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("functionalTest")
public class TestDistributionConfig {
    @Bean
    public Supplier<String> distributionTvmTicketProvider() {
        return () -> "fake_tvm_ticket";
    }

    @Bean
    public DistributionClient distributionClient() {
        return mock(DistributionClient.class);
    }

    @Bean
    public DistributionPlaceClient distributionPlaceClient() {
        return mock(DistributionPlaceClient.class);
    }
}
