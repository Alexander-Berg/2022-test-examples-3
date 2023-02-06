package ru.yandex.market.logistics.logistics4go.components.logbroker.consumer;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;

import static ru.yandex.market.logistics.logistics4go.utils.JsonTestUtils.fileToJson;
import static ru.yandex.market.logistics.logistics4go.utils.JsonTestUtils.toJsonNode;

@DisplayName("Обработка сообщений на события изменения заказов в LOM")
public class LomOrderEventConsumerTest extends AbstractIntegrationTest {
    private static final JsonNode YA_GO_SNAPSHOT = fileToJson(
        "components/logbroker/consumer/snapshots/ya_go_order.json"
    );

    private static final JsonNode NOT_YA_GO_SNAPSHOT = fileToJson(
        "components/logbroker/consumer/snapshots/not_ya_go_order.json"
    );

    private static final JsonNode TEAPOT = fileToJson("components/logbroker/consumer/snapshots/teapot.json");

    private static final JsonNode EMPTY_DIFF = toJsonNode("[]");

    private static final JsonNode ORDER_CANCELLED_DIFF = fileToJson(
        "components/logbroker/consumer/diffs/order_cancelled.json"
    );

    private static final JsonNode ORDER_CANCELLATION_PROCESSING_DIFF = fileToJson(
        "components/logbroker/consumer/diffs/cancellation_order_request_status_processing.json"
    );

    private static final JsonNode ORDER_CHANGE_PROCESSING_DIFF = fileToJson(
        "components/logbroker/consumer/diffs/change_order_request_status_processing.json"
    );

    @Autowired
    private LomOrderEventConsumer lomOrderEventConsumer;

    @Test
    @DisplayName("Успешная обработка сообщений")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/tasks_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/cancelled_order_event_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        EventDto yaGoOrder = yaGoOrderEvent();
        EventDto notYaGoOrder = notYaGoOrderEvent();
        EventDto notOrder = yaGoPartnerEvent();

        lomOrderEventConsumer.accept(List.of(notYaGoOrder, yaGoOrder, notOrder));
    }

    @Test
    @DisplayName("Успешная обработка сообщения с удалением персональных данных")
    @DatabaseSetup("/controller/order/get/remove_recipient_from_get_order.xml")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/tasks_enqueued_without_recipient.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/cancelled_order_event_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithoutRecipient() {
        lomOrderEventConsumer.accept(List.of(yaGoOrderEvent()));
    }

    @Test
    @DisplayName("В одном из сообщений невалидный снапшот заказа")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/tasks_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/cancelled_order_event_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void withInvalidSnapshot() {
        EventDto yaGoOrder = yaGoOrderEvent();
        EventDto invalidSnapshotOrder = teapotOrderEvent();
        EventDto notYaGoOrder = notYaGoOrderEvent();

        lomOrderEventConsumer.accept(List.of(yaGoOrder, invalidSnapshotOrder, notYaGoOrder));
    }

    @Test
    @DisplayName("Событие об отмене не создается, т.к. переход заявки не в финальный успешный статус")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/tasks_enqueued_cancellation_in_created_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/cancelled_order_event_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationOrderRequestStatusDidNotChange() {
        EventDto yaGoOrder = yaGoOrderEvent().setDiff(ORDER_CANCELLATION_PROCESSING_DIFF);
        lomOrderEventConsumer.accept(List.of(yaGoOrder));
    }

    @Test
    @DisplayName("Событие об отмене не создается, т.к. поменялась заявка на изменение, а не отмену")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/tasks_enqueued_cancellation_in_created_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/cancelled_order_event_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void changeOrderRequestStatusChangedNotCancellation() {
        EventDto yaGoOrder = yaGoOrderEvent().setDiff(ORDER_CHANGE_PROCESSING_DIFF);
        lomOrderEventConsumer.accept(List.of(yaGoOrder));
    }

    @Test
    @DisplayName("Событие об отмене не создается, т.к. уже существует")
    @DatabaseSetup("/components/logbroker/consumer/after/cancelled_order_event_created.xml")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/tasks_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/after/cancelled_order_event_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelOrderEventAlreadyExists() {
        EventDto yaGoOrder = yaGoOrderEvent();
        lomOrderEventConsumer.accept(List.of(yaGoOrder));
    }

    @Nonnull
    private EventDto yaGoOrderEvent() {
        return new EventDto()
            .setId(10L)
            .setCreated(Instant.parse("2022-03-01T01:10:03Z"))
            .setSnapshot(YA_GO_SNAPSHOT)
            .setEntityType(EntityType.ORDER)
            .setDiff(ORDER_CANCELLED_DIFF);
    }

    @Nonnull
    private EventDto notYaGoOrderEvent() {
        return new EventDto()
            .setId(11L)
            .setCreated(Instant.parse("2022-03-01T01:10:04Z"))
            .setSnapshot(NOT_YA_GO_SNAPSHOT)
            .setEntityType(EntityType.ORDER)
            .setDiff(EMPTY_DIFF);
    }

    @Nonnull
    private EventDto yaGoPartnerEvent() {
        return new EventDto()
            .setId(12L)
            .setCreated(Instant.parse("2022-03-01T01:10:05Z"))
            .setSnapshot(YA_GO_SNAPSHOT)
            .setEntityType(EntityType.PARTNER)
            .setDiff(EMPTY_DIFF);
    }

    @Nonnull
    private EventDto teapotOrderEvent() {
        return new EventDto()
            .setId(13L)
            .setCreated(Instant.parse("2022-03-01T01:10:06Z"))
            .setSnapshot(TEAPOT)
            .setEntityType(EntityType.ORDER)
            .setDiff(EMPTY_DIFF);
    }
}
