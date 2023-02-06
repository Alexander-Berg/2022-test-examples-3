package ru.yandex.market.jmf.utils;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.jmf.common.security.SecurityEventsLogService;

@Configuration
public class MockSecurityEventsLogServiceConfiguration {
    @Bean
    public SecurityEventsLogService securityEventsLogService() {
        return Mockito.mock(SecurityEventsLogService.class);
    }
}
