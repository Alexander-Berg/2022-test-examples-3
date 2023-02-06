package ru.yandex.market.delivery.mdbapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.delivery.mdbapp.integration.service.OrderDeliveryDateProcessIdProvider;

@Configuration
public class OrderDeliveryDateConfiguration {
    @Bean
    @Primary
    public OrderDeliveryDateProcessIdProvider processIdProvider() {
        return new OrderDeliveryDateProcessIdProvider(() -> "processId");
    }
}
