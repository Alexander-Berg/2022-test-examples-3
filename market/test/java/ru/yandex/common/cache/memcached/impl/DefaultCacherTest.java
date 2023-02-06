package ru.yandex.common.cache.memcached.impl;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.MemCachingTracer;
import ru.yandex.common.cache.memcached.cacheable.MemCacheable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DefaultCacherTest {
    MemCacheable cacheable = mock(MemCacheable.class);
    MemCachedAgent agent = mock(MemCachedAgent.class);
    MemCachingTracer tracer = NoOpCachingTracer.INSTANCE;
    Cacher cacher = new DefaultCacher(agent, cacheable, tracer);

    @Before
    public void setUp() {
        when(cacheable.cacheTime()).thenReturn(1000);
        when(cacheable.key(any())).thenAnswer(invocation -> toKey(invocation.getArgument(0)));
    }

    static String toKey(Object any) {
        return "key-" + any;
    }

    @Test
    public void skipNullValuesTest() {
        // given
        when(cacheable.cacheNullValue()).thenReturn(false);

        // when
        cacher.cache(1, null);

        // then
        verifyNoInteractions(agent);
    }

    @Test
    public void cacheNullValuesTest() {
        // given
        when(cacheable.cacheNullValue()).thenReturn(true);
        Object key = 1;
        Object object = new Object();

        // when
        cacher.cache(key, object);

        // then
        verify(agent).putInCache(eq(toKey(key)), eq(object), notNull());
        verifyNoMoreInteractions(agent);
    }
}
