package ru.yandex.market.antifraud.orders.cache.async;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils;
import ru.yandex.market.antifraud.orders.util.AsyncFunction;
import ru.yandex.market.antifraud.orders.util.concurrent.PendingException;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ComplexAsyncCacheTest {

    private ConcurrentMapCache cache;
    private AsyncFunction<String, Integer> valueSupplier;

    @Before
    public void setUp() {
        cache = spy(new ConcurrentMapCache("test_cache"));
        valueSupplier = mock(AsyncFunction.class);
        RequestContextHolder.setContext(new RequestContext(AntifraudTestUtils.REQUEST_ID));
    }

    @After
    public void tearDown() {
        var keyCaptor = ArgumentCaptor.forClass(Object.class);
        var valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(cache, atLeast(0))
            .put(keyCaptor.capture(), valueCaptor.capture());
        assertThat(keyCaptor.getAllValues())
            .allMatch(ComplexAsyncCacheTest::isSerializable, "be serializable");
        assertThat(valueCaptor.getAllValues())
            .allMatch(ComplexAsyncCacheTest::isSerializable, "be serializable");
    }

    private static boolean isSerializable(Object object) {
        if (Objects.isNull(object)) {
            return true;
        }
        return object.equals(AntifraudJsonUtil.fromJson(AntifraudJsonUtil.toJson(object), object.getClass()));
    }

    @SneakyThrows
    @Test
    public void getValueFromCache() {
        setDelay(0);
        var asyncCache = getCache(1000);
        assertThat(asyncCache.getAsync("key")).succeedsWithin(20, TimeUnit.MILLISECONDS).isEqualTo(1);
        Thread.sleep(1);
        assertThat(cache.get("key", Integer.class)).isEqualTo(1);
        assertThat(asyncCache.getAsync("key")).isCompletedWithValue(1);
        assertThat(cache.get("key", Integer.class)).isEqualTo(1);
        verify(valueSupplier).apply("key");
    }

    @SneakyThrows
    @Test
    public void reloadValueIfDelayHasPassed() {
        setDelay(0);
        var asyncCache = getCache(0);
        assertThat(asyncCache.getAsync("key")).succeedsWithin(5, TimeUnit.MILLISECONDS).isEqualTo(1);
        assertThat(asyncCache.getAsync("key")).isCompletedWithValue(1);
        Thread.sleep(1);
        assertThat(cache.get("key", Integer.class)).isEqualTo(2);
        verify(valueSupplier, times(2)).apply("key");
    }

    @Test
    public void waitForCurrentRequestIfPresent() {
        setDelay(50);
        var asyncCache = getCache(1000);
        var firstRequest = asyncCache.getAsync("key");
        var secondRequest = asyncCache.getAsync("key");
        assertThat(firstRequest).succeedsWithin(55, TimeUnit.MILLISECONDS).isEqualTo(1);
        assertThat(secondRequest).hasFailedWithThrowableThat().isInstanceOf(PendingException.class);
        assertThat(cache.get("key", Integer.class)).isEqualTo(1);
        verify(valueSupplier).apply("key");
    }

    private void setDelay(int delayMs) {
        var it = IntStream.iterate(1, i -> i + 1).iterator();
        when(valueSupplier.apply(any())).thenAnswer(in ->
            CompletableFuture.supplyAsync(it::nextInt, CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)));
    }

    private ComplexAsyncCache<String, String, Integer> getCache(int maxCacheDelayMs) {
        return ComplexAsyncCacheImpl.<String, Integer>simpleBuilder()
            .cache(cache)
            .valueSupplier(valueSupplier)
            .maxCacheDelay(Duration.ofMillis(maxCacheDelayMs))
            .build();
    }
}
