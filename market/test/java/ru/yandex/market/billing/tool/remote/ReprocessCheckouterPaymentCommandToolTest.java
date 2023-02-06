package ru.yandex.market.billing.tool.remote;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.billing.checkout.GetOrderEventsService;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.core.util.tool.ToolRequest;

@RunWith(MockitoJUnitRunner.class)
public class ReprocessCheckouterPaymentCommandToolTest {
    @InjectMocks
    private ReprocessCheckouterPaymentCommandTool tool;

    @Mock
    private GetOrderEventsService getOrderEventsService;

    @Test
    public void testDoToolAction() {
        tool.doToolAction(new ToolRequest(0, null, ImmutableMap.<String, String>builder()
                .put("event", "1")
                .put("goal", PaymentGoal.ORDER_POSTPAY.name())
                .build(), null));

        Mockito.verify(getOrderEventsService).processPaymentEvent(1L, PaymentGoal.ORDER_POSTPAY);
        Mockito.verifyNoMoreInteractions(getOrderEventsService);
    }
}
