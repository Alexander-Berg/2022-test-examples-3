package ru.yandex.market.antifraud.orders.service.offline;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.logbroker.entities.CancelOrderRequest;
import ru.yandex.market.antifraud.orders.queue.PgQueue;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.CheckouterClient;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.RoleService;
import ru.yandex.market.antifraud.orders.storage.dao.OfflineAntifraudEventsDao;
import ru.yandex.market.antifraud.orders.storage.dao.yt.CrmPlatformDao;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.xurma.OrderCancelEvent;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.BulkOrderCancellationResponseDto;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.antifraud.orders.service.offline.AntifraudOfflineAction.BAN_USER;
import static ru.yandex.market.antifraud.orders.service.offline.AntifraudOfflineAction.CANCEL_ORDER;
import static ru.yandex.market.antifraud.orders.service.offline.AntifraudOfflineAction.NULLIFY_CASHBACK_EMIT;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class OfflineAntifraudServiceTest {

    @Mock
    private OfflineAntifraudRule rule;
    @Mock
    private BlacklistService blackListService;
    @Mock
    private CrmPlatformDao crmPlatformDao;
    @Mock
    private OfflineAntifraudEventsDao offlineAntifraudEventsDao;

    private Queue<CancelOrderRequest> cancelOrderRequestQueue;
    @Mock
    private CheckouterClient checkouterClient;
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private RoleService roleService;
    @Mock
    private Map<AntifraudOfflineAction, AntifraudOfflineActionHandler> actionHandlers;

    private OfflineAntifraudService offlineAntifraudService;
    private OfflineAntifraudBackendProxy offlineAntifraudBackendProxy;

    @Before
    public void init() {
        offlineAntifraudService = new OfflineAntifraudService(
                List.of(rule),
                crmPlatformDao,
                offlineAntifraudEventsDao,
                mockPgQueue(),
                roleService,
                actionHandlers);
        offlineAntifraudBackendProxy = new OfflineAntifraudBackendProxy(blackListService, checkouterClient, configurationService);
        when(configurationService.orderCancelEnabled()).thenReturn(true);
        when(configurationService.userBanEnabled()).thenReturn(true);
        when(roleService.getRoleByUid(anyString())).thenReturn(Optional.empty());
    }

    private PgQueue<CancelOrderRequest> mockPgQueue() {
        cancelOrderRequestQueue = new ArrayDeque<>();
        var pgQueue = (PgQueue<CancelOrderRequest>) mock(PgQueue.class);
        when(pgQueue.offer(any())).then(x -> cancelOrderRequestQueue.offer(x.getArgument(0)));
        return pgQueue;
    }

    @Test
    public void shouldCancelOrderSuccess() {
        when(rule.processRequest(any(CancelOrderRequest.class)))
                .thenReturn(new OfflineRuleResult(Set.of(CANCEL_ORDER)));
        when(actionHandlers.get(any(AntifraudOfflineAction.class)))
                .thenReturn(new CancelOrderActionHandler(offlineAntifraudBackendProxy));
        when(offlineAntifraudEventsDao.findEntries(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());
        when(checkouterClient.cancelOrders(anySet()))
                .thenReturn(new BulkOrderCancellationResponseDto(Set.of(123L), Set.of()));
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verify(offlineAntifraudEventsDao).save(captor.capture());
        List<OrderCancelEvent> entities = (List<OrderCancelEvent>) captor.getValue();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.get(0);
        assertThat(entity.getAction()).isEqualTo(CANCEL_ORDER);
        assertThat(entity.getSuccess()).isTrue();
        assertThat(cancelOrderRequestQueue).isEmpty();
    }

    @Test
    //TODO: этого теста достаточно
    public void processCancellationRequest() {
        var actionResult = OrderCancelEvent.builder()
                .id(1024L)
                .orderId(1023L)
                .success(true)
                .action(NULLIFY_CASHBACK_EMIT)
                .build();
        when(rule.processRequest(any(CancelOrderRequest.class)))
                .thenReturn(new OfflineRuleResult(Set.of(NULLIFY_CASHBACK_EMIT)));
        var action = mock(AntifraudOfflineActionHandler.class);
        when(actionHandlers.get(any(AntifraudOfflineAction.class)))
                .thenReturn(action);
        when(action.call(any(CancelOrderRequest.class), anySet()))
                .thenReturn(new AntifraudOfflineActionHandler.Result(List.of(actionResult), true));
        when(offlineAntifraudEventsDao.findEntries(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verify(offlineAntifraudEventsDao).save(captor.capture());
        List<OrderCancelEvent> entities = (List<OrderCancelEvent>) captor.getValue();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0)).isSameAs(actionResult);
    }

    @Test
    public void shouldCancelMultiOrderSuccess() {
        when(rule.processRequest(any(CancelOrderRequest.class)))
                .thenReturn(new OfflineRuleResult(Set.of(CANCEL_ORDER)));
        when(actionHandlers.get(any(AntifraudOfflineAction.class)))
                .thenReturn(new CancelOrderActionHandler(offlineAntifraudBackendProxy));
        when(offlineAntifraudEventsDao.findEntries(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());
        when(checkouterClient.cancelOrders(anySet()))
                .thenReturn(new BulkOrderCancellationResponseDto(Set.of(123L), Set.of()));
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        CancelOrderRequest request = CancelOrderRequest.builder()
                .multiOrderId("multi_order")
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verify(offlineAntifraudEventsDao).save(captor.capture());
        List<OrderCancelEvent> entities = (List<OrderCancelEvent>) captor.getValue();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.get(0);
        assertThat(entity.getAction()).isEqualTo(CANCEL_ORDER);
        assertThat(entity.getSuccess()).isTrue();
        assertThat(cancelOrderRequestQueue).containsExactly(request);
    }

    @Test
    public void shouldPostProcessMultiOrderSuccess() {
        when(rule.processRequest(any(CancelOrderRequest.class)))
                .thenReturn(new OfflineRuleResult(Set.of(CANCEL_ORDER)));
        when(actionHandlers.get(any(AntifraudOfflineAction.class)))
                .thenReturn(new CancelOrderActionHandler(offlineAntifraudBackendProxy));
        when(crmPlatformDao.getCrmOrdersForUid(anyLong()))
                .thenReturn(List.of(
                        Order.newBuilder().setId(123).setMultiOrderId("multi_order").build(),
                        Order.newBuilder().setId(124).setMultiOrderId("multi_order").build()
                ));
        when(offlineAntifraudEventsDao.findEntriesByMultiOrderId("multi_order"))
                .thenReturn(List.of(OrderCancelEvent.builder().orderId(123L).build()));
        ArgumentCaptor<Set> cancelOrdersCaptor = ArgumentCaptor.forClass(Set.class);
        when(checkouterClient.cancelOrders(cancelOrdersCaptor.capture()))
                .thenReturn(new BulkOrderCancellationResponseDto(Set.of(124L), Set.of()));
        CancelOrderRequest request = CancelOrderRequest.builder()
                .multiOrderId("multi_order")
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.postProcessRequest(request);
        ArgumentCaptor<Collection> eventsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(offlineAntifraudEventsDao).save(eventsCaptor.capture());
        Set<Long> cancelledOrders = cancelOrdersCaptor.getValue();
        assertThat(cancelledOrders).containsExactly(124L);
        List<OrderCancelEvent> entities = (List<OrderCancelEvent>) eventsCaptor.getValue();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.get(0);
        assertThat(entity.getAction()).isEqualTo(CANCEL_ORDER);
        assertThat(entity.getOrderId()).isEqualTo(124L);
        assertThat(entity.getSuccess()).isTrue();
        assertThat(cancelOrderRequestQueue).isEmpty();
    }

    @Test
    public void shouldCancelOrderFailed() {
        when(rule.processRequest(any(CancelOrderRequest.class)))
                .thenReturn(new OfflineRuleResult(Set.of(CANCEL_ORDER)));
        when(actionHandlers.get(any(AntifraudOfflineAction.class)))
                .thenReturn(new CancelOrderActionHandler(offlineAntifraudBackendProxy));
        when(offlineAntifraudEventsDao.findEntries(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());
        when(checkouterClient.cancelOrders(anySet()))
                .thenReturn(new BulkOrderCancellationResponseDto(Set.of(), Set.of(123L)));
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verify(offlineAntifraudEventsDao).save(captor.capture());
        List<OrderCancelEvent> entities = (List<OrderCancelEvent>) captor.getValue();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.get(0);
        assertThat(entity.getAction()).isEqualTo(CANCEL_ORDER);
        assertThat(entity.getSuccess()).isFalse();
        assertThat(cancelOrderRequestQueue).isEmpty();
    }

    @Test
    public void shouldBanUser() {
        when(rule.processRequest(any(CancelOrderRequest.class)))
                .thenReturn(new OfflineRuleResult(Set.of(BAN_USER)));
        when(actionHandlers.get(any(AntifraudOfflineAction.class)))
                .thenReturn(new BanUserActionHandler(offlineAntifraudBackendProxy));
        when(offlineAntifraudEventsDao.findEntries(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verify(offlineAntifraudEventsDao).save(captor.capture());
        List<OrderCancelEvent> entities = (List<OrderCancelEvent>) captor.getValue();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.get(0);
        assertThat(entity.getAction()).isEqualTo(BAN_USER);
        assertThat(entity.getSuccess()).isTrue();
        assertThat(cancelOrderRequestQueue).isEmpty();
    }

    @Test
    public void shouldSkipProcessed() {
        when(offlineAntifraudEventsDao.findEntries(anyLong(), anyString(), anyString()))
                .thenReturn(List.of(OrderCancelEvent.builder().build()));
        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verifyNoMoreInteractions(checkouterClient);
        verifyNoMoreInteractions(rule);
    }

    @Test
    public void shouldSkipWhitelist() {
        when(roleService.getRoleByUid(anyString())).thenReturn(Optional.of(BuyerRole.builder().build()));
        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        offlineAntifraudService.processCancellationRequest(request);
        verifyNoMoreInteractions(checkouterClient);
        verifyNoMoreInteractions(rule);
    }
}
