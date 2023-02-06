package ru.yandex.market.billing.checkout;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author snoop
 */
public class GetOrderEventsStrategyTest {

    @Test(expected = IllegalStateException.class)
    public void fulfilment_changed_off() {
        fulfilmentChange(true, false);
    }

    @Test(expected = IllegalStateException.class)
    public void fulfilment_changed_on() {
        fulfilmentChange(false, true);
    }

    @Test
    public void fulfilment_unchanged() {
        fulfilmentChange(false, false);
        fulfilmentChange(true, true);
        fulfilmentChange(null, true);
        fulfilmentChange(null, false);
    }

    private void fulfilmentChange(Boolean before, boolean after) {
        final EventProcessorSupport support = mock(EventProcessorSupport.class);
        final EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getValue(any(), any())).thenReturn("0");
        when(support.getEnvironmentService()).thenReturn(environmentService);
        GetOrderEventsStrategy strategy = new GetOrderEventsStrategy(support);
        final OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.NEW_PAYMENT);
        final Order orderAfter = mock(Order.class);
        when(orderAfter.isFulfilment()).thenReturn(after);
        event.setOrderAfter(orderAfter);
        if (before != null) {
            final Order orderBefore = mock(Order.class);
            when(orderBefore.isFulfilment()).thenReturn(before);
            event.setOrderBefore(orderBefore);
        }
        strategy.processIfNecessary(event, new Date());
    }
}
