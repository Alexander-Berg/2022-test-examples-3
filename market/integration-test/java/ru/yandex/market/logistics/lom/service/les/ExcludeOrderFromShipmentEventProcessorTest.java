package ru.yandex.market.logistics.lom.service.les;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.logistics4shops.ExcludeOrderFromShipmentRequestCreated;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DatabaseSetup("/service/les/excludeorderfromshipment/before/prepare.xml")
public class ExcludeOrderFromShipmentEventProcessorTest extends AbstractContextualTest {

    @Autowired
    private LesConsumer lesConsumer;

    @Test
    @DisplayName("Обработка ивента об исключении заказа из отгрузки")
    void processExcludeOrderFromShipmentRequestCreatedEventTest() {
        processEvent(new ExcludeOrderFromShipmentRequestCreated("LO-123", 2L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_PROCESSING_DELAY,
            PayloadFactory.processOrderProcessingDelayPayload(1, 1, 2, "1", 1)
        );
    }

    @Test
    @DisplayName("Обработка ивента об исключении заказа из отгрузки - заказ не существует")
    void processExcludeOrderFromShipmentRequestCreatedEventOrderDoesNotExist() {
        processEvent(new ExcludeOrderFromShipmentRequestCreated("nonexistent_barcode", 2L));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t"
                    + "format=plain\t"
                    + "payload=Order with barcode [nonexistent_barcode] does not exist"
            );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Обработка ивента об исключении заказа из отгрузки - ивент не валиден")
    void processNotValidExcludeOrderFromShipmentRequestCreatedEvent() {
        processEvent(new ExcludeOrderFromShipmentRequestCreated(null, null));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t"
                    + "format=plain\t"
                    + "payload=Invalid LES event: "
                    + "ExcludeOrderFromShipmentRequestCreated(barcode=null, excludeOrderFromShipmentRequestId=null)"
            );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Обработка ивента об исключении заказа из отгрузки - у заказа нет DROPSHIP сегмента")
    @DatabaseSetup(
        value = "/service/les/excludeorderfromshipment/before/fulfillment_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    void processExcludeOrderFromShipmentRequestCreatedEventNoDropshipSegment() {
        processEvent(new ExcludeOrderFromShipmentRequestCreated("LO-123", 2L));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t"
                    + "format=plain\t"
                    + "payload=Order 1 has no DROPSHIP segment"
            );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    void processEvent(ExcludeOrderFromShipmentRequestCreated payload) {
        var event = new Event(
            "lom",
            "event_id_3",
            Instant.now().toEpochMilli(),
            ExcludeOrderFromShipmentRequestCreated.EVENT_TYPE,
            payload,
            "Тест"
        );
        lesConsumer.processEvent("messageId", event);
    }
}
