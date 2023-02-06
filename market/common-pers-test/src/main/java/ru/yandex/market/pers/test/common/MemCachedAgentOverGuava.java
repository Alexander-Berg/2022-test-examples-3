package ru.yandex.market.pers.test.common;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.util.collections.Pair;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.09.2018
 */
public class MemCachedAgentOverGuava implements MemCachedAgent {
    private final Cache<String, Object> cache;

    public MemCachedAgentOverGuava(Cache<String, Object> cache) {
        this.cache = cache;
    }

    @Override
    public Object getFromCache(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public Map<String, Object> getFromCache(Collection<String> keys) {
        return keys.stream()
                .map(x -> new Pair<>(x, cache.getIfPresent(x)))
                .filter(x -> x.second != null)
                .collect(Collectors.toMap(
                        Pair::getFirst,
                        Pair::getSecond,
                        (x, y) -> y
                ));
    }

    @Override
    public void putInCache(String key, Object result, Date expiry) {
        if (key == null || result == null) {
            return;
        }
        cache.put(key, result);
    }

    @Override
    public void putInCache(String key, Object result) {
        if (key == null || result == null) {
            return;
        }
        cache.put(key, result);
    }

    @Override
    public void deleteFromCache(String key) {
        if (key == null) {
            return;
        }
        cache.invalidate(key);
    }

    @Override
    public long incrementInCache(String key, Date expiry) {
        return incrementInCache(key, 1, expiry);
    }

    @Override
    public long incrementInCache(String key, long inc, Date expiry) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean addInCache(String key, Object value, Date expiry) {
        if (key == null || value == null) {
            return false;
        }
        if (cache.getIfPresent(key) == null) {
            cache.put(key, value);
            return true;
        }
        return false;
    }
}
