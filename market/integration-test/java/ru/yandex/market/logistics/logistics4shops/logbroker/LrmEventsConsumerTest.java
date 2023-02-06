package ru.yandex.market.logistics.logistics4shops.logbroker;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.consumer.LrmEventsConsumer;
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;

@DisplayName("Обработка событий из LRM")
class LrmEventsConsumerTest extends AbstractIntegrationTest {

    @Autowired
    private LrmEventsConsumer consumer;

    @Test
    @DisplayName("Успех для заказа Покупок")
    @ExpectedDatabase(
        value = "/logbroker/lrm/event/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        consumer.accept(List.of(returnEventOf("123456")));
    }

    @Test
    @DisplayName("Успех для заказа FaaS")
    @ExpectedDatabase(
        value = "/logbroker/lrm/event/after/success_faas.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successFaas() {
        consumer.accept(List.of(returnEventOf("FF-123456")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "LO-1234",
        "123-LO-456",
    })
    @DisplayName("Не обрабатываем события для неподходящих заказов")
    @ExpectedDatabase(
        value = "/jobs/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectOrder(String barcode) {
        consumer.accept(List.of(returnEventOf(barcode)));
    }

    @ParameterizedTest
    @EnumSource(value = ReturnEventType.class, names = "RETURN_STATUS_CHANGED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Не обрабатываем события ненужного типа")
    @ExpectedDatabase(
        value = "/jobs/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectEventType(ReturnEventType returnEventType) {
        consumer.accept(List.of(returnEventOf(returnEventType)));
    }

    @Nonnull
    private ReturnEvent returnEventOf(ReturnEventType returnEventType) {
        return returnEventOf("123456", returnEventType);
    }

    @Nonnull
    private ReturnEvent returnEventOf(String barcode) {
        return returnEventOf(barcode, ReturnEventType.RETURN_STATUS_CHANGED);
    }

    @Nonnull
    private ReturnEvent returnEventOf(String barcode, ReturnEventType returnEventType) {
        return ReturnEvent.builder()
            .requestId("external-request-id")
            .orderExternalId(barcode)
            .eventType(returnEventType)
            .build();
    }
}
