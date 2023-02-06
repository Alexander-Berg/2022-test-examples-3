package ru.yandex.market.logistics.lom.jobs.processor.les;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderEnqueuedEvent;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;

@DisplayName("Тест на отправку ивента о переходе заказа в статус ENQUEUED в LES")
class ExportOrderEnqueuedLesEventProcessorTest extends AbstractExportLesEventProcessorTest {
    @Autowired
    private ExportOrderEnqueuedLesEventProcessor processor;

    @Test
    @DisplayName("Отправка ивента о переходе заказа в статус ENQUEUED")
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
            OrderEnqueuedEvent.EVENT_TYPE,
            new OrderEnqueuedEvent(
                1L,
                "LO1",
                "MARKET_OWN_PICKUP_POINT",
                Instant.parse("2019-06-17T00:00:00Z")
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }
}
