package step;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import client.CheckouterClient;
import client.LrmClient;
import client.ScIntClient;
import delivery.client.lrm.client.model.CreateReturnResponse;
import delivery.client.lrm.client.model.LogisticPointType;
import delivery.client.lrm.client.model.ReturnBox;
import delivery.client.lrm.client.model.ReturnBoxStatus;
import delivery.client.lrm.client.model.ReturnSegment;
import delivery.client.lrm.client.model.ReturnSegmentShipment;
import delivery.client.lrm.client.model.ReturnSegmentStatus;
import delivery.client.lrm.client.model.ReturnSource;
import delivery.client.lrm.client.model.SearchReturn;
import delivery.client.lrm.client.model.ShipmentDestination;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.checkout.checkouter.order.Order;

@Slf4j
public class LrmSteps {

    private static final LrmClient LRM_CLIENT = new LrmClient();
    private static final CheckouterClient CHECKOUTER_CLIENT = new CheckouterClient();
    private static final ScIntClient SC_INT_CLIENT = new ScIntClient();

    @Step("Создание частичного возврата в ЛРМ")
    public CreateReturnResponse createReturnLrm(
        Long orderId,
        ReturnSource source,
        String offerId,
        Long scLogisticPointId
    ) {
        log.debug("Create return in LRM");
        Order order = Retrier.clientRetry(() -> CHECKOUTER_CLIENT.getOrder(orderId));
        return LRM_CLIENT.createReturn(order, source, offerId, scLogisticPointId);
    }

    @Step("Коммит возврата в ЛРМ")
    public void commitReturnLrm(Long returnId) {
        log.debug("Commit return in LRM");
        LRM_CLIENT.commitReturn(returnId);
    }

