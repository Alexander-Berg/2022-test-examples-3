package ru.yandex.market.api.util.cache;

import io.netty.util.concurrent.Future;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;

import java.nio.file.Paths;
import java.time.Clock;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
public class CommonFuturedCacheTest extends UnitTestBase {

    private static final long FOREVER = Long.MAX_VALUE;

    @Mock
    Clock clock;

    @Mock
    Function<Integer, Future<Integer>> service;

    @Test
    public void shouldCacheValue() throws Exception {
        CommonFuturedCache<Integer, Integer> cache = getCacheWithExpiredTime(FOREVER);

        when(service.apply(eq(1))).thenReturn(Futures.newSucceededFuture(1));

        CommonFuturedCache.Result<Integer> result1 = Futures.waitAndGet(cache.get(1, service));
        CommonFuturedCache.Result<Integer> result2 = Futures.waitAndGet(cache.get(1, service));

        assertEquals(CommonFuturedCache.Status.ACTUAL, result1.getStatus());
        assertEquals(Integer.valueOf(1), result1.getValue());

        assertEquals(CommonFuturedCache.Status.ACTUAL, result2.getStatus());
        assertEquals(Integer.valueOf(1), result2.getValue());

        verify(service, times(1)).apply(eq(1));
    }

    @Test
    public void shouldProcessNullValue() throws Exception {
        CommonFuturedCache<Integer, Integer> cache = getCacheWithExpiredTime(FOREVER);

        when(service.apply(eq(2))).thenReturn(Futures.newSucceededFuture(null));

        CommonFuturedCache.Result<Integer> result1 = Futures.waitAndGet(cache.get(2, service));
        CommonFuturedCache.Result<Integer> result2 = Futures.waitAndGet(cache.get(2, service));

        assertEquals(CommonFuturedCache.Status.ACTUAL, result1.getStatus());
        assertEquals(null, result1.getValue());

        assertEquals(CommonFuturedCache.Status.ACTUAL, result2.getStatus());
        assertEquals(null, result2.getValue());

        verify(service, times(1)).apply(eq(2));
    }

    @Test
    public void shouldRenewValueIfTimeExpired() throws Exception {
        CommonFuturedCache<Integer, Integer> cache = getCacheWithExpiredTime(100);

        when(service.apply(eq(1))).thenReturn(Futures.newSucceededFuture(1));

        when(clock.millis()).thenReturn(0L);
        CommonFuturedCache.Result<Integer> result1 = Futures.waitAndGet(cache.get(1, service));

        when(clock.millis()).thenReturn(100L);
        CommonFuturedCache.Result<Integer> result2 = Futures.waitAndGet(cache.get(1, service));

        assertEquals(CommonFuturedCache.Status.ACTUAL, result1.getStatus());
        assertEquals(Integer.valueOf(1), result1.getValue());

        assertEquals(CommonFuturedCache.Status.ACTUAL, result2.getStatus());
        assertEquals(Integer.valueOf(1), result2.getValue());

        verify(service, times(2)).apply(eq(1));
    }

    @Test
    public void shouldReturnOldValueOnError() throws Exception {
        CommonFuturedCache<Integer, Integer> cache = getCacheWithExpiredTime(100);

        when(service.apply(eq(1)))
            .thenReturn(Futures.newSucceededFuture(1))
            .thenReturn(Futures.newFailedFuture(new Exception("test")));

        when(clock.millis()).thenReturn(0L);
        CommonFuturedCache.Result<Integer> result1 = Futures.waitAndGet(cache.get(1, service));

        when(clock.millis()).thenReturn(100L);
        CommonFuturedCache.Result<Integer> result2 = Futures.waitAndGet(cache.get(1, service));

        // Тут проверяем, что до истечения таймаута обновления элемент не будет перезапрошен даже при ошибке
        when(clock.millis()).thenReturn(150L);
        CommonFuturedCache.Result<Integer> result3 = Futures.waitAndGet(cache.get(1, service));

        assertEquals(CommonFuturedCache.Status.ACTUAL, result1.getStatus());
        assertEquals(Integer.valueOf(1), result1.getValue());

        assertEquals(CommonFuturedCache.Status.EXPIRED, result2.getStatus());
        assertEquals(Integer.valueOf(1), result2.getValue());

        assertEquals(CommonFuturedCache.Status.EXPIRED, result3.getStatus());
        assertEquals(Integer.valueOf(1), result3.getValue());

        verify(service, times(2)).apply(eq(1));
    }

    @Test
    public void shouldNotFefreshOnError() throws Exception {
        CommonFuturedCache<Integer, Integer> cache = getCacheWithExpiredTime(100);

        when(service.apply(anyInt()))
            .thenReturn(Futures.newFailedFuture(new Exception("test")));

        when(clock.millis()).thenReturn(0L);
        CommonFuturedCache.Result<Integer> result1 = Futures.waitAndGet(cache.get(1, service));

        when(clock.millis()).thenReturn(50L);
        CommonFuturedCache.Result<Integer> result2 = Futures.waitAndGet(cache.get(1, service));

        verify(service, times(1)).apply(eq(1));

        when(clock.millis()).thenReturn(150L);
        CommonFuturedCache.Result<Integer> result3 = Futures.waitAndGet(cache.get(1, service));

        assertEquals(CommonFuturedCache.Status.EXPIRED, result1.getStatus());
        assertEquals(null, result1.getValue());

        assertEquals(CommonFuturedCache.Status.ACTUAL, result2.getStatus());
        assertEquals(null, result2.getValue());

        assertEquals(CommonFuturedCache.Status.EXPIRED, result3.getStatus());
        assertEquals(null, result1.getValue());

        verify(service, times(2)).apply(eq(1));
    }

    private CommonFuturedCache<Integer, Integer> getCacheWithExpiredTime(long expiredTime) {
        return new CommonFuturedCache<>(
            clock,
            100,
            100,
            expiredTime,
            expiredTime,
            Paths.get("test"),
            x -> null,
            x -> null,
            x -> null,
            x -> null
        );

    }
}
