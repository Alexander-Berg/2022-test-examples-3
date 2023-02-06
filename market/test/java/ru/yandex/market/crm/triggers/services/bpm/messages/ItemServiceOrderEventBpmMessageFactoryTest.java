package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class ItemServiceOrderEventBpmMessageFactoryTest extends OrderEventBpmMessageFactoryTestBase {
    private final long ORDER_ID = 12345678L;
    private final long ITEM_SERVICE_ID = 658624L;
    private static final ZoneId MOSCOW_ZONE_ID = ZoneId.of("Europe/Moscow");
    private LocalDateTime ITEM_SERVICE_DATE = LocalDateTime.of(2021, Month.APRIL, 2, 19, 31, 4);

    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/ITEM_SERVICE_STATUS_UPDATED.json")
    private Resource itemServiceEventJson;

    @Test
    public void testItemServiceStatusUpdatedMessageProperties() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        List<UidBpmMessage> messages = factory.from(event);
        assertItemServiceMessageProperties(messages);
    }

    @Test
    public void testItemServiceDateUpdatedMessageProperties() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setType(HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED);
        List<UidBpmMessage> messages = factory.from(event);

        assertItemServiceMessageProperties(messages);
    }

    private void assertItemServiceMessageProperties(List<UidBpmMessage> messages) {
        assertThat(messages.size(), is(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));
        UidBpmMessage message = messages.get(0);

        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(ORDER_ID));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ITEM_SERVICE_ID),
                equalTo(ITEM_SERVICE_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.ITEM_SERVICE_ID), equalTo(ITEM_SERVICE_ID));
        assertThat(message.getVariables().get(ProcessVariablesNames.ITEM_SERVICE_STATUS),
                equalTo("COMPLETED"));
        assertThat(message.getVariables().get(ProcessVariablesNames.ITEM_SERVICE_DATE_TS),
                equalTo(ITEM_SERVICE_DATE.atZone(MOSCOW_ZONE_ID).toInstant().toEpochMilli()));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(96L));
    }

    @Override
    protected Resource testedJson() {
        return itemServiceEventJson;
    }
}
