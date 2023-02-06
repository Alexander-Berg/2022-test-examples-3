package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.triggers.domain.system.MessageInfo;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PaymentInvoiceGenerateEventBpmMessageFactoryTest extends BusinessOrderEventBpmMessageFactoryTestBase{
    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/PAYMENT_INVOICE_GENERATE.json")
    private Resource paymentInvoiceGenerateJson;

    @Override
    protected Resource testedJson() {
        return paymentInvoiceGenerateJson;
    }

    @Override
    @Test
    public void shouldCreateBpmMessageWithEmailUidIfMuidExists() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setMuid(123L);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.BUSINESS_PAYMENT_INVOICE_GENERATED))
                .collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getUid().getType(), is(UidType.EMAIL));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getEmail())));
    }

    @Test
    public void shouldCreateBpmMessageForGeneratePaymentInvoice() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();

        List<UidBpmMessage> messages = factory.from(event);
        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.BUSINESS_PAYMENT_INVOICE_GENERATED));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(order.getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        Map<String, Object> vars = message.getVariables();
        assertThat(vars.size(), equalTo(11));
        assertThat(vars.get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
        assertThat(vars.get(ProcessVariablesNames.LAST_ORDER_STATUS), equalTo(order.getStatus().name()));
        assertThat(vars.get(ProcessVariablesNames.EXPERIMENTS), equalTo(Map.of()));

        MessageInfo messageInfo = (MessageInfo) vars.get(ProcessVariablesNames.MESSAGE_INFO);
        assertNotNull(messageInfo);
        assertEquals(MessageTypes.BUSINESS_PAYMENT_INVOICE_GENERATED, messageInfo.getType());
    }

}
