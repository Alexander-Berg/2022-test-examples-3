package ru.yandex.market.logistics.logistics4go.components.logbroker.consumer;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.queue.payload.RecipientCodePayload;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;

import static ru.yandex.market.logistics.logistics4go.utils.JsonTestUtils.fileToJson;

@DisplayName("Обработка изменения заказа в LOM: код получателя готов к отправке")
@DatabaseSetup("/components/logbroker/consumer/common/enable_recipient_code_processing.xml")
public class RecipientCodeReadyTest extends AbstractIntegrationTest {
    private static final Instant EVENT_TIME = Instant.parse("2022-03-01T01:10:03Z");

    @Autowired
    private LomOrderEventConsumer lomOrderEventConsumer;

    @Test
    @DisplayName("Задача на передачу кода создана")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/common/recipient_code_ready_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recipientCodeSent() {
        recipientCodeSent(arrivedPickupEvent());
    }

    @Test
    @DisplayName("Задача на передачу кода создана - поменялся код")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    void recipientCodeChanged() {
        recipientCodeSent(recipientCodeChangedEvent());
    }

    private void recipientCodeSent(EventDto event) {
        lomOrderEventConsumer.accept(List.of(event));

        RecipientCodePayload expectedPayload = RecipientCodePayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .externalId("aa872c9c-afd4-4bf2-a8d9-df8b84852c04")
            .lomId(1L)
            .created(EVENT_TIME)
            .code("9999")
            .build();

        queueTaskChecker.assertAnyTaskWithPayload("PUSH_RECIPIENT_CODE_TO_LES", expectedPayload);
        queueTaskChecker.assertTasksCount("PUSH_LOM_ORDER_EVENT_TO_LES", 1);
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - событие было обработано ранее")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    void recipientCodeAlreadySent() {
        recipientCodeNotSent(arrivedPickupEvent());
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - нет 45 чп от ПВЗ")
    void recipientCodeNotSentNotArrived() {
        recipientCodeNotSent(courierShippedPickupEvent());
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - код еще не пришел")
    void recipientCodeNotArrived() {
        recipientCodeNotSent(arrivedPickupWithoutRecipientCodeEvent());
    }

    private void recipientCodeNotSent(EventDto event) {
        lomOrderEventConsumer.accept(List.of(event));

        queueTaskChecker.assertTasksCount("PUSH_RECIPIENT_CODE_TO_LES", 0);
        queueTaskChecker.assertTasksCount("PUSH_LOM_ORDER_EVENT_TO_LES", 1);
    }

    @Nonnull
    private EventDto arrivedPickupEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/order_arrived_pickup.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/order_arrived_pickup.json"));
    }

    @Nonnull
    private EventDto recipientCodeChangedEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/order_arrived_pickup.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/recipient_code_changed.json"));
    }

    @Nonnull
    private EventDto arrivedPickupWithoutRecipientCodeEvent() {
        return commonEvent()
            .setSnapshot(fileToJson(
                "components/logbroker/consumer/snapshots/order_arrived_pickup_without_recipient_code.json"
            ))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/order_arrived_pickup.json"));
    }

    @Nonnull
    private EventDto courierShippedPickupEvent() {
        return commonEvent()
            .setSnapshot(fileToJson("components/logbroker/consumer/snapshots/order_courier_shipped_pickup.json"))
            .setDiff(fileToJson("components/logbroker/consumer/diffs/order_arrived_pickup.json"));
    }

    @Nonnull
    private EventDto commonEvent() {
        return new EventDto()
            .setId(10L)
            .setCreated(EVENT_TIME)
            .setEntityType(EntityType.ORDER);
    }
}
