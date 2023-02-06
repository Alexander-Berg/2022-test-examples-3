package ru.yandex.market.antifraud.orders.service.offline;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.logbroker.entities.CancelOrderRequest;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.CheckouterClient;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.storage.entity.xurma.OrderCancelEvent;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.BulkOrderCancellationResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.NullifyCashbackEmitResponseDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AntifraudOfflineActionHandlerTest {
    @Mock
    private BlacklistService blackListService;
    @Mock
    private CheckouterClient checkouterClient;
    @Mock
    private ConfigurationService configurationService;

    private OfflineAntifraudBackendProxy backendProxy;

    @Before
    public void init() {
        backendProxy = new OfflineAntifraudBackendProxy(blackListService, checkouterClient, configurationService);
        when(configurationService.orderCancelEnabled()).thenReturn(true);
        when(configurationService.nullifyCashbackEnabled()).thenReturn(true);
        when(configurationService.userBanEnabled()).thenReturn(true);
    }

    @Test
    public void shouldCancelOrderSuccess() {
        var action = new CancelOrderActionHandler(backendProxy);
        when(checkouterClient.cancelOrders(anySet()))
                .thenReturn(new BulkOrderCancellationResponseDto(Set.of(123L), Set.of()));

        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        var result = action.call(request, Set.of(123L));
        Collection<OrderCancelEvent> entities = result.getEvents();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.toArray(new OrderCancelEvent[0])[0];
        assertThat(entity.getAction()).isEqualTo(AntifraudOfflineAction.CANCEL_ORDER);
        assertThat(entity.getSuccess()).isTrue();
    }

    @Test
    public void shouldCancelCashbackSuccess() {
        var action = new NullifyCashbackEmitActionHandler(backendProxy);
        when(checkouterClient.nullifyCashbackEmit(anySet()))
                .thenReturn(new NullifyCashbackEmitResponseDto(Set.of(123L), Set.of()));

        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        var result = action.call(request, Set.of(123L));
        Collection<OrderCancelEvent> entities = result.getEvents();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.toArray(new OrderCancelEvent[0])[0];
        assertThat(entity.getAction()).isEqualTo(AntifraudOfflineAction.NULLIFY_CASHBACK_EMIT);
        assertThat(entity.getSuccess()).isTrue();
    }

    @Test
    public void shouldBanUserSuccess() {
        var action = new BanUserActionHandler(backendProxy);

        CancelOrderRequest request = CancelOrderRequest.builder()
                .name("cancel_order")
                .entity("order")
                .key("123")
                .puid(33L)
                .ruleName("cancel_order")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        var result = action.call(request, Set.of(123L));
        Collection<OrderCancelEvent> entities = result.getEvents();
        assertThat(entities).hasSize(1);
        OrderCancelEvent entity = entities.toArray(new OrderCancelEvent[0])[0];
        assertThat(entity.getAction()).isEqualTo(AntifraudOfflineAction.BAN_USER);
        assertThat(entity.getSuccess()).isTrue();
    }
}
