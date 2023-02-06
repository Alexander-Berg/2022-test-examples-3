package ru.yandex.market.tpl.core.util;

import java.util.List;

import org.springframework.cache.CacheManager;

public class CacheTestUtil {

    public static void clear(CacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(it -> cacheManager.getCache(it).clear());
    }

    public static void clear(List<CacheManager> cacheManagers) {
        cacheManagers.forEach(CacheTestUtil::clear);
    }

}
