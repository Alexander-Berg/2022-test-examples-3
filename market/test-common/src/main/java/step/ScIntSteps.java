package step;

import java.util.List;

import client.ScIntClient;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

@Slf4j
public class ScIntSteps {

    private static final ScIntClient SCINT = new ScIntClient();

    private Long freeCell(Long scId, Long zoneId) {
        return SCINT
            .getCellsForZone(scId, zoneId)
            .getCells()
            .stream()
            .filter(cellsItem -> cellsItem.isEmpty())
            .findAny()
            .get()
            .getId();
    }

    /**
     * Получаем свободные ячейки в выбранной зоне, чтобы отсортировать туда поставку
     **/
    @Step("Получаем id свободной ячейки в выбранной зоне СЦ")
    public Long getFreeCell(Long scId, Long zoneId) {
        log.debug("Getting first free cell for zone {}", zoneId);
        Retrier.retry(() -> Assertions.assertNotNull(freeCell(scId, zoneId), "Нет свободных ячеек в зоне" + zoneId));
        return freeCell(scId, zoneId);
    }

    /**
     * Очистка ячейки, нужна для чистки данных после теста
     **/
    @Step("Очищаем ячейку после прохождения теста")
    public void clearCell(Long cellId) {
        log.debug("Clearing cell {}", cellId);
        Retrier.retry(() -> SCINT.clearCell(cellId));
    }

    @Step("Производим приёмку и сортировку на СЦ")
    public void acceptAndSortOrder(String orderId, String placeId, Long sortingCenterId) {
        log.debug("Getting cells for SC {}", sortingCenterId);
        List<Long> cellIds = Retrier.retry(() -> SCINT.getCellIds(sortingCenterId));
        log.debug("Accept and Sort SC order {}", orderId);
        Retrier.retry(() -> SCINT.acceptAndSortOrder(orderId, placeId, sortingCenterId, cellIds));
        Retrier.retry(SCINT::sendSegmentFfStatusHistoryToSqs);
    }

    @Step("Производим отгрузку на СЦ")
    public void shipOrder(String orderId, String placeId, Long sortingCenterId) {
        log.debug("Ship SC order {}", orderId);
        Retrier.retry(() -> SCINT.shipOrder(orderId, placeId, sortingCenterId));
        Retrier.retry(SCINT::sendSegmentFfStatusHistoryToSqs);
    }

    public void sendSegmentFfStatusHistoryToSqs() {
        Retrier.retry(SCINT::sendSegmentFfStatusHistoryToSqs);
    }

    @Step("Подтверждаем приёмку диспетчером")
    public void getReadyToReceiveInbound(Long scId, String inboundId) {
        log.debug("Getting ready to receive inbound {}", inboundId);
        Retrier.retry(() -> SCINT.performInboundAction(scId, inboundId, "READY_TO_RECEIVE"));
    }

    @Step("Заполняем данные водителя и авто")
    public void carArrived(Long scId, String inboundId) {
        log.debug("Adding car and driver data to inbound {}", inboundId);
        Retrier.retry(() -> SCINT.carArrived(scId, inboundId));
    }

    /**
     * Принимаем коробку на СЦ
     **/
    @Step("Принимаем коробку на СЦ")
    public void acceptPlace(String externalOrderId, String externalPlaceId, Long scId) {
        log.debug(
            "Accept place... externalOrderId:{} externalPlaceId:{} scId:{}",
            externalOrderId,
            externalPlaceId,
            scId
        );
        Retrier.retry(() -> SCINT.acceptOrder(externalOrderId, externalPlaceId, scId));
    }

    @Step("Сортируем коробку на СЦ")
    public void sortPlace(String externalPlaceId, Long scId) {
        log.debug("Getting cells for SC {} and order {}", scId, externalPlaceId);
        List<Long> cells = Retrier.retry(() -> SCINT.getAvailableCellsForOrder(scId, externalPlaceId));
        log.debug("Sort place... externalPlaceId:{} scId:{}", externalPlaceId, scId);
        Retrier.retry(() -> SCINT.sortOrder(externalPlaceId, externalPlaceId, scId, cells));
        Retrier.retry(SCINT::sendSegmentFfStatusHistoryToSqs);
    }

    /**
     * Фиксируем поставку на СЦ
     **/
    @Step("Фиксируем поставку на СЦ")
    public void fixInbound(String inboundExternalId) {
        log.debug("Closing inbound in sorting center");
        Retrier.retry(() -> SCINT.fixInbound(inboundExternalId));
    }
}