    @Step("Проверка коммита возврата в ЛРМ")
    public void verifyReturnCommit(String boxExternalId) {
        log.debug("Get commited return from LRM");
        Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            Assertions.assertNotNull(response.getCommitedTs(), "Возврат не закоммичен для коробки " + boxExternalId);
        });
    }

    @Step("Проверка создания ПВЗ сегмента в ЛРМ")
    public void verifyPvzSegmentCreation(String boxExternalId) {
        log.debug("Check PVZ segemnt in LRM");
        Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            ReturnSegment segment = Optional.ofNullable(response.getBoxes())
                .stream()
                .flatMap(Collection::stream)
                .map(ReturnBox::getSegments)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(s -> s.getLogisticPoint() != null && s.getLogisticPoint().getType() == LogisticPointType.PICKUP)
                .findFirst()
                .orElse(null);
            Assertions.assertNotNull(segment, "Не найден ПВЗ сегмент для коробки " + boxExternalId);
            Assertions.assertEquals(
                ReturnSegmentStatus.CREATED,
                segment.getStatus(),
                "Неподходящий статус для ПВЗ сегмента для коробки " + boxExternalId
            );
        });
    }

    @Step("Проверка статуса коробки в ЛРМ")
    public void verifyBoxStatus(String boxExternalId, ReturnBoxStatus boxStatus) {
        log.debug("Check box status in LRM");
        Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            ReturnBox returnBox = Optional.ofNullable(response.getBoxes())
                .stream()
                .flatMap(Collection::stream)
                .filter(box -> boxExternalId.equals(box.getExternalId()))
                .findFirst()
                .orElse(null);
            Assertions.assertNotNull(returnBox, "Не найдена коробка " + boxExternalId);
            Assertions.assertEquals(
                boxStatus,
                returnBox.getStatus(),
                "Неподходящий статус коробки " + boxExternalId
            );
        });
    }

    @Step("Проверка статуса сегмента в ЛРМ")
    public void verifySegmentStatus(String boxExternalId, Long segmentPartnerId, ReturnSegmentStatus segmentStatus) {
        verifySegmentStatus(boxExternalId, segmentPartnerId, segmentStatus, false);
    }

    @Step("Проверка статуса сегмента в ЛРМ")
    public void verifySegmentStatus(
        String boxExternalId,
        Long segmentPartnerId,
        ReturnSegmentStatus segmentStatus,
        boolean sendSegmentFfStatusHistoryToSqs
    ) {
        log.debug("Check segment status in LRM");
        Retrier.retry(
            () -> {
                if (sendSegmentFfStatusHistoryToSqs) {
                    Retrier.retry(SC_INT_CLIENT::sendSegmentFfStatusHistoryToSqs);
                }
                SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
                ReturnBox returnBox = Optional.ofNullable(response.getBoxes())
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(box -> boxExternalId.equals(box.getExternalId()))
                    .findFirst()
                    .orElse(null);
                Assertions.assertNotNull(returnBox, "Не найдена коробка " + boxExternalId);
                List<ReturnSegment> segmentsForPartner = Optional.ofNullable(returnBox.getSegments())
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(segment -> segmentPartnerId.equals(segment.getLogisticPoint().getPartnerId()))
                    .toList();
                Assertions.assertFalse(
                    segmentsForPartner.isEmpty(),
                    "Не найдены сегменты для партнёра " + segmentPartnerId
                );
                Assertions.assertTrue(
                    segmentsForPartner.stream().anyMatch(s -> Objects.equals(s.getStatus(), segmentStatus)),
                    "Неподходящий статус сегмента %s".formatted(
                        segmentsForPartner.stream().map(ReturnSegment::getUniqueId).toList()
                    )
                );
            },
            Retrier.RETRIES_BIG
        );
    }

    /**
     * Проверка заполнения данных отгрузки из ПВЗ.
     *
     * @param boxExternalId     штрихкод коробки
     * @param mkSortingCenterId идентификатор партнёра МК СЦ
     * @return данные МК СЦ, куда отгружаем
     */
    @Step("Проверка сохранения данных отгрузки ПВЗ сегмента в ЛРМ")
    public ShipmentDestination verifyPvzSegmentShipment(
        String boxExternalId,
        Long mkSortingCenterId
    ) {
        log.debug("Check PVZ segment shipment in LRM");
        return Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            ReturnSegment segment = Optional.ofNullable(response.getBoxes())
                .stream()
                .flatMap(Collection::stream)
                .map(ReturnBox::getSegments)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(s -> s.getLogisticPoint() != null && s.getLogisticPoint().getType() == LogisticPointType.PICKUP)
                .findFirst()
                .orElse(null);
            Assertions.assertNotNull(segment, "Не найден ПВЗ сегмент для коробки " + boxExternalId);
            ShipmentDestination shipmentDestination = Optional.of(segment)
                .map(ReturnSegment::getShipment)
                .map(ReturnSegmentShipment::getDestination)
                .orElse(null);
            Assertions.assertNotNull(
                shipmentDestination,
                "Не найдена информация о логистической точке отгрузки для ПВЗ сегмента для коробки " + boxExternalId
            );
            Assertions.assertNotNull(
                shipmentDestination.getLogisticPointId(),
                "Не найден идентификатор логистической точки отгрузки для ПВЗ сегмента для коробки " + boxExternalId
            );
            Assertions.assertEquals(
                mkSortingCenterId,
                shipmentDestination.getPartnerId(),
                "Некорректный партнёр отгрузки для ПВЗ сегмента для коробки " + boxExternalId
            );
            return shipmentDestination;
        });
    }

    /**
     * Проверка заполнения данных отгрузки из курьером от клиента.
     *
     * @param boxExternalId     штрихкод коробки
     * @param mkSortingCenterId идентификатор партнёра МК СЦ
     * @return данные МК СЦ, куда отгружаем
     */
    @Step("Проверка сохранения данных отгрузки курьерского сегмента в ЛРМ")
    public ShipmentDestination verifyCourierSegmentShipment(
        String boxExternalId,
        Long mkSortingCenterId
    ) {
        log.debug("Check Courier segment shipment in LRM");
        return Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            ReturnSegment segment = Optional.ofNullable(response.getBoxes())
                .stream()
                .flatMap(Collection::stream)
                .map(ReturnBox::getSegments)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(
                    s -> s.getLogisticPoint() != null
                        && s.getLogisticPoint().getType() == LogisticPointType.COURIER
                )
                .findFirst()
                .orElse(null);
            Assertions.assertNotNull(segment, "Не найден курьерский сегмент для коробки " + boxExternalId);
            ShipmentDestination shipmentDestination = Optional.of(segment)
                .map(ReturnSegment::getShipment)
                .map(ReturnSegmentShipment::getDestination)
                .orElse(null);
            Assertions.assertNotNull(
                shipmentDestination,
                "Не найдена информация о логистической точке отгрузки для курьерский сегмента для коробки "
                    + boxExternalId
            );
            Assertions.assertNotNull(
                shipmentDestination.getLogisticPointId(),
                "Не найден идентификатор логистической точки отгрузки для курьерский сегмента для коробки "
                    + boxExternalId
            );
            Assertions.assertEquals(
                mkSortingCenterId,
                shipmentDestination.getPartnerId(),
                "Некорректный партнёр отгрузки для курьерский сегмента для коробки " + boxExternalId
            );
            return shipmentDestination;
        });
    }

    /**
     * Проверка создания сегмента СЦ.
     *
     * @param boxExternalId          штрихкод коробки
     * @param sortingCenterPartnerId идентификатор партнёра СЦ
     * @param sortingCenterPointId   идентификато логистической точки СЦ
     * @param shipmentPartnerId      идентификатор СЦ куда отгружаем
     * @return данные точки, куда отгружаем
     */
    @Step("Проверка создания СЦ сегмента в ЛРМ")
    public ShipmentDestination verifyScSegmentCreation(
        String boxExternalId,
        Long sortingCenterPartnerId,
        Long sortingCenterPointId,
        Long shipmentPartnerId
    ) {
        log.debug("Check SC segment creation in LRM");
        return Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            ReturnSegment segment = Optional.ofNullable(response.getBoxes())
                .stream()
                .flatMap(Collection::stream)
                .map(ReturnBox::getSegments)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(s ->
                    s.getLogisticPoint() != null
                        && s.getLogisticPoint().getType() == LogisticPointType.SORTING_CENTER
                        && sortingCenterPartnerId.equals(s.getLogisticPoint().getPartnerId())
                )
                .findFirst()
                .orElse(null);
            Assertions.assertNotNull(
                segment,
                "Не найден СЦ сегмент для коробки " + boxExternalId + " и партнёра " + sortingCenterPartnerId
            );
            Assertions.assertNotNull(
                segment.getLogisticPoint(),
                "Не найдена информация о логистической точке для СЦ сегмента " +
                    "для коробки " + boxExternalId + " и партнёра " + sortingCenterPartnerId
            );
            Assertions.assertEquals(
                sortingCenterPointId,
                segment.getLogisticPoint().getLogisticPointId(),
                "Некорректный идентификатор логистической точки для СЦ сегмента " +
                    "для коробки " + boxExternalId + " и партнёра " + sortingCenterPartnerId
            );
            ShipmentDestination shipmentDestination = Optional.of(segment)
                .map(ReturnSegment::getShipment)
                .map(ReturnSegmentShipment::getDestination)
                .orElse(null);
            Assertions.assertNotNull(
                shipmentDestination,
                "Не найдена информация о логистической точке отгрузки для СЦ сегмента " +
                    "для коробки " + boxExternalId + " и партнёра " + sortingCenterPartnerId
            );
            Assertions.assertNotNull(
                shipmentDestination.getLogisticPointId(),
                "Не найден идентификатор логистической точки отгрузки для СЦ сегмента " +
                    "для коробки " + boxExternalId + " и партнёра " + sortingCenterPartnerId
            );
            Assertions.assertEquals(
                shipmentPartnerId,
                shipmentDestination.getPartnerId(),
                "Некорректный партнёр отгрузки для СЦ сегмента " +
                    "для коробки " + boxExternalId + " и партнёра " + sortingCenterPartnerId
            );
            return shipmentDestination;
        });
    }

    @Step("Проверка создания сегмента последней мили в ЛРМ")
    public void verifyLastMileSegmentCreation(
        String boxExternalId,
        Long lastMilePartnerId,
        Long lastMilePointId,
        LogisticPointType lastMilePointType
    ) {
        log.debug("Check Last Mile segment creation in LRM");
        Retrier.retry(() -> {
            SearchReturn response = LRM_CLIENT.searchReturn(boxExternalId);
            ReturnSegment segment = Optional.ofNullable(response.getBoxes())
                .stream()
                .flatMap(Collection::stream)
                .map(ReturnBox::getSegments)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(s ->
                    s.getLogisticPoint() != null
                        && s.getLogisticPoint().getType() == lastMilePointType
                        && lastMilePartnerId.equals(s.getLogisticPoint().getPartnerId())
                )
                .findFirst()
                .orElse(null);
            Assertions.assertNotNull(
                segment,
                "Не найден сегмент последней мили " +
                    "для коробки " + boxExternalId + " и партнёра " + lastMilePartnerId
            );
            Assertions.assertNotNull(
                segment.getLogisticPoint(),
                "Не найдена информация о логистической точке для сегмента последней мили " +
                    "для коробки " + boxExternalId + " и партнёра " + lastMilePartnerId
            );
            Assertions.assertEquals(
                lastMilePointId,
                segment.getLogisticPoint().getLogisticPointId(),
                "Некорректный идентификатор логистической точки для сегмента последней мили " +
                    "для коробки " + boxExternalId + " и партнёра " + lastMilePartnerId
            );
        });
    }
}
