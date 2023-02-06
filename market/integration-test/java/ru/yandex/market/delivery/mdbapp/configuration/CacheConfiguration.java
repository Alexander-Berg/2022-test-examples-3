package ru.yandex.market.delivery.mdbapp.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
