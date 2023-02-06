package ru.yandex.market.tpl.tms.sqs;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.les.PickupOrderDeliveredEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCreateRequestEvent;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.tms.sqs.consumer.SqsConsumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RequiredArgsConstructor
@Import({SqsConsumer.class})
public class SqsConsumerTest extends TplAbstractTest {
    private final SqsConsumer sqsConsumer;

    @Test
    void processEvent() {
        var event = new Event(
                "courier",
                "event_id_1",
                1L,
                "",
                new TplReturnAtClientAddressCreateRequestEvent(),
                "Test"
        );
        assertDoesNotThrow(() -> sqsConsumer.processEvent(event));
    }

    @Test
    void failOnUnknownEventButNoExceptionThrown() {
        var event = new Event(
                "courier",
                "event_id_1",
                1L,
                "",
                new PickupOrderDeliveredEvent(),
                "Test"
        );
        assertDoesNotThrow(() -> sqsConsumer.processEvent(event));
    }

    @Test
    void failWithInvalidPayloadButNoExceptionThrown() {
        var event = new Event(
                "courier",
                "event_id_1",
                1L,
                "",
                null,
                "Test"
        );
        assertDoesNotThrow(() -> sqsConsumer.processEvent(event));
    }
}
