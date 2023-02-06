package ru.yandex.market.crm.operatorwindow.utils;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.operatorwindow.domain.order.OrderRules;

import static org.mockito.ArgumentMatchers.any;

@Component
public class MockOrderRules extends AbstractMockService<OrderRules> {
    private final OrderRules orderRules;

    public MockOrderRules(OrderRules orderRules) {
        super(orderRules);
        this.orderRules = orderRules;
    }

    public void mockOrderCanBeConfirmed() {
        Mockito.when(
                        orderRules.canBeConfirmed(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any()))
                .thenReturn(true);
    }

    public void mockOrderCanBeCancelled() {
        Mockito.when(
                        orderRules.canBeCancelled(
                                ArgumentMatchers.any()
                        ))
                .thenReturn(true);
    }

    public void canBeCancelConfirmed() {
        Mockito.when(orderRules.canOrderCancellationBeConfirmed(any())).thenReturn(true);
    }

    public void shouldSendCancelConfirmToCheckouter() {
        Mockito.when(orderRules.shouldSendCancelConfirmToCheckouter(any())).thenReturn(true);
    }
}
