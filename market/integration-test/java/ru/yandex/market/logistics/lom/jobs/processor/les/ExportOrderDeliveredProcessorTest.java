package ru.yandex.market.logistics.lom.jobs.processor.les;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderDeliveredEvent;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;

@DisplayName("Тест на отправку ивента о получении 50 чекпоинта в LES")
class ExportOrderDeliveredProcessorTest extends AbstractExportLesEventProcessorTest {
    @Autowired
    private ExportOrderDeliveredProcessor processor;

    @Test
    @DisplayName("Отправка ивента о получении 50 чекпоинта")
    @DatabaseSetup("/jobs/processor/les_export/before/order_pvz.xml")
    void success() {
        ProcessingResult result = processor.processPayload(PayloadFactory.lesOrderEventPayload(
            100,
            1,
            "1",
            1
        ));
        softly.assertThat(result).isEqualTo(ProcessingResult.success());

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            OrderDeliveredEvent.EVENT_TYPE,
            new OrderDeliveredEvent(
                1L,
                "LO1",
                "+79999999999"
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    @Test
    @DisplayName("Заказ не найден")
    @DatabaseSetup("/jobs/processor/les_export/before/order_pvz.xml")
    void orderNotFound() {
        softly.assertThatThrownBy(() -> processor.processPayload(
                PayloadFactory.lesOrderEventPayload(100, 2, "1", 1)
            ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [2]");
    }
}
