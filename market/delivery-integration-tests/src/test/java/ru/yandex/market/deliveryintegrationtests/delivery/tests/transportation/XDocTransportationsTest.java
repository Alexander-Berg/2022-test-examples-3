package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import dto.requests.scapi.ApiSortableSortRequest;
import dto.responses.scapi.orders.ApiOrderDto;
import dto.responses.scapi.sortable.SortableSort;
import dto.responses.tm.TmCheckpointStatus;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import toolkit.Delayer;

import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;

@Resource.Classpath({"delivery/delivery.properties"})
@Slf4j
@DisplayName("TM Test")
@Epic("TM")
public class XDocTransportationsTest extends AbstractTransportationTest {

    private final String tagCode = "FFWF_ROOT_REQUEST_ID";
    private Long freeCellId = null;

    @Property("delivery.sofino")
    private long sofino;

    @Property("delivery.transitrc")
    private long transitrc;

    @Test
    @DisplayName("ТМ: Создание XDoc перемещения")
    void xDocTransportationsTest() {
        Long shopRequestId = FFWF_API_STEPS.createXDocTransportations();
        Long transportation0Id = TM_STEPS.getTransportationsByTag(0, shopRequestId.toString(), tagCode);
        TM_STEPS.verifyStatusInHistory("inbound", transportation0Id, "ACCEPTED");
        Long transportation1Id = TM_STEPS.getTransportationsByTag(1, shopRequestId.toString(), tagCode);
        TM_STEPS.verifyTransportationStatus(transportation1Id, TransportationStatus.WAITING_DEPARTURE);
        TM_STEPS.verifyStatusInHistory("inbound", transportation1Id, "ACCEPTED");
        TM_STEPS.getInboundExternalId(transportation1Id);
        String inboundYandexId = TM_STEPS.getInboundIdWithPrefix(transportation1Id);
        // Принимаем приёмку на РЦ
        SCINT_STEPS.carArrived(1001426218L, inboundYandexId);
        SCINT_STEPS.getReadyToReceiveInbound(1001426218L, inboundYandexId);
        SC_STEPS.inboundsAccept(inboundYandexId);
        String sortableId = SC_STEPS.inboundsLink(inboundYandexId);
        ApiOrderDto scOrder = SC_STEPS.acceptOrdersAndGetZoneForCells(sortableId);
        freeCellId = SCINT_STEPS.getFreeCell(2000L, scOrder.getAvailableCells().get(0).getId());
        SC_STEPS.sortableSort(sortableId, freeCellId.toString());
        SC_STEPS.fixInbound(inboundYandexId);
        // Добавляем чекпоинты успешно завершённой приёмки, чтобы не дожидаться трекера
        long inboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(inboundYandexId, 2).getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
                inboundTrackerId,
                TmCheckpointStatus.INBOUND_ARRIVED,
                EntityType.INBOUND);
        Delayer.delay(1, TimeUnit.SECONDS);
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
                inboundTrackerId,
                TmCheckpointStatus.INBOUND_ACCEPTED,
                EntityType.INBOUND);
        // Проверяем, что информация о завершённой приёмке дошла до ТМ и перемещение перешло в RECEIVED
        TM_STEPS.verifyStatusInHistory("inbound", transportation1Id, "PROCESSED");
        TM_STEPS.verifyTransportationStatus(transportation1Id, TransportationStatus.RECEIVED);
    }

    @AfterEach
    @Step("Чистка данных после теста")
    public void tearDown() {
        log.info("Clearing SC cell");
        if (!freeCellId.equals(null)) {
            SCINT_STEPS.clearCell(freeCellId);
        }
    }

    /**
     * Тест выключен в DELIVERY-35718, т.к. флоу не включен в проде
     **/
    @Test
    @DisplayName("ТМ: Создание 1P XDoc перемещения")
    @Disabled
    void xDoc1PTransportationsTest() {
        Long shopRequestId = FFWF_API_STEPS.create1PXDocTransportations(transitrc, sofino);
        Long transportation0Id = TM_STEPS.getTransportationsByTag(0, shopRequestId.toString(), tagCode);
        TM_STEPS.verifyTransportationStatus(transportation0Id, TransportationStatus.INBOUND_SENT);
        TM_STEPS.verifyInboundStatus(transportation0Id, "ACCEPTED");
        Long transportation1Id = TM_STEPS.getTransportationsByTag(1, shopRequestId.toString(), tagCode);
        TM_STEPS.verifyTransportationStatus(transportation1Id, TransportationStatus.MOVEMENT_SENT);
        TM_STEPS.verifyMovementStatus(transportation1Id, "LGW_CREATED");
    }

}
