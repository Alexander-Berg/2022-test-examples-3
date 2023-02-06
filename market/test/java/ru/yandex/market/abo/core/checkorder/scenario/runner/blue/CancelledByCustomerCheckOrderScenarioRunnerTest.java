package ru.yandex.market.abo.core.checkorder.scenario.runner.blue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.checkorder.CheckOrderService;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.FAIL;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.IN_PROGRESS;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_NOT_PAID;

/**
 * @author artemmz
 * @date 13/12/2019.
 */
class CancelledByCustomerCheckOrderScenarioRunnerTest {
    private static final long USER_ID = 13L;
    private static final Long SHOP_ID = 4234234L;

    @InjectMocks
    CancelledByCustomerCheckOrderScenarioRunner cancelledRunner;
    @Mock
    Order order;
    @Mock
    CheckOrderScenario scenario;
    @Mock
    CheckOrderService checkOrderService;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    OrderHistoryEvent event;
    @Mock
    ClientInfo cancelledClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(order.getUid()).thenReturn(USER_ID);
        when(order.getShopId()).thenReturn(SHOP_ID);
        when(order.getRgb()).thenReturn(Color.BLUE);
        when(scenario.getShopId()).thenReturn(SHOP_ID);
        when(mbiApiService.getIndexState(SHOP_ID)).thenReturn(new PartnerIndexState(SHOP_ID, false, true));
    }

    @Test
    void inProgress() {
        when(order.getSubstatus()).thenReturn(OrderSubstatus.STARTED);
        assertEquals(IN_PROGRESS, cancelledRunner.checkProgress(scenario, order));
        verifyNoMoreInteractions(checkOrderService);
    }

    @Test
    void createCancellation() {
        when(order.getSubstatus()).thenReturn(CancelledByCustomerCheckOrderScenarioRunner.CANCEL_AFTER);
        assertEquals(IN_PROGRESS, cancelledRunner.checkProgress(scenario, order));
        verify(checkOrderService).createCancellationRequest(
                any(), anyLong(), any(), any(), anyLong(), anyList());
    }

    @ParameterizedTest
    @ValueSource(longs = {USER_ID, USER_ID + 1})
    void cancelledByUs(Long cancelledUserId) {
        when(order.getSubstatus()).thenReturn(CancelledByCustomerCheckOrderScenarioRunner.CANCEL_AFTER);
        when(order.getCancellationRequest()).thenReturn(new CancellationRequest(USER_NOT_PAID, "foo"));
        when(checkOrderService.getOrderHistoryEvents(any(), any(), any())).thenReturn(List.of(event));
        when(event.getAuthor()).thenReturn(cancelledClient);
        when(cancelledClient.getUid()).thenReturn(cancelledUserId);

        assertEquals(cancelledUserId.equals(USER_ID) ? SUCCESS : FAIL, cancelledRunner.checkProgress(scenario, order));
    }
}
