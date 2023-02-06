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
import ru.yandex.market.logistics.logistics4go.queue.payload.ReturnOutboundCodePayload;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;

import static ru.yandex.market.logistics.logistics4go.utils.JsonTestUtils.fileToJson;

@DisplayName("Обработка изменения заказа в LOM: код выдачи невыкупа готов к отправке")
@DatabaseSetup("/components/logbroker/consumer/common/enable_return_outbound_code_processing.xml")
public class ReturnOutboundCodeReadyTest extends AbstractIntegrationTest {
    private static final Instant EVENT_TIME = Instant.parse("2022-03-01T01:10:03Z");

    @Autowired
    private LomOrderEventConsumer lomOrderEventConsumer;

    @Test
    @DisplayName("Задача на передачу кода создана")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    @ExpectedDatabase(
        value = "/components/logbroker/consumer/common/return_outbound_code_ready_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnOutboundCodeSent() {
        returnOutboundCodeSent(createEvent(
            "components/logbroker/consumer/snapshots/c2c_order_return_arrived_dropoff.json"
        ));
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - событие было обработано ранее")
    @DatabaseSetup("/components/logbroker/consumer/common/return_outbound_code_ready_event.xml")
    void returnOutboundCodeAlreadySent() {
        returnOutboundCodeNotSent(createEvent(
            "components/logbroker/consumer/snapshots/c2c_order_return_arrived_dropoff.json"
        ));
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - не C2C заказ")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    void notC2COrder() {
        returnOutboundCodeNotSent(createEvent(
            "components/logbroker/consumer/snapshots/not_c2c_order.json"
        ));
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - нет сегмента дропоффа")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    void noDropoffSegment() {
        returnOutboundCodeNotSent(createEvent(
            "components/logbroker/consumer/snapshots/c2c_no_dropoff_segment.json"
        ));
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - возврат еще не принят на дропоффе")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    void returnNotArrivedAtDropoff() {
        returnOutboundCodeNotSent(createEvent(
            "components/logbroker/consumer/snapshots/c2c_order_return_not_arrived_dropoff.json"
        ));
    }

    @Test
    @DisplayName("Задача на передачу кода не создана - на сегменте нет кода верификации")
    @DatabaseSetup("/components/logbroker/consumer/common/recipient_code_ready_event.xml")
    void verificationCodeNotFound() {
        returnOutboundCodeNotSent(createEvent(
            "components/logbroker/consumer/snapshots/c2c_order_return_not_arrived_dropoff.json"
        ));
    }

    private void returnOutboundCodeSent(EventDto event) {
        lomOrderEventConsumer.accept(List.of(event));

        ReturnOutboundCodePayload expectedPayload = ReturnOutboundCodePayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .externalId("aa872c9c-afd4-4bf2-a8d9-df8b84852c04")
            .lomId(1L)
            .created(EVENT_TIME)
            .code("12345")
            .build();

        queueTaskChecker.assertAnyTaskWithPayload("PUSH_RETURN_OUTBOUND_CODE_TO_LES", expectedPayload);
        queueTaskChecker.assertTasksCount("PUSH_LOM_ORDER_EVENT_TO_LES", 1);
    }

    private void returnOutboundCodeNotSent(EventDto event) {
        lomOrderEventConsumer.accept(List.of(event));

        queueTaskChecker.assertTasksCount("PUSH_RETURN_OUTBOND_CODE_TO_LES", 0);
        queueTaskChecker.assertTasksCount("PUSH_LOM_ORDER_EVENT_TO_LES", 1);
    }

    @Nonnull
    private EventDto createEvent(String snapshotPath) {
        return new EventDto()
            .setId(10L)
            .setCreated(EVENT_TIME)
            .setSnapshot(fileToJson(snapshotPath))
            .setEntityType(EntityType.ORDER)
            .setDiff(objectMapper.createArrayNode());
    }
}
