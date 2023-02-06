package ru.yandex.market.delivery.transport_manager.config;

import org.springframework.context.annotation.Bean;

import ru.yandex.market.delivery.transport_manager.util.TestSpringScheduler;

public class SpringSchedulerMonitoringConfiguration {
    @Bean
    public TestSpringScheduler testSpringScheduler() {
        return new TestSpringScheduler();
    }
}
