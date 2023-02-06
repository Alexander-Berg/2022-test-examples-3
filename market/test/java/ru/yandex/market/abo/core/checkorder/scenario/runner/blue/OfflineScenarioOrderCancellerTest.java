package ru.yandex.market.abo.core.checkorder.scenario.runner.blue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.core.checkorder.CheckOrderService;
import ru.yandex.market.abo.core.storage.json.checkorder.offline.JsonOfflineScenarioOrderStatusService;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.08.2020
 */
class OfflineScenarioOrderCancellerTest {

    private static final long ORDER_ID = 24135L;

    @InjectMocks
    private OfflineScenarioOrderCanceller offlineScenarioOrderCanceller;

    @Mock
    private CheckOrderService checkOrderService;
    @Mock
    private JsonOfflineScenarioOrderStatusService jsonOfflineScenarioOrderStatusService;

    @Mock
    private Order order;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getStatus()).thenReturn(OrderStatus.PROCESSING);
    }

    @Test
    void cancelOrderByUserTest() {
        offlineScenarioOrderCanceller.cancelOrderByUser(order, CheckOrderScenarioStatus.CANCELLED);

        verify(jsonOfflineScenarioOrderStatusService).save(eq(ORDER_ID), any());
        verify(checkOrderService).createCancellationRequest(any(), eq(ORDER_ID), any(), any(), anyLong(), anyList());
    }

    @Test
    void cancelOrderByUserTest__orderAlreadyCancelled() {
        when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);

        offlineScenarioOrderCanceller.cancelOrderByUser(order, CheckOrderScenarioStatus.CANCELLED);

        verify(jsonOfflineScenarioOrderStatusService).save(eq(ORDER_ID), any());
        verify(checkOrderService, never()).createCancellationRequest(any(), eq(ORDER_ID), any(), any(), anyLong(), anyList());
    }
}
