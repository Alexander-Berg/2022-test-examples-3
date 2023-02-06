package ru.yandex.market.delivery.mdbapp.configuration;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityExecutorFactory;

@Configuration
public class CapacityExecutorFactoryConfig {
    @Bean
    @Primary
    CapacityExecutorFactory getThisThreadExecutor() {
        return new CapacityExecutorFactory() {
            @Override
            public Executor createExecutor() {
                return Runnable::run;
            }
        };
    }
}
