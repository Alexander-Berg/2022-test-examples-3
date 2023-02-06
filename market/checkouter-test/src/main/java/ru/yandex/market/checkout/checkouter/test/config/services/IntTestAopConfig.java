package ru.yandex.market.checkout.checkouter.test.config.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import ru.yandex.market.checkout.application.EventPublisherAspect;

@EnableAspectJAutoProxy
@Configuration
public class IntTestAopConfig {
    @Bean
    public EventPublisherAspect eventPublisherAspect() {
        return new EventPublisherAspect();
    }
}
