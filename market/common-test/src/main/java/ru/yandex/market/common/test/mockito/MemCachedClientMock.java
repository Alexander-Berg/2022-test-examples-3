package ru.yandex.market.common.test.mockito;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;

import ru.yandex.common.cache.memcached.client.MemCachedClient;

/**
 * Клиент, который использует in-memory кеш в качестве хранилища.
 * Для функциональных тестов лучше использовать {@link MemCachedClientFactoryMock},
 * а этот класс оставить для юнит-тестов, когда надо точечно проверить содержимое кеша.
 * Не поддерживает expiry.
 * Потокобезоспасен.
 *
 * @apiNote
 * Если вы используете {@link ru.yandex.common.cache.memcached.impl.ParallelMemCachedAgent}
 * или его наследников, для предсказуемости тестов лучше передать
 * в {@link ru.yandex.common.cache.memcached.impl.ParallelMemCachedAgent#setExecutor(Executor)} синхронный,
 * например {@link org.springframework.core.task.SyncTaskExecutor}.
 *
 * Если вы используете {@link ru.yandex.common.cache.memcached.impl.DefaultMemCachingService}
 * или его наследников, для предсказуемости тестов лучше передать
 * в {@link ru.yandex.common.cache.memcached.impl.DefaultMemCachingService#setLocalCacherExecutor(Executor)} синхронный,
 * например {@link org.springframework.core.task.SyncTaskExecutor}.
 */
public class MemCachedClientMock implements MemCachedClient {
    private final ConcurrentMap<String, Object> cache;

    public MemCachedClientMock(ConcurrentMap<String, Object> sharedCache) {
        this.cache = sharedCache;
    }

    /**
     * Хранилище as is. Может пригодиться для ассертов.
     */
    public ConcurrentMap<String, Object> getCache() {
        return cache;
    }

    @Nullable
    @Override
    public Object get(String key) {
        return cache.get(key);
    }

    @Override
    public void set(String key, Object obj, Date expiry) {
        cache.put(key, obj);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public boolean add(String key, Object obj, Date expiry) {
        return cache.putIfAbsent(key, obj) == null;
    }

    @Override
    public long incr(String key, long inc) {
        Object newValue = cache.computeIfPresent(key, (k, v) -> v instanceof Long
                ? Long.valueOf(((Long) v) + inc)
                : v);
        return newValue instanceof Long
                ? ((Long) newValue)
                : -1L;
    }
}
