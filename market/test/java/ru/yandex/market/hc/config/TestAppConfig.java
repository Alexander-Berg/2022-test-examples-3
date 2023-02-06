package ru.yandex.market.hc.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.hc.entity.DegradationConfig;
import ru.yandex.market.hc.service.RateLimitingService;

/**
 * Created by aproskriakov on 9/9/21
 */
@Configuration
@Import({
        CacheTestConfig.class,
        RateLimitingConfig.class,
        RateLimitingService.class,
})
public class TestAppConfig {

    @Bean
    public DegradationConfig defaultDegradationConfig() {
        return DegradationConfig.builder()
                .degradationModes(Collections.singleton(10))
                .updatePeriod(60)
                .build();
    }

}

