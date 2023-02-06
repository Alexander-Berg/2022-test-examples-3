package ru.yandex.market.logistics.mqm.utils

import org.springframework.cache.CacheManager

fun clearCache(cacheManager: CacheManager) = cacheManager.cacheNames
    .map(cacheManager::getCache)
    .forEach { cache -> cache!!.clear() }
