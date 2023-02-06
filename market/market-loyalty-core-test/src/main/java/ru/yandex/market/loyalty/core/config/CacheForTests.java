package ru.yandex.market.loyalty.core.config;

import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.loyalty.core.service.cache.Cache;
import ru.yandex.market.loyalty.core.service.cache.CacheValueSetter;
import ru.yandex.market.loyalty.core.service.cache.Cacheable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class CacheForTests implements Cache {
    private final Map<String, Pair<Object, Long>> cache;
    private final Clock clock;
    private final AtomicInteger cacheCalls = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);

    public CacheForTests(Clock clock) {
        this.cache = new ConcurrentHashMap<>();
        this.clock = clock;
    }

    @Override
    public <T extends Serializable> void setNewValue(@Nonnull String key, @Nullable T newValue,
                                                     ToIntFunction<T> ttlSupplier) {
        this.cache.put(key, Pair.of(newValue, clock.millis()));
    }

    @Override
    public void reset(String key) {
        this.cache.remove(key);
    }

    public void clear() {
        this.cache.clear();
        cacheCalls.set(0);
        cacheHits.set(0);
    }

    public int getCacheCalls() {
        return cacheCalls.get();
    }

    public int getCacheHits() {
        return cacheHits.get();
    }

    @Override
    public <T extends Serializable> T get(String key, Supplier<? extends T> valueSupplier,
                                          ToIntFunction<T> ttlSupplier) {
        cacheCalls.incrementAndGet();
        if (isKeyAbsentOrExpired(key, ttlSupplier)) {
            setNewValue(key, valueSupplier.get(), ttlSupplier);
        } else {
            cacheHits.incrementAndGet();
        }
        @SuppressWarnings("unchecked")
        T ret = (T) this.cache.get(key).getLeft();
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T extends Serializable & Cacheable> Collection<T> getBulk(
            @Nonnull Collection<String> keys,
            @Nonnull Function<Collection<String>, Collection<T>> valueFunction,
            ToIntFunction<T> ttlSupplier,
            @Nonnull CacheValueSetter valueSetter
    ) {
        cacheCalls.incrementAndGet();
        final List<T> result = new ArrayList<>();
        final Set<String> missedKeys = new HashSet<>();
        for (String key : keys) {
            if (isKeyAbsentOrExpired(key, ttlSupplier)) {
                missedKeys.add(key);
            } else {
                final T ret = (T) this.cache.get(key).getLeft();
                result.add(ret);
            }
        }

        if (missedKeys.isEmpty()) {
            cacheHits.incrementAndGet();
        } else {
            final Collection<T> readValues = valueFunction.apply(missedKeys);
            readValues.forEach(v -> {
                valueSetter.setNewValue(v.getCacheKey(), v, ttlSupplier);
                result.add(v);
            });
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> boolean isKeyAbsentOrExpired(String key, ToIntFunction<T> ttlSupplier) {
        return !this.cache.containsKey(key) ||
                getTimetamp(key) + getTtl(key, ttlSupplier) < clock.millis();
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> long getTtl(String key, ToIntFunction<T> ttlSupplier) {
        return TimeUnit.SECONDS.toMillis(ttlSupplier.applyAsInt((T) cache.get(key).getLeft()));
    }

    private Long getTimetamp(String key) {
        return this.cache.get(key).getRight();
    }
}
