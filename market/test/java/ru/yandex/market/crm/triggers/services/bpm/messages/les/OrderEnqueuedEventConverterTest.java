package ru.yandex.market.crm.triggers.services.bpm.messages.les;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderEnqueuedEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class OrderEnqueuedEventConverterTest {
    private final OrderEnqueuedEventConverter converter = new OrderEnqueuedEventConverter();

    @Test
    public void orderEnqueuedEvent() {
        Event event = new Event(
                "lom",
                "1",
                1L,
                "LOM_ORDER_ENQUEUED",
                new OrderEnqueuedEvent(
                        123L,
                        "234-LO-345",
                        "PICKUP",
                        LocalDate.of(2021, 1, 1)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                ),
                null
        );
        UidBpmMessage message = converter.convert(event);

        assertThat(message.getType(), equalTo("LOM_ORDER_ENQUEUED"));
        assertThat(message.getCorrelationVariables(), equalTo(Map.of("orderId", 123L)));

        assertThat(message.getVariables().get("barcode"), equalTo("234-LO-345"));
        assertThat(message.getVariables().get("deliveryType"), equalTo("PICKUP"));
        assertThat(message.getVariables().get(
                        "maxDeliveryDate"),
                equalTo(LocalDate.of(2021, 1, 1))
        );
    }
}
