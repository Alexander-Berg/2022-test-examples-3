package ru.yandex.market.mcrm.utils;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mcrm.utils.security.SecurityEventsLogService;

@Configuration
public class MockSecurityEventsLogServiceConfiguration {
    @Bean
    public SecurityEventsLogService securityEventsLogService() {
        return Mockito.mock(SecurityEventsLogService.class);
    }
}
