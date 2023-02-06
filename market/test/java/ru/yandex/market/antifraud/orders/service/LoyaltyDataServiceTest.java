package ru.yandex.market.antifraud.orders.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;

import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.storage.dao.loyalty.LoyaltyDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class LoyaltyDataServiceTest {

    @Mock
    private LoyaltyDao loyaltyDao;
    @Mock
    private GluesService gluesService;

    @SneakyThrows
    @Test
    public void warmupCache() {
        Set<MarketUserId> ids = Set.of(MarketUserId.fromUid(123L), (MarketUserId.fromUid(124L)));
        when(gluesService.getGluedIdsWithCache(123L, true)).thenReturn(CompletableFuture.completedFuture(ids));
        var cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(any())).thenReturn(new NoOpCache("name"));
        LoyaltyDataService dataService =
            new LoyaltyDataService(
                loyaltyDao,
                gluesService,
                new CacheBuilder(cacheManager)
            );
        dataService.warmupCache(123L);
        Thread.sleep(10);
        verify(loyaltyDao).findCoinsUsedByUsers(eq(ids));
    }

    @SneakyThrows
    @Test
    public void findCoinsUsedByUsers() {
        Cache cache = mock(Cache.class);
        var cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(any())).thenReturn(cache);
        LoyaltyDataService dataService =
            new LoyaltyDataService(loyaltyDao, gluesService, new CacheBuilder(cacheManager));
        Set<MarketUserId> ids = Set.of(MarketUserId.fromUid(123L), (MarketUserId.fromUid(124L)));
        when(loyaltyDao.findCoinsUsedByUsers(eq(ids))).thenReturn(Collections.emptyList());
        dataService.findCoinsUsedByUsers(123L, CompletableFuture.completedFuture(ids)).get();
        Thread.sleep(10);
        verify(cache).get(eq(123L));
        verify(cache).put(eq(123L), any(LoyaltyDataService.CoinContainer.class));
    }

}
