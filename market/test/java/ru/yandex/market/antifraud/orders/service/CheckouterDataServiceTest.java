package ru.yandex.market.antifraud.orders.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;

import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.cache.serialization.JsonSerializer;
import ru.yandex.market.antifraud.orders.entity.CheckouterRequestData;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.external.volva.HttpVolvaClient;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.storage.dao.UnglueDao;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdType;
import ru.yandex.market.antifraud.orders.storage.entity.unglue.UnglueNode;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerDeviceIdRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerUserDeviceRequestDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author kateleb
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckouterDataServiceTest {

    private CheckouterDataService service;
    @Mock
    private MarketUserIdDao marketUserIdDao;
    @Mock
    private UnglueDao unglueDao;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private HttpVolvaClient httpVolvaClient;
    @Mock
    private ConfigurationService configurationService;

    @Before
    public void init() {
        when(cacheManager.getCache(anyString())).thenReturn(new NoOpCache("stub"));
        var cacheBuilder = new CacheBuilder(cacheManager);
        var executor = Executors.newSingleThreadExecutor();
        service = new CheckouterDataService(marketUserIdDao, unglueDao,
            new GluesService(marketUserIdDao, httpVolvaClient, configurationService, cacheBuilder, executor, executor),
            configurationService);
    }

    @SneakyThrows
    @Test
    public void testNoActionsForAssessor() {
        OrderBuyerRequestDto buyer =
                OrderBuyerRequestDto.builder()
                        .uid(123L)
                        .uuid("222")
                        .yandexuid("yauid")
                        .assessor(true)
                        .build();

        Set<MarketUserId> users = service.getGluedUsers(buyer).get();
        boolean saved = service.saveNewCheckouterData(users, buyer);
        verifyNoMoreInteractions(marketUserIdDao);
        verifyNoMoreInteractions(unglueDao);
        Assert.assertFalse("No new data should be inserted!", saved);
    }

    @SneakyThrows
    @Test
    public void testNoActionsForNullUid() {
        OrderBuyerRequestDto buyer =
                OrderBuyerRequestDto.builder()
                        .uid(null)
                        .uuid("222")
                        .yandexuid("yauid")
                        .assessor(null)
                        .build();

        Set<MarketUserId> users = service.getGluedUsers(buyer).get();
        boolean saved = service.saveNewCheckouterData(users, buyer);
        Assert.assertFalse("No new data should be inserted!", saved);
    }

    @SneakyThrows
    @Test
    public void testNoActionsForUnglue() {
        OrderBuyerRequestDto buyer =
                OrderBuyerRequestDto.builder()
                        .uid(1234L)
                        .uuid("222")
                        .yandexuid("yauid")
                        .assessor(false)
                        .build();

        when(unglueDao.checkNodeUnglued(any()))
                .thenReturn(Optional.empty());
        when(unglueDao.checkNodeUnglued(MarketUserId.fromUid(1234L)))
                .thenReturn(Optional.of(UnglueNode.builder().id(5L).nodeValue("1234").nodeType(BuyerIdType.UID).build()));
        Set<MarketUserId> users = service.getGluedUsers(buyer).get();
        boolean saved = service.saveNewCheckouterData(users, buyer);
        verify(unglueDao, times(2)).checkNodeUnglued(MarketUserId.fromUid(1234L));
        verifyNoMoreInteractions(marketUserIdDao);
        Assert.assertFalse("No new data should be inserted!", saved);
    }

    @SneakyThrows
    @Test
    public void testFullCycleNewData() {
        OrderBuyerRequestDto buyer =
                OrderBuyerRequestDto.builder()
                        .uid(1234L)
                        .uuid("222")
                        .yandexuid("yauid")
                        .userDevice(OrderBuyerUserDeviceRequestDto.builder()
                                .deviceId(OrderBuyerDeviceIdRequestDto.builder().iosDeviceId("1").build())
                                .build())
                        .assessor(false)
                        .build();

        List<MarketUserId> ids = List.of(
                MarketUserId.fromUid(1234L),
                MarketUserId.fromYandexuid("yauid"),
                MarketUserId.fromUuid("222"),
                MarketUserId.fromDeviceId("1"));

        when(unglueDao.checkNodeUnglued(any())).thenReturn(Optional.empty());
        when(unglueDao.checkEdgeUnglued(any(), any())).thenReturn(Optional.empty());

        when(marketUserIdDao.getGluedIds(ids)).thenReturn(new ArrayList<>());
        Set<MarketUserId> users = service.getGluedUsers(buyer).get();
        boolean saved = service.saveNewCheckouterData(users, buyer);
        verify(unglueDao, times(2)).checkNodeUnglued(MarketUserId.fromUid(1234L));
        verify(unglueDao, times(2)).checkNodeUnglued(MarketUserId.fromUuid("222"));
        verify(unglueDao, times(2)).checkNodeUnglued(MarketUserId.fromYandexuid("yauid"));
        verify(unglueDao, times(2)).checkNodeUnglued(MarketUserId.fromDeviceId("1"));
        verify(unglueDao, times(2)).checkEdgeUnglued(MarketUserId.fromUid(1234L), MarketUserId.fromUuid("222"));
        verify(unglueDao, times(2)).checkEdgeUnglued(MarketUserId.fromUid(1234L), MarketUserId.fromYandexuid("yauid"));
        verify(unglueDao, times(2)).checkEdgeUnglued(MarketUserId.fromUid(1234L), MarketUserId.fromDeviceId("1"));
        verify(marketUserIdDao).getGluedIds(ids);
        verify(marketUserIdDao).insertCheckouterData(ids);
        verify(marketUserIdDao).insertNewGlues(ids);
        verifyNoMoreInteractions(unglueDao);
        verifyNoMoreInteractions(marketUserIdDao);
        Assert.assertTrue("New data should be inserted!", saved);
    }

    @SneakyThrows
    @Test
    public void testFullCycleKnownData() {
        OrderBuyerRequestDto buyer =
                OrderBuyerRequestDto.builder()
                        .uid(1234L)
                        .uuid("222")
                        .yandexuid("yauid")
                        .userDevice(OrderBuyerUserDeviceRequestDto.builder()
                                .deviceId(OrderBuyerDeviceIdRequestDto.builder().googleServiceId("1").build())
                                .build())
                        .assessor(false)
                        .build();

        List<MarketUserId> ids = List.of(
                MarketUserId.fromUid(1234L),
                MarketUserId.fromYandexuid("yauid"),
                MarketUserId.fromUuid("222"),
                MarketUserId.fromDeviceId("1"));

        when(unglueDao.checkNodeUnglued(any())).thenReturn(Optional.empty());
        when(unglueDao.checkEdgeUnglued(any(), any())).thenReturn(Optional.empty());

        when(marketUserIdDao.getGluedIds(ids)).thenReturn(Collections.singletonList(MarketUserId.fromUuid("222",
                123L)));
        Set<MarketUserId> users = service.getGluedUsers(buyer).get();
        boolean saved = service.saveNewCheckouterData(users, buyer);

        verify(marketUserIdDao).getGluedIds(ids);
        verify(marketUserIdDao).insertCheckouterData(ids);
        verify(marketUserIdDao).insertGlues(List.of(MarketUserId.fromUid(1234L, 123L), MarketUserId.fromYandexuid("yauid", 123L)));

        verifyNoMoreInteractions(marketUserIdDao);
        Assert.assertTrue("New data should be inserted!", saved);
    }


    @SneakyThrows
    @Test
    public void testFullCycleNoNewData() {
        OrderBuyerRequestDto buyer =
                OrderBuyerRequestDto.builder()
                        .uid(1234L)
                        .uuid("222")
                        .yandexuid("yauid")
                        .userDevice(OrderBuyerUserDeviceRequestDto.builder()
                                .deviceId(OrderBuyerDeviceIdRequestDto.builder().iosDeviceId("1").build())
                                .build())
                        .assessor(false)
                        .build();

        List<MarketUserId> ids = List.of(
                MarketUserId.fromUid(1234L),
                MarketUserId.fromYandexuid("yauid"),
                MarketUserId.fromUuid("222"),
                MarketUserId.fromDeviceId("1"));

        when(unglueDao.checkNodeUnglued(any())).thenReturn(Optional.empty());
        when(unglueDao.checkEdgeUnglued(any(), any())).thenReturn(Optional.empty());

        when(marketUserIdDao.getGluedIds(ids)).thenReturn(Arrays.asList(MarketUserId.fromUuid("222", 123L),
                MarketUserId.fromYandexuid("yauid", 124L),
                MarketUserId.fromUid(1234L, 124L)));
        Set<MarketUserId> users = service.getGluedUsers(buyer).get();
        boolean saved = service.saveNewCheckouterData(users, buyer);

        verify(marketUserIdDao).getGluedIds(ids);

        verifyNoMoreInteractions(marketUserIdDao);
        Assert.assertFalse("No new data should be inserted!", saved);
    }


    @Test
    public void testCleanOldData() {
        service.cleanOldData();
        verify(marketUserIdDao).deleteCheckouterDataOlderThan(any());
        verifyNoMoreInteractions(marketUserIdDao);
        verifyNoMoreInteractions(unglueDao);
    }

    @Test
    public void testGlueOldData() {
        List<CheckouterRequestData> checkouterData = List.of(
                new CheckouterRequestData("123", "uuid", "yandexuid", Instant.now().minusSeconds(600)),
                new CheckouterRequestData("123", null, null, Instant.now().minusSeconds(600)));

        List<MarketUserId> checkouterUserIdsPt1 = List.of(
                MarketUserId.fromUid(123L), MarketUserId.fromUuid("uuid"), MarketUserId.fromYandexuid("yandexuid"));
        List<MarketUserId> checkouterUserIdsPt2 = List.of(MarketUserId.fromUid(123L));

        Assert.assertEquals("Error in converting checkouter data to userIds",
                CheckouterRequestData.toMarketUserIds(checkouterData.get(0)), checkouterUserIdsPt1);
        Assert.assertEquals("Error in converting checkouter data to userIds",
                CheckouterRequestData.toMarketUserIds(checkouterData.get(1)), checkouterUserIdsPt2);

        when(marketUserIdDao.getUngluedFromCheckouterData(any())).thenReturn(checkouterData);
        when(marketUserIdDao.getGluedIds(checkouterUserIdsPt1)).thenReturn(
                List.of(MarketUserId.fromUuid("uuid", 133L),
                        MarketUserId.fromYandexuid("yandexuid", 134L)));
        when(marketUserIdDao.getGluedIds(checkouterUserIdsPt2)).thenReturn(
                List.of(MarketUserId.fromUid(123L, 133L),
                        MarketUserId.fromUid(123L, 134L)));

        service.glueCheckouterData();
        verify(marketUserIdDao).getUngluedFromCheckouterData(any());
        verify(marketUserIdDao).getGluedIds(checkouterUserIdsPt1);
        verify(marketUserIdDao).getGluedIds(checkouterUserIdsPt2);

        verify(marketUserIdDao).insertGlues(List.of(MarketUserId.fromUid(123L, 133L),
                MarketUserId.fromUid(123L, 134L)));

        verifyNoMoreInteractions(marketUserIdDao);
        verifyNoMoreInteractions(unglueDao);
    }

    @Test
    public void glueContainerSerialization() {
        JsonSerializer<GluesService.GlueContainer> serializer =
                JsonSerializer.forClass(GluesService.GlueContainer.class);
        var container = new GluesService.GlueContainer(List.of(
                MarketUserId.fromUid(123L),
                MarketUserId.fromUuid("uuid")
        ));
        byte[] data = serializer.serialize(container);
        var deserialized = serializer.deserialize(data);
        assertThat(container.getIds()).containsAll(deserialized.getIds());
    }


    @Test
    public void filterMuids() {
        assertThat(service.saveNewCheckouterData(
                List.of(
                        MarketUserId.fromUid(1152921505357502251L),
                        MarketUserId.fromUuid("uuid-1152921505357502251")
                ),
                OrderBuyerRequestDto.builder().uid(1152921505357502251L).build())
        ).isFalse();
        assertThat(service.saveNewCheckouterData(
                List.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUuid("uuid-123L")
                ),
                OrderBuyerRequestDto.builder().uid(123L).build())
        ).isTrue();
    }
}
