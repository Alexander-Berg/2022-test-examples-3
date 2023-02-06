package ru.yandex.market.logistics.lom.jobs.processor.les;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderBox;
import ru.yandex.market.logistics.les.lom.OrderBoxesChangedEvent;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;

@DisplayName("Отправка события изменения коробок заказа")
public class ExportOrderBoxesChangedProcessorTest extends AbstractExportLesEventProcessorTest {

    @Autowired
    private ExportOrderBoxesChangedProcessor processor;

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() {
        softly.assertThatThrownBy(() -> processor.processPayload(
                PayloadFactory.lesOrderEventPayload(100, 2, "1", 1)
            ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [2]");
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/jobs/processor/les_export/before/order_places.xml")
    void success() {
        ProcessingResult result = processor.processPayload(PayloadFactory.lesOrderEventPayload(100, 1, "1", 1));
        softly.assertThat(result).isEqualTo(ProcessingResult.success());

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            OrderBoxesChangedEvent.EVENT_TYPE,
            new OrderBoxesChangedEvent(
                "1",
                "LO1",
                List.of(new OrderBox("place-1"))
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

}
