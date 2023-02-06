package ru.yandex.market.pers.test.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import ru.yandex.common.cache.memcached.client.MemCachedClient;
import ru.yandex.common.util.collections.Pair;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.11.2021
 */
public class MemCachedClientOverGuava implements MemCachedClient {
    private final Cache<String, Object> cache;

    public MemCachedClientOverGuava(Cache<String, Object> cache) {
        this.cache = cache;
    }

    @Override
    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public Map<String, Object> getMany(Collection<String> keys) {
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
    public void set(String key, Object obj, Date expiry) {
        // do not cache non-serializable items as memcached does not
        if (key == null || obj == null) {
            return;
        }
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("Can't use cache - object is not serializable:" + obj.getClass().getSimpleName());
        }

        try {
            SerializationUtils.serialize((Serializable) obj);
        } catch (SerializationException e) {
            throw new IllegalArgumentException("Can't use cache - serialization failed", e);
        }
        cache.put(key, obj);
    }

    @Override
    public void delete(String key) {
        cache.invalidate(key);
    }

    @Override
    public long incr(String key, long inc) {
        return 0;
    }

    @Override
    public boolean add(String key, Object value, Date expiry) {
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
