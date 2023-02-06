package ru.yandex.market.pricelabs.integration.api.programs;

import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ru.yandex.market.pricelabs.cache.CachedDataSource;

@Configuration
public class AutocreationTestConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public CacheManager cacheManager(@Value("${pricelabs.cache.spec}") String spec) {
        var cacheBuilder = CacheBuilder.from(spec);
        return new ConcurrentMapCacheManager(
                CachedDataSource.CACHE_SHOPS,
                CachedDataSource.CACHE_CATEGORIES,
                CachedDataSource.CACHE_CATEGORIES_TREE,
                CachedDataSource.CACHE_BLUE_CATEGORIES,
                CachedDataSource.CACHE_BLUE_CATEGORIES_TREE,
                CachedDataSource.CACHE_BLUE_CATEGORIES_TREE_WITH_OFFERS,
                CachedDataSource.CACHE_CATEGORY_RECOMMENDATION
        ) {
            @Override
            protected Cache createConcurrentMapCache(String name) {
                return new ConcurrentMapCache(name, cacheBuilder.build().asMap(), true);
            }
        };
    }

}
