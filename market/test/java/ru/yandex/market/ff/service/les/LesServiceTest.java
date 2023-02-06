package ru.yandex.market.ff.service.les;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.dbqueue.service.LesBoxReturnEventProcessingService;
import ru.yandex.market.ff.model.dbqueue.LesReturnBoxEventPayload;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.ff.FulfilmentBoxItemsReceivedEvent;
import ru.yandex.market.logistics.les.ff.dto.BoxItemDto;
import ru.yandex.market.logistics.les.ff.enums.UnitCountType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Класс функциональных тестов для {@link LesService}.
 */
public class LesServiceTest extends IntegrationTest {

    @Autowired
    private LesProducer lesProducer;

    @Autowired
    private LesService lesService;

    @Autowired
    private LesBoxReturnEventProcessingService processingService;

    @Test
    @DatabaseSetup("classpath:service/les/success.xml")
    void success() throws IOException {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        processingService.processPayload(new LesReturnBoxEventPayload(1L));
        processingService.processPayload(new LesReturnBoxEventPayload(2L));

        verify(lesProducer, times(2)).send(captor.capture(), any());
        List<Event> allValues = captor.getAllValues();
        assertThat(allValues).hasSize(2);

        assertThat(allValues.get(0).getEventId()).isEqualTo("1-BOX_12345");
        assertThat(allValues.get(1).getEventId()).isEqualTo("2-BOX_12346");

        var event1 = (FulfilmentBoxItemsReceivedEvent) allValues.get(0).getPayload();

        assertThat(event1).extracting("boxId", "ffRequestId", "deliveryServicePartnerId", "warehousePartnerId")
                .containsOnly("BOX_12345", 1L, 0L, 121L);

        assertThat(event1.getItems())
                .extracting(BoxItemDto::getSupplierId, BoxItemDto::getVendorCode, BoxItemDto::getStock,
                        BoxItemDto::getInstances)
                .containsOnly(tuple(11L, "shopsku1", UnitCountType.FIT, Map.of("CIS", "991870055836")));

        var event2 = (FulfilmentBoxItemsReceivedEvent) allValues.get(1).getPayload();

        assertThat(event2).extracting("boxId", "orderId", "ffRequestId", "deliveryServicePartnerId",
                "warehousePartnerId")
                .containsOnly("BOX_12346", "12345", 2L, 0L, 121L);

        assertThat(event2.getItems())
                .extracting("supplierId", "vendorCode", "stock", "instances")
                .containsOnly(tuple(12L, "shopsku2", UnitCountType.DEFECT, Map.of("CIS", "991870055836")),
                        tuple(13L, "shopsku3", UnitCountType.FIT, Map.of("CIS", "991870055837")),
                        tuple(13L, "shopsku3", UnitCountType.FIT, Map.of("CIS", "991870055838")));
    }

    @Test
    @DatabaseSetup("classpath:service/les/ignoreExtraBoxesWithoutItems.xml")
    void ignoreExtraBoxesWithoutItems() throws IOException {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        processingService.processPayload(new LesReturnBoxEventPayload(1L));
        processingService.processPayload(new LesReturnBoxEventPayload(2L));

        verify(lesProducer, times(2)).send(captor.capture(), any());
        List<Event> allValues = captor.getAllValues();
        assertThat(allValues).hasSize(2);

        assertThat(allValues.get(0).getEventId()).isEqualTo("1-BOX_12345");
        assertThat(allValues.get(1).getEventId()).isEqualTo("2-BOX_12346");

        var event1 = (FulfilmentBoxItemsReceivedEvent) allValues.get(0).getPayload();

        assertThat(event1).extracting("boxId", "ffRequestId", "deliveryServicePartnerId", "warehousePartnerId")
                .containsOnly("BOX_12345", 1L, 0L, 121L);

        assertThat(event1.getItems())
                .extracting("supplierId", "vendorCode", "stock", "instances")
                .containsOnly(tuple(11L, "shopsku1", UnitCountType.FIT, Map.of("CIS", "991870055836")));

        var event2 = (FulfilmentBoxItemsReceivedEvent) allValues.get(1).getPayload();

        assertThat(event2).extracting("boxId", "ffRequestId", "deliveryServicePartnerId", "warehousePartnerId")
                .containsOnly("BOX_12346", 2L, 0L, 121L);

        assertThat(event2.getItems())
                .extracting("supplierId", "vendorCode", "stock", "instances")
                .containsOnly(tuple(12L, "shopsku2", UnitCountType.DEFECT, Map.of("CIS", "991870055836")),
                        tuple(13L, "shopsku3", UnitCountType.FIT, Map.of("CIS", "991870055837")),
                        tuple(13L, "shopsku3", UnitCountType.FIT, Map.of("CIS", "991870055838")));
    }


