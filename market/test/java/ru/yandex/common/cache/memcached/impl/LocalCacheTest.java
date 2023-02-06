package ru.yandex.common.cache.memcached.impl;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class LocalCacheTest {
    private LocalCache localCache;

    @Before
    public void setUp() {
        localCache = new LocalCache(300, 100);
    }

    @Test
    public void itMustCacheNullValues() {
        // Given
        localCache.put("key", null);

        // When
        LocalCache.ValueWrapper valueWrapper = localCache.get("key");

        // Then
        assertNull(valueWrapper.getValue());
    }

    @Test
    public void itMustEvictValues() throws InterruptedException {
        // Given
        localCache.put("key", "value");

        // When
        TimeUnit.MILLISECONDS.sleep(500);
        LocalCache.ValueWrapper valueWrapper = localCache.get("key");

        // Then
        assertNull(valueWrapper);
    }
}
