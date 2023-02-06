package ru.yandex.market.logistics.lrm.les.consumer;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.boxbot.CodeEvent;
import ru.yandex.market.logistics.les.dto.CarDto;
import ru.yandex.market.logistics.les.dto.CourierDto;
import ru.yandex.market.logistics.les.dto.PersonDto;
import ru.yandex.market.logistics.les.dto.PhoneDto;
import ru.yandex.market.logistics.les.tpl.CourierReceivedPickupReturnEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.service.les.LesConsumer;

@DisplayName("Обработка сообщения из LES")
@ParametersAreNonnullByDefault
class LesConsumerTest extends AbstractIntegrationTest {

    private static final Instant TIMESTAMP = Instant.parse("2021-09-06T11:12:13.00Z");

    @Autowired
    private LesConsumer lesConsumer;

    @BeforeEach
    void setup() {
        clock.setFixed(TIMESTAMP, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Ивент складывается в очередь db-queue")
    @ExpectedDatabase(
        value = "/database/les/consumer/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processTrueEvent() {
        var event = new Event(
            "lom",
            "event_id_3",
            TIMESTAMP.toEpochMilli(),
            CodeEvent.EVENT_TYPE_NEW_CODE,
            new CourierReceivedPickupReturnEvent(
                "box-external-id",
                100L,
                List.of(100L),
                Instant.parse("2021-09-21T12:30:00Z"),
                new CourierDto(
                    1L,
                    null,
                    200L,
                    new PersonDto("name", null, null),
                    new PhoneDto("phone", null),
                    new CarDto("number", null),
                    null
                )
            ),
            "Тест"
        );
        process(event);
    }

    @Test
    @DisplayName("Ивент с null складывается в очередь db-queue")
    @ExpectedDatabase(
        value = "/database/les/consumer/null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processEventWithNull() {
        var event = new Event(
            null,
            null,
            null,
            null,
            null,
            null
        );
        process(event);
    }

    private void process(Event event) {
        lesConsumer.processEvent("messageId", "les-request-id", event);
    }
}
