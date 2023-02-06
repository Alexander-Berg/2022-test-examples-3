package ru.yandex.market.common.test.mockito;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.cacheable.BulkMemCacheable;
import ru.yandex.common.cache.memcached.cacheable.MemCacheable;

/**
 * Мок-реализация {@link MemCachingService}, использующая кэширование данных.
 * Используется в функциональных тестах.
 * Не поддерживает expiry.
 *
 * @author serenitas
 * @deprecated используйте {@link MemCachedClientFactoryMock} для честного поведения кеша (не считая expiry).
 * Поведение этого класса (уже) расходится с тем, который будет в полноценном сервисе.
 */
@Deprecated
public class MemCachedServiceMock implements MemCachingService {

    private Map<String, Object> cache = new HashMap<>();

    @Override
    public synchronized <T, Q> T query(MemCacheable<T, Q> memCacheable, Q q) {
        String key = memCacheable.key(q);
        if (cache.containsKey(key)) {
            Object value = cache.get(key);
            return (value != null || memCacheable.cacheNullValue()) ? (T) value : memCacheable.queryNonCached(q);
        } else {
            T value = memCacheable.queryNonCached(q);
            if (value != null || memCacheable.cacheNullValue()) {
                cache.put(key, value);
            }
            return value;
        }
    }

    @Override
    public synchronized  <T, Q> Map<Q, T> queryBulk(BulkMemCacheable<T, Q> bulkMemCacheable, Collection<Q> collection) {
        Map<Q, T> result = new HashMap<>();
        Collection<Q> nonCachedQueries = new HashSet<>();

        collection.forEach(q -> {
            if (cache.containsKey(bulkMemCacheable.key(q))) {
                result.put(q, (T) cache.get(bulkMemCacheable.key(q)));
            } else {
                nonCachedQueries.add(q);
            }
        });

        bulkMemCacheable.queryNonCachedBulk(nonCachedQueries)
                .forEach((q, t) -> {
                    cache(bulkMemCacheable, q, t);
                    result.put(q, t);
                });

        return result;

    }

    @Override
    public synchronized  <T, Q> void clean(MemCacheable<T, Q> memCacheable, Q q) {
        cache.remove(memCacheable.key(q));
    }

    @Override
    public synchronized  <T, Q> void cache(MemCacheable<T, Q> memCacheable, Q query, T result) {
        cache.put(memCacheable.key(query), result);
    }

    public synchronized void cleanAll() {
        cache.clear();
    }
}
