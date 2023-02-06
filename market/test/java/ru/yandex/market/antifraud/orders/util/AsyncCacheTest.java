package ru.yandex.market.antifraud.orders.util;

import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
public class AsyncCacheTest {

    @Test
    public void getAndUpdate() {
        Cache cache = new ConcurrentMapCache("test_cache");
        ExecutorService executor = MoreExecutors.newDirectExecutorService();
        AsyncCache asyncCache = new AsyncCache(cache, executor);

        cache.put("key", 123L);
        Long value = asyncCache.readThroughOrUpdate("key", () -> 125L);
        assertThat(value).isEqualTo(123L);
        assertThat(cache.get("key", Long.class)).isEqualTo(125L);

        Long value2 = asyncCache.readThroughOrUpdate("key2", () -> 126L);
        assertThat(value2).isEqualTo(126L);
    }

    @Test
    public void checkCacheUpdateCondition() {
        Cache cache = mock(Cache.class);
        ExecutorService executor = MoreExecutors.newDirectExecutorService();

        when(cache.get(any())).thenReturn(() -> "value1");

        AsyncCache asyncCache = new AsyncCache(cache, executor);
        String value1 = asyncCache.readThroughOrUpdate("key", () -> "value2", (v) -> false);
        assertThat(value1).isEqualTo("value1");
        verify(cache, never()).put(any(), any());

        String value2 = asyncCache.readThroughOrUpdate("key", () -> "value2", (v) -> true);
        assertThat(value2).isEqualTo("value1");
        verify(cache, times(1)).put(any(), any());
    }
}
