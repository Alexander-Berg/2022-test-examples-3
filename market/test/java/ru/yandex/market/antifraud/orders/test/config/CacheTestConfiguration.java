package ru.yandex.market.antifraud.orders.test.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.antifraud.orders.cache.MemcachedClientStub;
import ru.yandex.market.antifraud.orders.cache.SpyMemCacheManager;
import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;

/**
 * @author dzvyagin
 */
@Configuration
@EnableCaching
public class CacheTestConfiguration {

    @Bean
    @Primary
    public CacheManager memcacheManager() {
        return new SpyMemCacheManager(new MemcachedClientStub(), Duration.ofMinutes(60L));
    }

    @Bean
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES);
    }

    @Bean
    public CacheManager localCacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

    @Bean
    public CacheBuilder cacheBuilder(ConfigurationService configurationService, CacheManager cacheManager) {
        return new CacheBuilder(cacheManager);
    }
}
