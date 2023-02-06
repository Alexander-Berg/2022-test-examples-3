package ru.yandex.market.logistics.lom.jobs.processor.les;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.ShopReadyToCreateOrderEvent;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;

@DisplayName("Отправка ивента о готовности магазина создать заказ в LES")
public class ExportShopReadyToCreateOrderProcessorTest extends AbstractExportLesEventProcessorTest {
    @Autowired
    private ExportShopReadyToCreateOrderProcessor processor;

    @Test
    @DisplayName("Отправка ивента о готовности магазина создать заказ, без тэгов")
    @DatabaseSetup("/jobs/processor/les_export/before/order_pvz.xml")
    void success() {
        ProcessingResult result = processor.processPayload(PayloadFactory.lesWaybillEventPayload(100, 1, "1", 1));
        softly.assertThat(result).isEqualTo(ProcessingResult.success());

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            ShopReadyToCreateOrderEvent.EVENT_TYPE,
            new ShopReadyToCreateOrderEvent(
                "LO1",
                12L,
                1000L,
                LocalDate.parse("2019-05-24"),
                Instant.ofEpochSecond(1558699200L),
                null,
                null
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    @Test
    @DisplayName("Отправка ивента о готовности магазина создать заказ, непустые тэги")
    @DatabaseSetup({
        "/jobs/processor/les_export/before/order_pvz.xml",
        "/jobs/processor/les_export/before/add_tags.xml"
    })
    void successWithTags() {
        ProcessingResult result = processor.processPayload(PayloadFactory.lesWaybillEventPayload(100, 1, "1", 1));
        softly.assertThat(result).isEqualTo(ProcessingResult.success());

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            ShopReadyToCreateOrderEvent.EVENT_TYPE,
            new ShopReadyToCreateOrderEvent(
                "LO1",
                12L,
                1000L,
                LocalDate.parse("2019-05-24"),
                Instant.ofEpochSecond(1558699200L),
                List.of("EXPRESS"),
                null
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    @Test
    @DisplayName("Отправка ивента о готовности магазина создать заказ DBS в дропофф")
    @DatabaseSetup("/jobs/processor/les_export/before/order_dbs_dropoff.xml")
    void successWithDbsDropoffShipmentPointId() {
        ProcessingResult result = processor.processPayload(PayloadFactory.lesWaybillEventPayload(100, 1, "1", 1));
        softly.assertThat(result).isEqualTo(ProcessingResult.success());

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            ShopReadyToCreateOrderEvent.EVENT_TYPE,
            new ShopReadyToCreateOrderEvent(
                "LO1",
                12L,
                1000L,
                LocalDate.parse("2019-05-24"),
                Instant.ofEpochSecond(1558699200L),
                null,
                12345654321L
            ),
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    @Test
    @DisplayName("Сегмент не найден")
    @DatabaseSetup("/jobs/processor/les_export/before/order_pvz.xml")
    void orderNotFound() {
        softly.assertThatThrownBy(() -> processor.processPayload(
                PayloadFactory.lesWaybillEventPayload(100, 1000, "1", 1)
            ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [WAYBILL_SEGMENT] with id [1000]");
    }
}
