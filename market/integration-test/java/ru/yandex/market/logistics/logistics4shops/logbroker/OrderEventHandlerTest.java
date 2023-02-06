package ru.yandex.market.logistics.logistics4shops.logbroker;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.factory.OrderServiceEventFactory;
import ru.yandex.market.logistics.logistics4shops.logging.code.MbiosEventCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecordFormat.ExceptionPayload;

import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsToIgnoringFields;

@DisplayName("Обработка событий из MBIOS")
class OrderEventHandlerTest extends AbstractIntegrationTest {
    protected static final byte[] INVALID = "invalid".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private OrderEventMessageHandler orderEventProtoMessageHandler;

    @Test
    @DisplayName("Парсинг невалидного сообщения")
    void failParse() {
        softly.assertThatCode(() -> orderEventProtoMessageHandler.parse(INVALID))
            .isInstanceOf(RuntimeException.class);

        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.exception(ExceptionPayload.of(
                "Cannot parse mbios message",
                "InvalidProtocolBufferException: While parsing a protocol message, the input ended unexpectedly "
                + "in the middle of a field.  This could mean either that the input has been truncated "
                + "or that an embedded message misreported its own length."
            )).setLoggingCode(MbiosEventCode.MBIOS_MESSAGE_PARSING_ERROR)
        ));
    }

    @Test
    @DisplayName("Событие, не подходящее под процессоры, трассировки")
    void requestIdFromEvent() {
        softly.assertThatCode(() -> orderEventProtoMessageHandler.handle(List.of(
            OrderServiceEventFactory.baseEventBuilder().build(),
            OrderServiceEventFactory.baseEventBuilder().setId(2L).setTraceId("").build()
        ))).doesNotThrowAnyException();

        assertLogs()
            .anyMatch(logEqualsTo(
                TskvLogRecord.warn("Not processed event 1 as no processors matched to it")
                    .setLoggingCode(MbiosEventCode.MBIOS_MESSAGE_NOT_PROCESSED)
                    .setRequestId("order-service-event-request-id")
                    .setExtra(Map.of("eventId", "1"))
            ))
            .anyMatch(
                logEqualsToIgnoringFields(
                    TskvLogRecord.warn("Not processed event 2 as no processors matched to it")
                        .setLoggingCode(MbiosEventCode.MBIOS_MESSAGE_NOT_PROCESSED).setExtra(Map.of("eventId", "2")),
                    "requestId"
                )
                    .and(record -> record.getRequestId().matches(REQUEST_ID_PATTERN))
            );
    }
}
