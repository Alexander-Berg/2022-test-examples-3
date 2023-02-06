package ru.yandex.market.vendors.analytics.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author antipov93.
 */

@Configuration
@Profile("functionalTest")
public class CacheConfig {


    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
