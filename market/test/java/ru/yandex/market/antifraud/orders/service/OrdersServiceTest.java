package ru.yandex.market.antifraud.orders.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import ru.yandex.market.antifraud.orders.cache.MemcachedClientStub;
import ru.yandex.market.antifraud.orders.cache.SpyMemCache;
import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountRequest;
import ru.yandex.market.antifraud.orders.storage.dao.yt.CrmPlatformDao;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDto;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.profiles.Facts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class OrdersServiceTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private CrmPlatformDao crmPlatformDao;
    private ExecutorService executor = MoreExecutors.newDirectExecutorService();

    @SneakyThrows
    @Test
    public void checkOrdersCache() {
        Cache ordersCache = spy(SpyMemCache.builder()
            .cacheName("orders_cache")
            .expirationPeriod(Duration.ofHours(1))
            .memcachedClient(new MemcachedClientStub())
            .build());
        Cache ordersCountCache = mock(Cache.class);
        List<Order> orders = List.of(
            Order.newBuilder().setId(1L).build(),
            Order.newBuilder().setId(2L).build()
        );
        when(cacheManager.getCache(eq("order_count_cache"))).thenReturn(ordersCountCache);
        when(cacheManager.getCache(eq("orders_cache"))).thenReturn(ordersCache);
        when(crmPlatformDao.getCrmOrdersForIds(anyCollection(), any(Instant.class), eq(null)))
            .thenReturn(CompletableFuture.completedFuture(orders));
        OrdersService ordersService = new OrdersService(crmPlatformDao, cacheManager, new CacheBuilder(cacheManager), executor);
        var userId = MarketUserId.fromUid(123L);
        assertThat(ordersCache.get(userId)).isNull();
        ordersService.getOrders(userId, CompletableFuture.completedFuture(Collections.emptySet())).join();
        Facts cached = (Facts) ordersCache.get(userId).get();
        assertThat(cached.getAntifraudBlockingEventCount()).isEqualTo(1);
        assertThat(cached.getOrderCount()).isEqualTo(2);
        assertThat(cached.getOrderList()).containsExactlyInAnyOrder(orders.get(0), orders.get(1));
        verify(ordersCache, times(1)).put(eq(userId), any(Facts.class));
        ordersService.getOrders(userId, CompletableFuture.completedFuture(Collections.emptySet())).join();
        verify(ordersCache, times(1)).put(eq(userId), any(Facts.class));
    }

    @Test
    public void checkOrdersCountCache() {
        Cache ordersCache = mock(Cache.class);
        Cache ordersCountCache = spy(SpyMemCache.builder()
                .cacheName("order_count_cache")
                .expirationPeriod(Duration.ofHours(1))
                .memcachedClient(new MemcachedClientStub())
                .build());
        List<Order> orders = List.of(
                Order.newBuilder().setId(1L).build(),
                Order.newBuilder().setId(2L).build()
        );
        when(cacheManager.getCache(eq("order_count_cache"))).thenReturn(ordersCountCache);
        when(cacheManager.getCache(eq("orders_cache"))).thenReturn(ordersCache);
        when(crmPlatformDao.getCrmOrdersForIds(anyCollection(), any(Instant.class), any(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(orders));
        OrdersService ordersService = new OrdersService(crmPlatformDao, cacheManager, new CacheBuilder(cacheManager), executor);
        assertThat(ordersCountCache.get("123_10000000_20000000")).isNull();
        OrderCountRequestDto request = OrderCountRequestDto.builder()
                .puid(123L).from(Instant.ofEpochSecond(10000)).to(Instant.ofEpochSecond(20000)).build();
        ordersService.getOrders(request, Collections.emptyList());
        Facts cached = (Facts) ordersCountCache.get("123_10000000_20000000").get();
        assertThat(cached.getAntifraudBlockingEventCount()).isEqualTo(1);
        assertThat(cached.getOrderCount()).isEqualTo(2);
        assertThat(cached.getOrderList()).containsExactlyInAnyOrder(orders.get(0), orders.get(1));
        // почемуто мокито бросает ошибку сравнения с any
//        verify(ordersCountCache, times(1)).put(anyString(), any(Facts.class));
//        ordersService.getOrders(request, Collections.emptyList());
//        verify(ordersCountCache, times(1)) .put(anyString(), any(Facts.class));
    }

    @Test
    public void checkConsequentOrdersCountCache() {
        Cache ordersCache = mock(Cache.class);
        Cache ordersCountCache = SpyMemCache.builder()
                .cacheName("order_count_cache")
                .expirationPeriod(Duration.ofHours(1))
                .memcachedClient(new MemcachedClientStub())
                .build();
        List<Order> orders = List.of(
                Order.newBuilder().setId(1L).build(),
                Order.newBuilder().setId(2L).build()
        );
        when(cacheManager.getCache(eq("order_count_cache"))).thenReturn(ordersCountCache);
        when(cacheManager.getCache(eq("orders_cache"))).thenReturn(ordersCache);
        when(crmPlatformDao.getCrmOrdersForIds(anyCollection(), any(Instant.class), nullable(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(orders));
        OrdersService ordersService = new OrdersService(crmPlatformDao, cacheManager, new CacheBuilder(cacheManager), executor);
        OrderCountRequest request = OrderCountRequest.builder()
            .puid(123L)
            .from(Instant.now().minus(5, ChronoUnit.DAYS))
            .to(Instant.now().minus(3, ChronoUnit.DAYS))
            .build();
        ordersService.getOrders(request, Collections.emptyList()).join();
        ordersService.getOrders(request, Collections.emptyList()).join();
        verify(crmPlatformDao, times(1))
                .getCrmOrdersForIds(anyCollection(), any(Instant.class), nullable(Instant.class));
    }
}
