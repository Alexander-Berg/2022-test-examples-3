package ru.yandex.market.loyalty.core.service.cache;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.BulkFuture;
import org.junit.Test;

import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.trace.Tracer;
import ru.yandex.market.request.trace.Module;

import javax.annotation.Nonnull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class MemcachedCacheTest {

    private static final NamespaceAwareEncoder ENCODER = new NamespaceAwareEncoder() {
        @Nonnull
        @Override
        public String getNamespace() {
            return "";
        }

        @Nonnull
        @Override
        public String encodeKey(@Nonnull String key) {
            return key;
        }

        @Nonnull
        @Override
        public String decodeKey(@Nonnull String key) {
            return key;
        }
    };

    private final MemcachedClient client = mock(MemcachedClient.class);
    private final Cache cache = new MemcachedCache(
            client, new Tracer(Module.MARKET_LOYALTY), mock(PushMonitor.class));
    private final BulkFuture<Map<String, Object>> future = (BulkFuture<Map<String, Object>>) mock(BulkFuture.class);

    @Test
    public void getBulkShouldAddOneObjectToCache() throws Exception {
        when(future.get(anyLong(), any(TimeUnit.class))).thenReturn(
                (Map<String, Object>) (Map<String, ?>) prepareAnswer(3, "from_cache"));
        when(client.asyncGetBulk(anyCollection())).thenReturn(future);
        final Collection<? extends CacheKey> result = cache.getBulk(
                Arrays.asList("key1", "key2", "key2", "key3", "key3", "key4", "key4"),
                (keys) -> keys.stream().map(k -> new CacheKey(k, "raw_" + k)).collect(toList()),
                100,
                cache
        );
        assertThat(result, hasSize(4)); // Вернём все записи из кэша + одну, которая в кэш не попала
        assertThat(result, containsInAnyOrder(
                new CacheKey("key1", "from_cache1"),
                new CacheKey("key2", "from_cache2"),
                new CacheKey("key3", "from_cache3"),
                new CacheKey("key4", "raw_key4")
        ));
        // Должны сохранить в кэше одно значение
        verify(client, times(1)).set(anyString(), anyInt(), any());
    }

    @Test
    public void getBulkShouldNotGoToStorage() throws Exception {
        when(future.get(anyLong(), any(TimeUnit.class))).thenReturn(
                (Map<String, Object>) (Map<String, ?>) prepareAnswer(3, "from_cache"));
        when(client.asyncGetBulk(anyCollection())).thenReturn(future);
        final Collection<? extends CacheKey> result = cache.getBulk(
                Arrays.asList("key1", "key2", "key2", "key3", "key3"),
                (keys) -> {
                    throw new UnsupportedOperationException();
                }, 100,
                cache
        );
        assertThat(result, hasSize(3));
        assertThat(result, containsInAnyOrder(
                new CacheKey("key1", "from_cache1"),
                new CacheKey("key2", "from_cache2"),
                new CacheKey("key3", "from_cache3")
        ));
        verify(client, times(0)).set(anyString(), anyInt(), any());
    }

    private static Map<String, CacheKey> prepareAnswer(int count, String value) {
        final Map<String, CacheKey> answer = new HashMap<>();
        for (int i = 1; i <= count; ++i) {
            final String key = "key" + i;
            answer.put(key, new CacheKey(key, value + i));
        }
        return answer;
    }

    static class CacheKey implements Cacheable, Serializable {
        final String key;
        final String value;

        CacheKey(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Nonnull
        @Override
        public String getCacheKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheKey cacheKey = (CacheKey) o;

            if (!key.equals(cacheKey.key)) {
                return false;
            }
            return value.equals(cacheKey.value);
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }
    }
}
