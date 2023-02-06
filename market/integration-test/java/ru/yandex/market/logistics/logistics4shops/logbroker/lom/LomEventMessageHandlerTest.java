package ru.yandex.market.logistics.logistics4shops.logbroker.lom;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.logging.code.LomEventCode;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContentInBytes;

@DisplayName("Обработка ивентов LOM")
class LomEventMessageHandlerTest extends AbstractIntegrationTest {
    private static final Predicate<TskvLogRecord<?>> EVENT_NOT_PROCESSED_LOG_MATCHER = logEqualsTo(
        TskvLogRecord.info("Not processed event 1")
            .setLoggingCode(LomEventCode.EVENT_NOT_PROCESSED)
            .setExtra(Map.of("eventId", "1"))
    );

    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @SneakyThrows
    @Test
    @DisplayName("Успешный парсинг ивента")
    void eventParsingSuccess() {
        softly.assertThat(lomEventMessageHandler.parse(
            extractFileContentInBytes("logbroker/lom/handle/valid_event.json")
        ))
            .usingRecursiveComparison()
            .isEqualTo(LomEventFactory.eventDto(
                "logbroker/lom/handle/diff/valid_diff.json",
                "logbroker/lom/handle/snapshot/valid_shapshot.json"
            ));
    }

    @SneakyThrows
    @Test
    @DisplayName("Успешный процессинг заказа")
    void orderParsingSuccess() {
        softly.assertThatCode(() -> lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/handle/diff/valid_diff.json",
                "logbroker/lom/handle/snapshot/invalid_field.json"
            )
        )))
            .doesNotThrowAnyException();
        assertLogs().anyMatch(EVENT_NOT_PROCESSED_LOG_MATCHER);
    }

    @SneakyThrows
    @Test
    @DisplayName("Успешный процессинг заказа с невалидным полем")
    void orderWithInvalidFieldParsingSuccess() {
        softly.assertThatCode(() -> lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/handle/diff/valid_diff.json",
                "logbroker/lom/handle/snapshot/invalid_field.json"
            )
        )))
            .doesNotThrowAnyException();
        assertLogs().anyMatch(EVENT_NOT_PROCESSED_LOG_MATCHER);
    }

    @SneakyThrows
    @Test
    @DisplayName("Успешный процессинг заказа с невалидным значением enum")
    void orderWithInvalidEnumValueParsingSuccess() {
        softly.assertThatCode(() -> lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/handle/diff/valid_diff.json",
                "logbroker/lom/handle/snapshot/invalid_enum_value.json"
            )
        )))
            .doesNotThrowAnyException();
        assertLogs().anyMatch(EVENT_NOT_PROCESSED_LOG_MATCHER);
    }
}
