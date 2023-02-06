package ru.yandex.market.crm.triggers.services.bpm.delegates;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.triggers.services.pers.PersAgitationSender;
import ru.yandex.market.pers.author.client.api.model.AgitationType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendAgitationTriggerTest {

    private static final Long ORDER_ID = 1234567L;
    private static final Long UID = 123113213131L;

    @Mock
    private PersAgitationSender persAgitationSender;

    private SendAgitationTriggerTask sendAgitationTriggerTask;

    @Before
    public void before() {
        doNothing().when(persAgitationSender).send(Mockito.any());
        sendAgitationTriggerTask = new SendAgitationTriggerTask(persAgitationSender);
    }

    @Test
    public void test() throws Exception {
        DelegateExecutionContext ctx = getMockedContext();
        sendAgitationTriggerTask.doExecute(ctx);
        verify(persAgitationSender, times(1)).send(any());
    }

    @Test
    public void testNotSendWithoutIds() {
        DelegateExecutionContext ctx = getMockedContext();
        when(ctx.getProcessVariable(ProcessVariablesNames.BUYER)).thenReturn(new Buyer());

        Assertions.assertThrows(IllegalStateException.class, () -> sendAgitationTriggerTask.doExecute(ctx));
        verify(persAgitationSender, times(0)).send(any());
    }

    private DelegateExecutionContext getMockedContext() {
        var ctx = mock(DelegateExecutionContext.class);
        var buyer = new Buyer();
        buyer.setUid(UID);

        when(ctx.getProcessVariable(ProcessVariablesNames.ORDER_ID)).thenReturn(ORDER_ID);
        when(ctx.getProcessVariable(ProcessVariablesNames.BUYER))
                .thenReturn(buyer);

        when(ctx.getCustomAttribute(CustomAttributesNames.AGITATION_TYPE, String.class))
                .thenReturn(String.valueOf(AgitationType.ORDER_CONFIRM_DELIVERY_DATES_MOVED_BY_USER));

        return ctx;
    }
}