    @Test
    @DatabaseSetup("classpath:service/les/ignoreNonComplientBoxes.xml")
    void ignoreNonComplientBoxes() throws IOException {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        processingService.processPayload(new LesReturnBoxEventPayload(1L));
        processingService.processPayload(new LesReturnBoxEventPayload(2L));

        verify(lesProducer, times(2)).send(captor.capture(), any());
        List<Event> allValues = captor.getAllValues();
        assertThat(allValues).hasSize(2);

        assertThat(allValues.get(0).getEventId()).isEqualTo("1-BOX_12345");
        assertThat(allValues.get(1).getEventId()).isEqualTo("2-BOX_12346");

        var event1 = (FulfilmentBoxItemsReceivedEvent) allValues.get(0).getPayload();

        assertThat(event1).extracting("boxId", "ffRequestId", "deliveryServicePartnerId", "warehousePartnerId")
                .containsOnly("BOX_12345", 1L, 0L, 121L);

        assertThat(event1.getItems())
                .extracting("supplierId", "vendorCode", "stock", "instances")
                .containsOnly(tuple(11L, "shopsku1", UnitCountType.FIT, Map.of("CIS", "991870055836")));

        var event2 = (FulfilmentBoxItemsReceivedEvent) allValues.get(1).getPayload();

        assertThat(event2).extracting("boxId", "ffRequestId", "deliveryServicePartnerId", "warehousePartnerId")
                .containsOnly("BOX_12346", 2L, 0L, 121L);

        assertThat(event2.getItems())
                .extracting("supplierId", "vendorCode", "stock", "instances")
                .containsOnly(tuple(12L, "shopsku2", UnitCountType.DEFECT, Map.of("CIS", "991870055836")),
                        tuple(13L, "shopsku3", UnitCountType.FIT, Map.of("CIS", "991870055837")),
                        tuple(13L, "shopsku3", UnitCountType.FIT, Map.of("CIS", "991870055838")));
    }

    @Test
    @DatabaseSetup("classpath:service/les/failOnUnknownBoxes.xml")
    void failOnUnknownBoxes() {
        assertThrows(IllegalStateException.class, () ->
                        processingService.processPayload(new LesReturnBoxEventPayload(1L)),
                "Registry units [10] don't have a BOX_ID"
        );

        verifyZeroInteractions(lesProducer);
    }

    @Test
    void correctMappingToLesCountType() {
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.FIT))
                .isEqualTo(UnitCountType.FIT);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.DEFECT))
                .isEqualTo(UnitCountType.DEFECT);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.UNKNOWN))
                .isEqualTo(UnitCountType.UNKNOWN);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.ANOMALY))
                .isEqualTo(UnitCountType.ANOMALY);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.SURPLUS))
                .isEqualTo(UnitCountType.SURPLUS);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.EXPIRED))
                .isEqualTo(UnitCountType.EXPIRED);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.MISGRADING))
                .isEqualTo(UnitCountType.MISGRADING);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.UNDEFINED))
                .isEqualTo(UnitCountType.UNDEFINED);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.INCORRECT_IMEI))
                .isEqualTo(UnitCountType.INCORRECT_IMEI);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.INCORRECT_SERIAL_NUMBER))
                .isEqualTo(UnitCountType.INCORRECT_SERIAL_NUMBER);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.INCORRECT_CIS))
                .isEqualTo(UnitCountType.INCORRECT_CIS);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.PART_MISSING))
                .isEqualTo(UnitCountType.PART_MISSING);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.NON_COMPLIENT))
                .isEqualTo(UnitCountType.NON_COMPLIENT);
        assertThat(lesService.getLesCountType(ru.yandex.market.ff.client.enums.UnitCountType.NOT_ACCEPTABLE))
                .isEqualTo(UnitCountType.NOT_ACCEPTABLE);

        assertThat(UnitCountType.values()).hasSize(14);
        assertThat(ru.yandex.market.ff.client.enums.UnitCountType.values()).hasSize(14);
    }
}
