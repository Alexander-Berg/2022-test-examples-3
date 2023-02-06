package ru.yandex.market.logistics.logistics4go.components.logbroker.consumer;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.dbqueue.payload.QueuePayload;
import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.queue.payload.ReturnReadyPayload;
import ru.yandex.market.logistics.logistics4go.queue.payload.ReturnReturnedPayload;
import ru.yandex.market.logistics.logistics4go.queue.payload.ReturnTransportationStartedPayload;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;

import static ru.yandex.market.logistics.logistics4go.utils.JsonTestUtils.fileToJson;

@DisplayName("Обработка изменения заказа в LOM: события по невыкупу")
@DatabaseSetup("/components/logbroker/consumer/common/enable_return_events_from_lom_processing.xml")
public class LomReturnEventsTest extends AbstractIntegrationTest {
    private static final Instant EVENT_CREATED_TIMESTAMP = Instant.parse("2022-05-31T16:14:03.830084Z");
    private static final String ORDER_EXTERNAL_ID = "aa872c9c-afd4-4bf2-a8d9-df8b84852c04";
    private static final long RETURN_WAREHOUSE_ID = 10001700279L;
    @Autowired
    private LomOrderEventConsumer lomOrderEventConsumer;

    @Test
    @DisplayName("Отправляем событие при смене статуса заказа на RETURNING")
    void orderReturningEventSent() {
        lomOrderEventConsumer.accept(List.of(orderReturningEvent()));

        ReturnTransportationStartedPayload expectedPayload = ReturnTransportationStartedPayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .created(EVENT_CREATED_TIMESTAMP)
            .externalId(ORDER_EXTERNAL_ID)
            .lomId(1L)
            .destinationWarehouseId(RETURN_WAREHOUSE_ID)
            .build();

        assertTask("PUSH_RETURN_TRANSPORTATION_STARTED_EVENT_TO_LES", expectedPayload);
    }

    @Test
    @DisplayName("Отправляем событие при прибытии на конечный возвратный склад")
    void returnReadyEventSent() {
        lomOrderEventConsumer.accept(List.of(orderReturnReadyEvent()));

        ReturnReadyPayload expectedPayload = ReturnReadyPayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .created(EVENT_CREATED_TIMESTAMP)
            .externalId(ORDER_EXTERNAL_ID)
            .lomId(1L)
            .warehouseId(RETURN_WAREHOUSE_ID)
            .storageExpirationTimestamp(null)
            .build();

        assertTask("PUSH_RETURN_READY_EVENT_TO_LES", expectedPayload);
    }

    @Test
    @DisplayName("Отправляем событие при смене возвратного склада")
    void returnWarehouseChangedEventSent() {
        lomOrderEventConsumer.accept(List.of(returnWarehouseChangedEvent()));

        ReturnTransportationStartedPayload expectedPayload = ReturnTransportationStartedPayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .created(EVENT_CREATED_TIMESTAMP)
            .externalId(ORDER_EXTERNAL_ID)
            .lomId(1L)
            .destinationWarehouseId(10001800841L)
            .build();

        assertTask("PUSH_RETURN_TRANSPORTATION_STARTED_EVENT_TO_LES", expectedPayload);
    }

    @Test
    @DisplayName("Отправляем событие при возвращении заказа в магазин")
    void returnReturnedEventSent() {
        lomOrderEventConsumer.accept(List.of(returnReturnedEvent()));

        ReturnReturnedPayload expectedPayload = ReturnReturnedPayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .created(EVENT_CREATED_TIMESTAMP)
            .externalId(ORDER_EXTERNAL_ID)
            .lomId(1L)
            .build();

        assertTask("PUSH_RETURN_RETURNED_EVENT_TO_LES", expectedPayload);
    }

    @Test
    @DisplayName("Событие не связано с возвратом")
    void returnEventNotSent() {
        lomOrderEventConsumer.accept(List.of(arrivedPickupEvent()));

        queueTaskChecker.assertTasksCount("PUSH_RETURN_TRANSPORTATION_STARTED_EVENT_TO_LES", 0);
        queueTaskChecker.assertTasksCount("PUSH_RETURN_READY_EVENT_TO_LES", 0);
        queueTaskChecker.assertTasksCount("PUSH_RETURN_RETURNED_EVENT_TO_LES", 0);
    }

    private <T extends QueuePayload> void assertTask(String queueName, T expectedPayload) {
        queueTaskChecker.assertAnyTaskWithPayload(queueName, expectedPayload);
        queueTaskChecker.assertTasksCount(queueName, 1);
    }

    @Nonnull
    private EventDto orderReturningEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/order_returning.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/order_returning.json"));
    }

    @Nonnull
    private EventDto orderReturnReadyEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/return_ready.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/return_ready.json"));
    }

    @Nonnull
    private EventDto returnWarehouseChangedEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/return_warehouse_changed.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/return_warehouse_changed.json"));
    }

    @Nonnull
    private EventDto returnReturnedEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/return_returned.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/return_returned.json"));
    }

    @Nonnull
    private EventDto arrivedPickupEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/order_arrived_pickup.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/order_arrived_pickup.json"));
    }

    @Nonnull
    private EventDto commonEvent() {
        return new EventDto()
            .setId(10L)
            .setCreated(Instant.parse("2022-05-31T16:14:03.830084Z"))
            .setEntityType(EntityType.ORDER);
    }
}
