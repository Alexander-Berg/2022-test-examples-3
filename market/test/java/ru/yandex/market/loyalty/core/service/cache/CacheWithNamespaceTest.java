package ru.yandex.market.loyalty.core.service.cache;

import org.junit.Test;

import ru.yandex.market.loyalty.core.config.CacheForTests;
import ru.yandex.market.loyalty.core.mock.ClockForTests;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CacheWithNamespaceTest {

    @Test
    public void shouldDecodeKeysForBulkQuery() {
        final Cache cache = new CacheWithNamespace(
                new CacheForTests(new ClockForTests()), "test");

        cache.getBulk(Arrays.asList("key1", "key2"), (keys) -> {
            assertThat(keys, containsInAnyOrder("key1", "key2"));
            return Collections.emptyList();
        }, 1, cache);
    }

    @Test
    public void shouldEncodeKeyBeforeStoringValueInCache() {
        final Cache rawCache = mock(Cache.class);
        doAnswer(invocation -> {
            final String key = invocation.getArgument(0, String.class);
            assertEquals("test_key1", key);
            return null;
        }).when(rawCache).setNewValue(anyString(), any(), anyInt());
        final Cache cache = new CacheWithNamespace(rawCache, "test");
        cache.setNewValue("key1", new MemcachedCacheTest.CacheKey("key1", "value"), 1);
        verify(rawCache, times(1)).setNewValue(anyString(), any(), any());
    }
}
