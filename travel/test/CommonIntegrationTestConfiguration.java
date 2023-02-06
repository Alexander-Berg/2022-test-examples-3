package ru.yandex.travel.orders.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonIntegrationTestConfiguration {
    @Bean
    public TestsTxHelper txHelper() {
        return new TestsTxHelper();
    }
}
