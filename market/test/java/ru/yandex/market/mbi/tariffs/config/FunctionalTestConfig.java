package ru.yandex.market.mbi.tariffs.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.tariffs.mvc.interceptor.MockAuthInterceptor;
import ru.yandex.market.mbi.tariffs.service.MockSecManager;
import ru.yandex.market.security.SecManager;

/**
 * Основной конфиг для тестов
 */
@Configuration
public class FunctionalTestConfig {
    /**
     * Время обновления/создания данных в тестах.
     * Если ты его поменяешь, придется менять тесты.
     */
    private static final LocalDateTime TEST_UPDATE_TIME = LocalDateTime.of(2020, 10, 1, 10, 0, 0);

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        final var configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setOrder(-1);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean
    public Clock clock() {
        return Clock.fixed(DateTimes.toInstantAtDefaultTz(TEST_UPDATE_TIME), ZoneId.systemDefault());
    }

    @Bean
    public SecManager secManager() {
        return new MockSecManager();
    }

    @Bean
    public Tvm2 moduleTvm() {
        return Mockito.mock(Tvm2.class);
    }

    @Bean
    public HandlerInterceptor authInterceptor() {
        return new MockAuthInterceptor();
    }
}
