package ru.yandex.market.jmf.module.relation.test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.relation.ModuleRelationConfiguration;
import ru.yandex.market.jmf.timings.test.TimingTestConfiguration;

@Import({
        ModuleRelationConfiguration.class,
        TimingTestConfiguration.class
})
@ComponentScan("ru.yandex.market.jmf.module.relation.test.impl")
@EnableCaching
public class ModuleRelationTestConfiguration {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
