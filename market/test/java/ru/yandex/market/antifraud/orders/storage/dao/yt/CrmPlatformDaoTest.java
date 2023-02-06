package ru.yandex.market.antifraud.orders.storage.dao.yt;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializer;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.yt.ytclient.object.ConsumerSource;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 03.12.2019
 */
public class CrmPlatformDaoTest {
    private final YtClient ytClient = Mockito.mock(YtClient.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);

    private final CrmPlatformDao crmPlatformDao = new CrmPlatformDao(ytClient, configurationService);

    @Before
    public void init() throws IllegalAccessException, InstantiationException, NoSuchMethodException,
            InvocationTargetException {
        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
        when(ytClient.waitProxies()).thenReturn(completableFuture);
    }

    @Test
    public void getOrdersByUidTest() {
        Order order = Order.getDefaultInstance();
        when(ytClient.selectRows(any(), any(), any()))
            .thenAnswer(inv -> {
                var serializer = inv.<YTreeObjectSerializer<?>>getArgument(1);
                var instance = serializer.newInstance();
                serializer.getField("fact").get().field.set(instance, order.toByteArray());
                inv.<ConsumerSource<Object>>getArgument(2).accept(instance);
                return CompletableFuture.completedFuture(null);
            });

        List<Order> orders = crmPlatformDao.getCrmOrdersForUid(433348306L);

        assertThat(orders.size()).isEqualTo(1);
        assertThat(orders.get(0)).isEqualTo(order);
        ArgumentCaptor<SelectRowsRequest> captor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        verify(ytClient).waitProxies();
        verify(ytClient).selectRows(captor.capture(), any(), any());
        verifyNoMoreInteractions(ytClient);
        assertThat(captor.getValue().getQuery()).contains("puid", "433348306");
    }

    @Test
    public void getCrmOrdersForSingleId() {
        ArgumentCaptor<SelectRowsRequest> captor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        when(ytClient.selectRows(captor.capture(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(configurationService.getMaxRequestsInCrmDaoSplit())
            .thenReturn(1L);
        crmPlatformDao.getCrmOrdersForIds(List.of(
                MarketUserId.fromUid(12331L)
            ),
            LocalDate.of(2019, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        String query = captor.getValue().getQuery();
        String expected = " id, id_type, fact_id, timestamp, fact FROM " +
            "[//home/market/production/crm/platform/facts/OrderAntifraud] " +
            "WHERE (id, id_type) = ('12331', 'puid')" +
            " order by id, id_type, fact_id desc limit 300";
        assertThat(query).isEqualTo(expected);
    }

    @Test
    public void getCrmOrdersForIds() {
        ArgumentCaptor<SelectRowsRequest> captor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        when(ytClient.selectRows(captor.capture(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        crmPlatformDao.getCrmOrdersForIds(List.of(
                MarketUserId.fromUid(12331L),
                MarketUserId.fromUid(12332L),
                MarketUserId.fromUuid("uuid-123"),
                MarketUserId.fromUuid("uuid-124"),
                MarketUserId.fromUuid("uuid-125")
            ),
            LocalDate.of(2019, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        String query = captor.getValue().getQuery();
        String expected = " id, id_type, fact_id, timestamp, fact FROM " +
            "[//home/market/production/crm/platform/facts/OrderAntifraud] " +
            "WHERE (id, id_type) IN (('12331', 'puid'), ('12332', 'puid'), ('uuid-123', 'uuid'), " +
            "('uuid-124', 'uuid'), ('uuid-125', 'uuid'))" +
            " and timestamp > 1559347200000" +
            " order by timestamp desc limit 300";
        assertThat(query).isEqualTo(expected);
    }

    @Test
    public void getCrmOrdersForFewIds() {
        ArgumentCaptor<SelectRowsRequest> captor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        when(ytClient.selectRows(captor.capture(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(configurationService.getMaxRequestsInCrmDaoSplit())
            .thenReturn(2L);
        crmPlatformDao.getCrmOrdersForIds(List.of(
                MarketUserId.fromUid(12331L),
                MarketUserId.fromUuid("uuid-123")
            ),
            LocalDate.of(2019, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        assertThat(captor.getAllValues())
            .extracting(SelectRowsRequest::getQuery)
            .allMatch(query -> query.contains("order by id, id_type, fact_id desc limit 300"));
    }

    @Test
    public void getCrmOrdersForDuplicateIds() {
        ArgumentCaptor<SelectRowsRequest> captor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        when(ytClient.selectRows(captor.capture(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        crmPlatformDao.getCrmOrdersForIds(List.of(
                MarketUserId.fromUuid("uuid-123"),
                MarketUserId.fromUuid("uuid-123"),
                MarketUserId.fromUid(12331L),
                MarketUserId.fromUid(12331L),
                MarketUserId.fromUid(12331L)
            ),
            null);
        String query = captor.getValue().getQuery();
        assertThat(query)
            .containsOnlyOnce("uuid-123")
            .containsOnlyOnce("12331");
    }


    @Test
    public void getAllOrders() {
        ArgumentCaptor<SelectRowsRequest> captor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        when(ytClient.selectRows(captor.capture(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        Iterator<Order> orderI =
            crmPlatformDao.getAllOrders(Instant.ofEpochMilli(1559347200000L), Instant.ofEpochMilli(1559347300000L));
        while (orderI.hasNext()) {
            orderI.next();
        }
        String query = captor.getValue().getQuery();
        String expected = " id, id_type, fact_id, timestamp, fact FROM " +
            "[//home/market/production/crm/platform/facts/OrderAntifraud] WHERE  " +
            "timestamp > 1559347200000 and timestamp < 1559347300000 order by timestamp asc limit 5000";
        assertThat(query).isEqualTo(expected);
    }
}
