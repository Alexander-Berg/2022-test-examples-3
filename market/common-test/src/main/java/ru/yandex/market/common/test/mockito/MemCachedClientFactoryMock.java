package ru.yandex.market.common.test.mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ru.yandex.common.cache.memcached.client.MemCachedClient;
import ru.yandex.common.cache.memcached.client.MemCachedClientFactory;

/**
 * Создаёт клиенты, которые шарят между собой in-memory кеш.
 * Потокобезопасен.
 *
 * @apiNote Также см очень важную заметку к {@link MemCachedClientMock}.
 */
public class MemCachedClientFactoryMock implements MemCachedClientFactory, AutoCloseable {
    // обычная мапа, тк инициализация бинов как правило
    private ConcurrentMap<String, ConcurrentMap<String, Object>> sharedCaches = new ConcurrentHashMap<>();

    @Override
    public void close() {
        sharedCaches.values().forEach(Map::clear);
    }

    @Override
    public MemCachedClient newClient(@Nullable String clientId, List<String> servers) {
        // мы имитируем серверный кеш, которому все равно на clientId, поэтому его не подмешиваем в cacheId
        ConcurrentMap<String, Object> sharedCache = sharedCaches.computeIfAbsent(
                servers.stream()
                        .map(StringUtils::trimToEmpty)
                        .sorted()
                        .distinct()
                        .collect(Collectors.joining(";")),
                cacheId -> new ConcurrentHashMap<>()
        );
        return new MemCachedClientMock(sharedCache);
    }

    /**
     * Хранилище as is. Может пригодиться для ассертов.
     */
    public ConcurrentMap<String, ConcurrentMap<String, Object>> getCaches() {
        return sharedCaches;
    }
}
