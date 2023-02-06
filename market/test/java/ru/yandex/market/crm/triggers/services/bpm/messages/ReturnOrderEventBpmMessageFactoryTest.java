package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ReturnOrderEventBpmMessageFactoryTest extends OrderEventBpmMessageFactoryTestBase {

    private final long ORDER_ID = 12345678L;
    private final long RETURN_ID = 658624L;
    private final int REGION_ID = 96;
    private final long SUPPLIER_ID = 635935L;

    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/ORDER_RETURN.json")
    private Resource orderReturnJson;

    @Test
    public void testReturnCreatedMessageProperties() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        List<UidBpmMessage> messages = factory.from(event);

        assertOrderReturnMessageProperties(messages);
    }

    @Test
    public void testReturnDeliveryUpdatedMessageProperties() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setType(HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED);

        List<UidBpmMessage> messages = factory.from(event);

        assertOrderReturnMessageProperties(messages);
    }

    @Test
    public void testReturnDeliveryStatusUpdatedMessageProperties() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setType(HistoryEventType.ORDER_RETURN_DELIVERY_STATUS_UPDATED);

        List<UidBpmMessage> messages = factory.from(event);

        assertOrderReturnMessageProperties(messages);
    }

    @Test
    public void testReturnDeliveryRescheduledMessageProperties() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setType(HistoryEventType.ORDER_RETURN_DELIVERY_RESCHEDULED);

        List<UidBpmMessage> messages = factory.from(event);

        assertOrderReturnMessageProperties(messages);
    }

    private void assertOrderReturnMessageProperties(List<UidBpmMessage> messages) {
        assertThat(messages.size(), is(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));
        UidBpmMessage message = messages.get(0);

        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(ORDER_ID));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.RETURN_ID), equalTo(RETURN_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(ORDER_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.RETURN_ID), equalTo(RETURN_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.SUPPLIER_ID), equalTo(SUPPLIER_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(REGION_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_FULFILMENT), equalTo(true));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_MODEL_DBS), equalTo(false));
        assertThat(message.getVariables().get(ProcessVariablesNames.YEAR),notNullValue());
        assertThat(message.getVariables().get(ProcessVariablesNames.BUYER),notNullValue());
        assertThat(message.getVariables().get(ProcessVariablesNames.CLIENT_NAME),notNullValue());
        Buyer buyer = (Buyer) message.getVariables().get(ProcessVariablesNames.BUYER);
        assertThat(buyer.getUid(), equalTo(987654321L));
        assertThat(buyer.getLastName(), equalTo("Иванов"));
        assertThat(buyer.getFirstName(), equalTo("Инакентий"));
    }

    @Override
    protected Resource testedJson() {
        return orderReturnJson;
    }
}
