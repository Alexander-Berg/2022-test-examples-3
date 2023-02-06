package ru.yandex.market.aliasmaker.cache;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author danfertev
 * @since 26.04.2019
 */
@Ignore
public class EvictionMapTest {
    private EvictionMap<Long, Long> evictionMap;

    @Before
    public void setUp() {
        evictionMap = new EvictionMap<>(CacheBuilder.newBuilder()
                .maximumSize(2)
                .expireAfterWrite(100L, TimeUnit.MILLISECONDS)
                .expireAfterAccess(100L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testNotExpired() throws InterruptedException {
        evictionMap.put(1L, 1L);
        Thread.sleep(10L);
        Assertions.assertThat(evictionMap.get(1L)).isEqualTo(1L);
    }

    @Test
    public void testExpiredAfterAccess() throws InterruptedException {
        evictionMap.put(1L, 1L);
        Thread.sleep(10L);
        Assertions.assertThat(evictionMap.get(1L)).isEqualTo(1L);
        Thread.sleep(100L);
        Assertions.assertThat(evictionMap.get(1L)).isNull();
    }

    @Test
    public void testExpiredAfterWrite() throws InterruptedException {
        evictionMap.put(1L, 1L);
        Thread.sleep(100L);
        Assertions.assertThat(evictionMap.get(1L)).isNull();
    }

    @Test
    public void testNotExpiredAfterWrite() throws InterruptedException {
        evictionMap.put(1L, 1L);
        Thread.sleep(10L);
        evictionMap.put(1L, 2L);
        Thread.sleep(90L);
        Assertions.assertThat(evictionMap.get(1L)).isEqualTo(2L);
    }

    @Test
    public void testMaxSize() throws InterruptedException {
        evictionMap.put(1L, 1L);
        evictionMap.put(2L, 2L);
        evictionMap.put(3L, 3L);
        Assertions.assertThat(evictionMap.size()).isEqualTo(2);
    }
}
