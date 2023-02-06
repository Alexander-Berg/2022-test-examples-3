package ru.yandex.market.pricelabs;

import java.util.Collection;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Сбрасывает кэши Спринга перед каждым тестовым методом
 */
@Slf4j
public class SpringCacheResetListener implements TestExecutionListener {

    private Collection<CacheManager> cacheManagers;

    @Override
    public void beforeTestClass(TestContext testContext) {
        this.cacheManagers = testContext.getApplicationContext().getBeansOfType(CacheManager.class).values();
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        for (var cacheManager : cacheManagers) {
            for (String cacheName : cacheManager.getCacheNames()) {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            }
        }
    }
}
