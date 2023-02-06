package ru.yandex.market.abo.cpa.order;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;

/**
 * @author artemmz
 * @date 04/02/2020.
 */
class OrderEventLoaderUnitTest {
    private static final Long EVENT_ID = 23141L;
    @InjectMocks
    OrderStatCreator orderStatCreator;
    @Mock
    CpaOrderStatService cpaOrderStatService;
    @Mock
    ConfigurationService countersConfigurationService;
    @Mock
    OrderDeliveryExtractor orderDeliveryExtractor;
    @Mock
    OrderHistoryEvent event;
    @Mock
    Order order;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getType()).thenReturn(ORDER_STATUS_UPDATED);
        when(event.getOrderAfter()).thenReturn(order);
        when(order.isFake()).thenReturn(false);
        when(order.getUserGroup()).thenReturn(UserGroup.DEFAULT);
    }


    @Test
    @Disabled("move to event history test")
    void processOldBatch() {
        when(countersConfigurationService.getValueAsLong(anyString())).thenReturn(EVENT_ID + 1);
//        cpaSnapshotCreator.createCpaOrderStat();

        verify(cpaOrderStatService, only()).get(eq(Set.of()));
        verify(cpaOrderStatService, never()).save(anyCollection());
    }

    @Test
    void processOddBatch() {
        when(order.isFake()).thenReturn(true);
        orderStatCreator.processBatch(List.of(event));
        verify(cpaOrderStatService, never()).save(anyCollection());
    }
}
