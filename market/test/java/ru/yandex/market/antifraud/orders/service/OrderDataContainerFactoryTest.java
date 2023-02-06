package ru.yandex.market.antifraud.orders.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;

import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.external.volva.HttpVolvaClient;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.storage.dao.UnglueDao;
import ru.yandex.market.antifraud.orders.storage.dao.yt.AntifraudYtDao;
import ru.yandex.market.antifraud.orders.storage.dao.yt.CrmPlatformDao;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.AccountState;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.PassportFeatures;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.profiles.Facts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
// Флапает проверка на использование мока при запуске всех тестов класса
@RunWith(MockitoJUnitRunner.Silent.class)
public class OrderDataContainerFactoryTest {

    private static final long MAX_GLUE_ID = 555L;
    @Mock
    private MarketUserIdDao marketUserIdDao;
    @Mock
    private UnglueDao unglueDao;
    @Mock
    private CrmPlatformDao crmPlatformDao;
    @Mock
    private AntifraudYtDao antifraudYtDao;
    @Mock
    private HttpVolvaClient httpVolvaClient;

    @Mock
    private CacheManager cacheManager;
    @Mock
    private ConfigurationService configurationService;

    @Test
    public void prepareContext() throws Exception {

        when(cacheManager.getCache(anyString())).thenReturn(new NoOpCache("stub"));
        when(marketUserIdDao.getGluedIds(anyCollection())).thenReturn(List.of());
        when(marketUserIdDao.getAllMarkersForPuid(anyLong())).thenReturn(Optional.empty());
        when(unglueDao.checkNodeUnglued(any())).thenReturn(Optional.empty());
        when(unglueDao.checkEdgeUnglued(any(), any())).thenReturn(Optional.empty());
        when(configurationService.volvaCheckEnabled()).thenReturn(false);
        when(configurationService.fastGlueEnabled()).thenReturn(true);

        Order order =
            Order.newBuilder().setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(123L).build()).build();

        Cache ordersCache = mock(Cache.class);
        Cache accountStateCache = mock(Cache.class);
        Cache passportFeaturesCache = mock(Cache.class);
        when(cacheManager.getCache(eq("orders_cache"))).thenReturn(ordersCache);
        when(cacheManager.getCache(eq("acc_state_cache"))).thenReturn(accountStateCache);
        when(cacheManager.getCache(eq("pass_ft_cache"))).thenReturn(passportFeaturesCache);

        var executor = Executors.newSingleThreadExecutor();
        var cacheBuilder = new CacheBuilder(cacheManager);
        AccountStateService accountStateService = new AccountStateService(antifraudYtDao, cacheBuilder);
        PassportFeaturesService passportFeaturesService = new PassportFeaturesService(antifraudYtDao, cacheBuilder);
        OrdersService ordersService = new OrdersService(crmPlatformDao, cacheManager, cacheBuilder, executor);
        GluesService gluesService = new GluesService(marketUserIdDao, httpVolvaClient, configurationService, cacheBuilder, executor, executor);
        CheckouterDataService checkouterDataService = new CheckouterDataService(marketUserIdDao, unglueDao, gluesService, configurationService);

        when(ordersCache.get(any())).thenReturn(() -> Facts.newBuilder().addOrder(order).build());
        when(accountStateCache.get(any())).thenReturn(() -> new AccountStateService.AccountStateCacheEntity(AccountState.builder().build()));
        when(passportFeaturesCache.get(any())).thenReturn(() -> new PassportFeaturesService.PassportFeaturesCacheEntity(PassportFeatures.builder().build()));

        when(crmPlatformDao.getCrmOrdersForIds(anyCollection(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(antifraudYtDao.getAccountStateByUid(anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        when(antifraudYtDao.getPassportFeaturesByUid(anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        OrderDataContainerFactory factory = new OrderDataContainerFactory(
            accountStateService,
            passportFeaturesService,
            ordersService,
            checkouterDataService,
            marketUserIdDao,
            executor);
        MultiCartRequestDto request = MultiCartRequestDto.builder()
            .buyer(OrderBuyerRequestDto.builder()
                .uid(123L)
                .uuid("222")
                .build())
            .build();

        OrderDataContainer container = factory.prepareContext(request);
        assertThat(container.getOrderRequest()).isEqualTo(request);
        assertThat(container.getLastOrdersFuture()).isNotNull();
        assertThat(container.getLastOrdersFuture().get()).isNotEmpty();
        assertThat(container.getAccountStateFuture()).isNotNull();
        assertThat(container.getAccountStateFuture().get().isPresent()).isTrue();
        assertThat(container.getPassportFeaturesFuture()).isNotNull();
        assertThat(container.getPassportFeaturesFuture().get().isPresent()).isTrue();
        assertThat(container.getGluedIdsFuture()).isNotNull();
        assertThat(container.getGluedIdsFuture().get()).contains(MarketUserId.fromUid(123L), MarketUserId.fromUuid("222"));
        assertThat(container.getOrders(2L, TimeUnit.SECONDS, null)).contains(order);
        assertThat(container.getUserMarkers()).isNotNull();
        assertThat(container.getUserMarkers().get()).isEmpty();

        Thread.sleep(20);

        List<MarketUserId> newData = Arrays.asList(MarketUserId.fromUid(123L), MarketUserId.fromUuid("222"));

        verify(marketUserIdDao).insertCheckouterData(newData);
        verify(marketUserIdDao).insertNewGlues(newData);
    }

}
