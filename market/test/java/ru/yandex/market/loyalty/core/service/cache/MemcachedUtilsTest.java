package ru.yandex.market.loyalty.core.service.cache;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MemcachedUtilsTest {

    @Test
    public void isCacheHitWithEmptyResult() {
        final Collection<String> keys = Arrays.asList("key1", "key2");
        assertFalse(MemcachedUtils.isCacheHit(null, Collections.emptyList()));
        assertFalse(MemcachedUtils.isCacheHit(null, keys));

        assertFalse(MemcachedUtils.isCacheHit(Collections.emptyMap(), Collections.emptyList()));
        assertFalse(MemcachedUtils.isCacheHit(Collections.emptyMap(), keys));
    }

    @Test
    public void isCacheHitWithDuplicatedKeys() {
        final Collection<String> keys = Arrays.asList("key1", "key2", "key1");
        final Map<String, String> fromCache = new HashMap<>();
        assertFalse(MemcachedUtils.isCacheHit(fromCache, keys));

        fromCache.put("key1", "value1");
        assertFalse(MemcachedUtils.isCacheHit(fromCache, keys));

        fromCache.put("key2", "value2");
        assertTrue(MemcachedUtils.isCacheHit(fromCache, keys));
    }
}
