package ru.yandex.market.tpl.core;

import java.util.Collection;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class CleanupCachesAfterEachExtension implements AfterEachCallback {
    @Override
    public void afterEach(ExtensionContext context) {
        clearCacheManager(context, "cacheManager");
        clearCacheManager(context, "oneMinuteCacheManager");
        clearCacheManager(context, "oneHourCacheManager");
        clearCacheManager(context, "oneDayCacheManager");
    }

    private void clearCacheManager(ExtensionContext context, String cacheManagerBeanName) {
        var oneDayCacheManager  = SpringExtension.getApplicationContext(context).getBean(cacheManagerBeanName, CacheManager.class);
        Collection<String> cacheNames = oneDayCacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            oneDayCacheManager.getCache(cacheName).clear();
        }
    }

}
