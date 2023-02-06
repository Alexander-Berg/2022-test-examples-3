package ru.yandex.common.cache.memcached.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.MemCachingTracer;
import ru.yandex.common.cache.memcached.cacheable.BulkMemCacheable;
import ru.yandex.common.util.collections.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DefaultBulkCacherTest {
    BulkMemCacheable cacheable = mock(BulkMemCacheable.class);
    MemCachedAgent agent = mock(MemCachedAgent.class);
    MemCachingTracer tracer = NoOpCachingTracer.INSTANCE;
    BulkCacher cacher = new DefaultBulkCacher(agent, cacheable, new DefaultCacher(agent, cacheable, tracer));

    @Before
    public void setUp() {
        when(cacheable.cacheTime()).thenReturn(1000);
        when(cacheable.key(any())).thenAnswer(invocation -> toKey(invocation.getArgument(0)));
        when(cacheable.queryNonCached(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    private void whenCacheableReturns(Function<Object, Object> queryToValue) {
        when(cacheable.queryNonCachedBulk(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, Collection.class).stream()
                        .map(q -> Pair.of(q, queryToValue.apply(q)))
                        .collect(Utils.toMapWithNulls()));
    }

    static String toKey(Object any) {
        return "key-" + any;
    }

    static String toValue(Object any) {
        return "value-" + any;
    }

    @Test
    public void dontCacheNullsLoadNonNullForMissing() {
        // given
        Object key1 = 1;
        Object key2 = 2;
        Object key3 = 3;
        Function<Object, Object> valueMaker = DefaultBulkCacherTest::toValue;
        whenCacheableReturns(valueMaker);
        when(cacheable.cacheNullValue()).thenReturn(false);
        when(agent.getFromCache(anyCollection())).thenReturn(ImmutableMap.of(
                // no cached value for key1
                toKey(key2), CachedValue.NULL, // null value in cache, probably a glitch
                toKey(key3), "some"
        ));

        // when
        Map result = cacher.queryBulk(Arrays.asList(
                key1,
                key2,
                key3
        ));

        // then
        assertThat(result).isEqualTo(Utils.makeMapWithNulls(
                Pair.of(key1, valueMaker.apply(key1)),
                Pair.of(key2, valueMaker.apply(key2)),
                Pair.of(key3, "some")
        ));
        verify(cacheable).queryNonCachedBulk(Arrays.asList(
                key1,
                key2
        ));
        verify(agent).getFromCache(Sets.newHashSet(
                toKey(key1),
                toKey(key2),
                toKey(key3)
        ));
        verify(agent).putInCache(eq(Utils.makeMapWithNulls(
                Pair.of(toKey(key1), valueMaker.apply(key1)),
                Pair.of(toKey(key2), valueMaker.apply(key2))
        )), notNull());
        verifyNoMoreInteractions(agent);
    }

    @Test
    public void cacheNullsLoadNonNullForMissing() {
        // given
        Object key1 = 1;
        Object key2 = 2;
        Object key3 = 3;
        Function<Object, Object> valueMaker = DefaultBulkCacherTest::toValue;
        whenCacheableReturns(valueMaker);
        when(cacheable.cacheNullValue()).thenReturn(true);
        when(agent.getFromCache(anyCollection())).thenReturn(ImmutableMap.of(
                // no cached value for key1
                toKey(key2), CachedValue.NULL, // null value in cache, probably a glitch
                toKey(key3), "some"
        ));

        // when
        Map result = cacher.queryBulk(Arrays.asList(
                key1,
                key2,
                key3
        ));

        // then
        assertThat(result).isEqualTo(ImmutableMap.of(
                key1, valueMaker.apply(key1),
                key3, "some"
        ));
        verify(cacheable).queryNonCachedBulk(Arrays.asList(
                key1
        ));
        verify(agent).getFromCache(Sets.newHashSet(
                toKey(key1),
                toKey(key2),
                toKey(key3)
        ));
        verify(agent).putInCache(eq(Utils.makeMapWithNulls(
                Pair.of(toKey(key1), valueMaker.apply(key1))
        )), notNull());
        verifyNoMoreInteractions(agent);
    }

    @Test
    public void dontCacheNullsLoadNullForMissing() {
        // given
        Object key1 = 1;
        Object key2 = 2;
        Object key3 = 3;
        whenCacheableReturns(q -> null);
        when(cacheable.cacheNullValue()).thenReturn(false);
        when(agent.getFromCache(anyCollection())).thenReturn(ImmutableMap.of(
                // no cached value for key1
                toKey(key2), CachedValue.NULL, // null value in cache, probably a glitch
                toKey(key3), "some"
        ));

        // when
        Map result = cacher.queryBulk(Arrays.asList(
                key1,
                key2,
                key3
        ));

        // then
        assertThat(result).isEqualTo(ImmutableMap.of(
                key3, "some"
        ));
        verify(cacheable).queryNonCachedBulk(Arrays.asList(
                key1,
                key2
        ));
        verify(agent).getFromCache(Sets.newHashSet(
                toKey(key1),
                toKey(key2),
                toKey(key3)
        ));
        verify(agent).putInCache(eq(Utils.makeMapWithNulls(
                // null values should not be cached
        )), notNull());
        verifyNoMoreInteractions(agent);
    }

    @Test
    public void cacheNullsLoadNullForMissing() {
        // given
        Object key1 = 1;
        Object key2 = 2;
        Object key3 = 3;
        whenCacheableReturns(q -> null);
        when(cacheable.cacheNullValue()).thenReturn(true);
        when(agent.getFromCache(anyCollection())).thenReturn(ImmutableMap.of(
                // no cached value for key1
                toKey(key2), CachedValue.NULL, // null value in cache, probably a glitch
                toKey(key3), "some"
        ));

        // when
        Map result = cacher.queryBulk(Arrays.asList(
                key1,
                key2,
                key3
        ));

        // then
        assertThat(result).isEqualTo(ImmutableMap.of(
                key3, "some"
        ));
        verify(cacheable).queryNonCachedBulk(Arrays.asList(
                key1
        ));
        verify(agent).getFromCache(Sets.newHashSet(
                toKey(key1),
                toKey(key2),
                toKey(key3)
        ));
        verify(agent).putInCache(eq(Utils.makeMapWithNulls(
                Pair.of(toKey(key1), null)
        )), notNull());
        verifyNoMoreInteractions(agent);
    }

    @Test
    public void forceCacheNulls() {
        // given
        BulkCacher cacher = new DefaultBulkCacher(agent, cacheable, new DefaultCacher(agent, cacheable, tracer,
                () -> true),
                () -> true);

        Object key1 = 1;
        Object key2 = 2;
        Object key3 = 3;
        whenCacheableReturns(q -> null);
        when(cacheable.cacheNullValue()).thenReturn(false);
        when(agent.getFromCache(anyCollection())).thenReturn(ImmutableMap.of(
                // no cached value for key1
                toKey(key2), CachedValue.NULL, // null value in cache, probably a glitch
                toKey(key3), "some"
        ));

        // when
        Map result = cacher.queryBulk(Arrays.asList(
                key1,
                key2,
                key3
        ));

        // then
        assertThat(result).isEqualTo(ImmutableMap.of(
                key3, "some"
        ));
        verify(cacheable).queryNonCachedBulk(Arrays.asList(
                key1
        ));
        verify(agent).getFromCache(Sets.newHashSet(
                toKey(key1),
                toKey(key2),
                toKey(key3)
        ));
        verify(agent).putInCache(eq(Utils.makeMapWithNulls(
                Pair.of(toKey(key1), null)
        )), notNull());
        verifyNoMoreInteractions(agent);
    }
}
