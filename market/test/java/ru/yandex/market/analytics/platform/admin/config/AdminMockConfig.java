package ru.yandex.market.analytics.platform.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.qe.passport.blackbox.BlackBoxPassportService;

import static org.mockito.Mockito.mock;

@Configuration
public class AdminMockConfig {

    @Bean
    public BlackBoxPassportService passportService() {
        return mock(BlackBoxPassportService.class);
    }

}
