package ru.yandex.market.antifraud.orders.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.entity.GlueSource;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.external.volva.HttpVolvaClient;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.ITERABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class GluesServiceTest {
    @Mock
    MarketUserIdDao marketUserIdDao;

    @Mock
    HttpVolvaClient volvaClient;

    @Mock
    ConfigurationService configurationService;

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache cache;

    GluesService service;

    @Before
    public void init() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(configurationService.volvaCheckEnabled()).thenReturn(true);
        var cacheBuilder = new CacheBuilder(cacheManager);
        var executor = Executors.newSingleThreadExecutor();
        service = new GluesService(marketUserIdDao, volvaClient, configurationService, cacheBuilder, executor, executor);
    }

    @SneakyThrows
    @Test
    public void cacheRequest() {
        var uid = MarketUserId.fromUid(123L);
        var uuid = MarketUserId.fromUuid("uuid");
        when(volvaClient.getGluedIds(any(), any())).thenReturn(CompletableFuture.completedFuture(Set.of(uid, uuid)));
        var request = GlueRequest.builder()
            .source(GlueSource.ACCURATE)
            .cacheKey(123L)
            .requestId(uuid)
            .acceptType(GluesService.ResultType.UID_ONLY)
            .build();
        assertThat(service.getGluedIds(request))
            .succeedsWithin(10, TimeUnit.MILLISECONDS)
            .isEqualTo(Set.of(uid));

        verify(volvaClient).getGluedIds(List.of(uuid), Set.of());
        verify(cache).get(123L);
        Thread.sleep(1);
        verify(cache).put(eq(123L), any());
    }

    @Test
    public void fastGlue_uidOnly() {
        fastGlueTest(GluesService.ResultType.UID_ONLY, "uid");
    }

    @Test
    public void fastGlue_withUuid() {
        fastGlueTest(GluesService.ResultType.WITH_UUID, "uid", "uuid");
    }

    @Test
    public void fastGlue_requestedTypes() {
        fastGlueTest(GluesService.ResultType.REQUESTED_TYPES, "uid", "yandexuid");
    }

    private void fastGlueTest(GluesService.ResultType acceptType, String... idTypes) {
        var uid = MarketUserId.fromUid(123L);
        var uuid = MarketUserId.fromUuid("uuid");
        var yandexuid = MarketUserId.fromYandexuid("yandexuid");
        when(marketUserIdDao.getGluedIds(anyList())).thenReturn(List.of(uid, uuid, yandexuid));
        var request = GlueRequest.builder()
            .source(GlueSource.FAST)
            .requestId(uid)
            .requestId(yandexuid)
            .acceptType(acceptType)
            .build();
        assertThat(service.getGluedIds(request).join())
            .extracting(MarketUserId::getIdType)
            .containsExactlyInAnyOrder(idTypes);
        verify(marketUserIdDao)
            .getGluedIds(List.of(uid, yandexuid));
    }

    @Test
    public void volva_uidOnly() {
        volvaTest(GluesService.ResultType.UID_ONLY, "uid");
    }

    @Test
    public void volva_withUuid() {
        volvaTest(GluesService.ResultType.WITH_UUID, "uid", "uuid");
    }

    @Test
    public void volva_requestedTypes() {
        volvaTest(GluesService.ResultType.REQUESTED_TYPES, "uid", "yandexuid");
    }

    public void volvaTest(GluesService.ResultType acceptType, String... resultIdTypes) {
        var uid = MarketUserId.fromUid(123L);
        var uuid = MarketUserId.fromUuid("uuid");
        var yandexuid = MarketUserId.fromYandexuid("yandexuid");
        when(volvaClient.getGluedIds(any(), any())).thenReturn(CompletableFuture.completedFuture(Set.of(uuid)));
        var request = GlueRequest.builder()
            .source(GlueSource.ACCURATE)
            .requestId(uid)
            .requestId(yandexuid)
            .acceptType(acceptType)
            .build();
        assertThat(service.getGluedIds(request))
            .succeedsWithin(1, TimeUnit.MILLISECONDS)
            .asInstanceOf(ITERABLE)
            .extracting("idType")
            .containsExactlyInAnyOrder(resultIdTypes);
        verify(volvaClient).getGluedIds(List.of(uid, yandexuid), Set.of(resultIdTypes));
    }
}