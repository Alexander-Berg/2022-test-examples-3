package ru.yandex.market.fulfillment.stockstorage.service.cache;

import java.util.Collections;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;


public class GuavaBatchCacheTest {

    private GuavaBatchCache<String, Integer> batchCache;
    private Cache<String, Integer> guavaCache;

    @BeforeEach
    public void setUp() {
        guavaCache = CacheBuilder.newBuilder().build();
        batchCache = new GuavaBatchCache<>(guavaCache);
    }

    @Test
    public void getAllOnNonEmptyCacheWithObjectsLoading() {
        guavaCache.putAll(ImmutableMap.of("Cat", 1, "Dog", 2));

        BatchCacheLoader<String, Integer> cacheLoader = keys -> {
            if (Iterables.contains(keys, "Bird")) {
                return ImmutableMap.of("Bird", 147);
            } else {
                return Collections.emptyMap();
            }
        };

        Map<String, Integer> actual = batchCache.getAll(Lists.newArrayList("Cat", "Dog", "Bird"), cacheLoader);
        Map<String, Integer> expected = ImmutableMap.of(
                "Cat", 1,
                "Dog", 2,
                "Bird", 147
        );
        assertEquals(expected, actual);

        actual = guavaCache.getAllPresent(Lists.newArrayList("Cat", "Dog", "Bird"));
        assertEquals(expected, actual);
    }

    @Test
    public void getAllOnNonEmptyCacheWithoutObjectsLoading() {
        guavaCache.putAll(ImmutableMap.of("Cat", 1, "Dog", 2));

        BatchCacheLoader<String, Integer> cacheLoader = keys -> Collections.emptyMap();

        Map<String, Integer> actual = batchCache.getAll(Lists.newArrayList("Cat", "Dog", "Bird"), cacheLoader);
        Map<String, Integer> expected = ImmutableMap.of(
                "Cat", 1,
                "Dog", 2
        );
        assertEquals(expected, actual);

        actual = guavaCache.getAllPresent(Lists.newArrayList("Cat", "Dog", "Bird"));
        assertEquals(expected, actual);
    }

    @Test
    public void getAllOnEmptyCacheWithObjectsLoading() {
        BatchCacheLoader<String, Integer> cacheLoader = keys -> {
            if (Iterables.contains(keys, "Bird")) {
                return ImmutableMap.of("Bird", 147);
            } else {
                return Collections.emptyMap();
            }
        };

        Map<String, Integer> actual = batchCache.getAll(Lists.newArrayList("Cat", "Dog", "Bird"), cacheLoader);
        Map<String, Integer> expected = ImmutableMap.of(
                "Bird", 147
        );
        assertEquals(expected, actual);

        actual = guavaCache.getAllPresent(Lists.newArrayList("Cat", "Dog", "Bird"));
        assertEquals(expected, actual);
    }

    @Test
    public void getAllOnNonEmptyCacheWithEmptyQuery() {
        guavaCache.putAll(ImmutableMap.of("Cat", 1, "Dog", 2));
        BatchCacheLoader<String, Integer> cacheLoader = keys -> {
            if (Iterables.contains(keys, "Bird")) {
                return ImmutableMap.of("Bird", 147);
            } else {
                return Collections.emptyMap();
            }
        };

        Map<String, Integer> actual = batchCache.getAll(Collections.emptyList(), cacheLoader);
        assertEquals(Collections.emptyMap(), actual);

        actual = guavaCache.getAllPresent(Lists.newArrayList("Cat", "Dog", "Bird"));
        Map<String, Integer> expected = ImmutableMap.of(
                "Cat", 1,
                "Dog", 2
        );
        assertEquals(expected, actual);
    }
}
