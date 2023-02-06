package ru.yandex.market.logistics.lom.jobs.processor.les;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.ExcludeOrderFromShipmentRequestProcessingFinished;
import ru.yandex.market.logistics.les.lom.enums.ExcludeOrderFromShipmentResult;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ExportOrderFromShipmentExclusionFinishedPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@DatabaseSetup("/jobs/processor/les_export/before/export_order_from_shipment_exclusion_finished.xml")
class ExportOrderFromShipmentExclusionFinishedProcessorTest extends AbstractExportLesEventProcessorTest {
    private static final ExportOrderFromShipmentExclusionFinishedPayload PAYLOAD =
        PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(100L, 1L, 101L, 1, "1", 1L);

    @Autowired
    private ExportOrderFromShipmentExclusionFinishedProcessor processor;

    @Test
    @DisplayName("Отправка события с успешным статусом")
    void sendSuccessEvent() {
        processor.processPayload(PAYLOAD);

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            ExcludeOrderFromShipmentRequestProcessingFinished.EVENT_TYPE,
            new ExcludeOrderFromShipmentRequestProcessingFinished(
                "1",
                101L,
                ExcludeOrderFromShipmentResult.SUCCESS
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    @Test
    @DisplayName("Отправка события с неуспешным статусом")
    @DatabaseSetup(value = "/jobs/processor/les_export/before/failed_status.xml", type = DatabaseOperation.REFRESH)
    void sendFailedEvent() {
        processor.processPayload(PAYLOAD);

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            ExcludeOrderFromShipmentRequestProcessingFinished.EVENT_TYPE,
            new ExcludeOrderFromShipmentRequestProcessingFinished(
                "1",
                101L,
                ExcludeOrderFromShipmentResult.FAIL
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    @Test
    @DisplayName("Отправка события - заказ не найден")
    void orderNotFound() {
        softly.assertThatThrownBy(() -> processor.processPayload(
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(100L, 2L, 101L, 1, "1", 1L)
        ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [2]");
    }

    @Test
    @DisplayName("Отправка события - заявка не найдена")
    void changeRequestNotFound() {
        softly.assertThatThrownBy(() -> processor.processPayload(
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(100L, 1L, 101L, 2, "1", 1L)
        ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [2]");
    }

    @Test
    @DisplayName("Отправка события - заявка в активном статусе")
    @DatabaseSetup(value = "/jobs/processor/les_export/before/processing_status.xml", type = DatabaseOperation.REFRESH)
    void changeRequestInActiveStatus() {
        ProcessingResult result = processor.processPayload(PAYLOAD);

        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(result.getComment()).isEqualTo("Change request 1 is active");
    }

    @Test
    @DisplayName("Отправка события - LES недоступен")
    void lesFailedToRespond() {
        doThrow(new RuntimeException("LES failed to respond")).when(lesProducer).send(any(), any());
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("LES failed to respond");

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            ExcludeOrderFromShipmentRequestProcessingFinished.EVENT_TYPE,
            new ExcludeOrderFromShipmentRequestProcessingFinished(
                "1",
                101L,
                ExcludeOrderFromShipmentResult.SUCCESS
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }
}
