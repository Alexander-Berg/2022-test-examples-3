package ru.yandex.market.logistics.logistics4shops.service.les;

import java.time.Instant;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.tm.OrderBindEvent;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logging.code.LesEventCode;
import ru.yandex.market.logistics.logistics4shops.service.les.processor.OrderBindEventProcessor;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Обработчик событий LES: привязка заказа к отгрузке")
class OrderBindEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private OrderBindEventProcessor processor;
    private static final Instant INTERVAL_START = Instant.parse("2022-02-21T11:30:00.00Z");
    private static final Instant INTERVAL_END = Instant.parse("2022-02-21T13:30:00.00Z");

    @Test
    @DisplayName("Успешно обработать событие привязки заказа к отгрузке")
    @DatabaseSetup("/service/les/orderbind/export/before/prepare.xml")
    @ExpectedDatabase(
        value = "/service/les/orderbind/export/after/queue_task_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processingEventSuccess() {
        processor.process(new OrderBindEvent("1", 2L, 3L, "TMU100", INTERVAL_START, INTERVAL_END), "1");
    }

    @Test
    @DisplayName("Успешно обработать событие привязки заказа к отгрузке, в базе нет отправки")
    @ExpectedDatabase(
        value = "/service/les/orderbind/export/after/queue_task_with_new_outbound_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/orderbind/export/after/outbound_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processingEventSuccessNewOutbound() {
        processor.process(new OrderBindEvent("1", 2L, 3L, "TMU100", INTERVAL_START, INTERVAL_END), "1");
    }

    @Test
    @DisplayName("Обработать событие привязки заказа к отгрузке - событие невалидное")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void processingNullEvent() {
        processor.process(new OrderBindEvent(null, 1L, null, null, null, null), "1");
        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.error("Event 1 is invalid")
                .setLoggingCode(LesEventCode.PROCESS_ORDER_BIND_EVENT_ERROR)
                .setExtra(Map.of("eventId", "1"))
        ));
    }

}
