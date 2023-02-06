package ru.yandex.market.pvz.core.test;

import java.util.Collection;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pvz.core.domain.configuration.ConfigurationProviderSource;

public class ResetCachesAfterBeforeEachExtension implements BeforeTestExecutionCallback {
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        clearCacheManager(context, "cacheManager");
        clearCacheManager(context, "oneMinuteCacheManager");
        clearCacheManager(context, "oneHourCacheManager");

        var configurationProviderSource = SpringExtension.getApplicationContext(context)
                .getBean("configurationProviderSource", ConfigurationProviderSource.class);
        configurationProviderSource.changeCacheVersion();
    }

    private void clearCacheManager(ExtensionContext context, String cacheManagerBeanName) {
        var cacheManager = SpringExtension.getApplicationContext(context)
                .getBean(cacheManagerBeanName, CacheManager.class);
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            cacheManager.getCache(cacheName).clear();
        }
    }
}
