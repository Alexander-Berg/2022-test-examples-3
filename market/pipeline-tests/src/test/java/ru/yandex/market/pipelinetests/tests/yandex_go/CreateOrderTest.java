package ru.yandex.market.pipelinetests.tests.yandex_go;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dto.responses.lgw.message.get_order.Order;
import dto.responses.tpl.pvz.PvzOrderDto;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import toolkit.FileUtil;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_AT_START;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_AT_START_SORT;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED;

@DisplayName("Yandex Go Test")
@Epic("Yandex Go")
@Tag("YandexGoOrderTest")
public class CreateOrderTest extends AbstractYandexGoTest {

    private static final Set<SegmentType> DS_API_SEGMENT_TYPES = EnumSet.of(
        SegmentType.MOVEMENT,
        SegmentType.PICKUP,
        SegmentType.COURIER
    );

    private static final Long TPL_PVZ_ID = 1001489032L;

    private static final String CIS_FULL = "010942102361011221oPJf79NVw;8so\u001d91TWFxiUG;06" +
        "\u001d92SWthRXp6NXEnMGtEM3FyMWpVVTYleTtiamluPmdFM3hGJVMieiVIMSpXO3JXY2NUVVdxN0U3Tk9VZGswakFUYzI=";
    private static final String CIS = "010942102361011221oPJf79NVw;8so";

    // убрать после фикса MARKETTPLPVZ-2418
    private static final String CIS_FULL_PVZ = "010942102361011221oPJf79NVw;8so91TWFxiUG;06" +
        "92SWthRXp6NXEnMGtEM3FyMWpVVTYleTtiamluPmdFM3hGJVMieiVIMSpXO3JXY2NUVVdxN0U3Tk9VZGswakFUYzI=";

    @Test
    @DisplayName("Создание и доставка заказа в ПВЗ")
    void pvzOrderTest() {
        String corporateClientId = "2283221488";
        OrderDto lomOrder = createOrderFlow(corporateClientId, getPvzRequest());
        checkCisesInPvz(lomOrder);
        deliverOrderFlow(corporateClientId, lomOrder);
    }

    @Test
    @DisplayName("Создание и доставка курьерского заказа")
    void courierOrderTest() {
        String corporateClientId = "gifts";
        OrderDto lomOrder = createOrderFlow(corporateClientId, getCourierRequest());
        checkCisesInCourier(lomOrder);
        deliverOrderFlow(corporateClientId, lomOrder);
    }

    @Test
    @DisplayName("Создание курьерского заказа, отмена после создания")
    void cancelOrderTest() {
        String corporateClientId = "gifts";
        OrderDto lomOrder = createOrderFlow(corporateClientId, getCourierRequest());
        checkCisesInCourier(lomOrder);
        cancelOrder(corporateClientId, lomOrder);
    }

    @Nonnull
    private OrderDto createOrderFlow(String corporateClientId, JsonNode request) {
        String orderExternalId = LOG_PLATFORM_STEPS.createAndConfirmOrder(corporateClientId, request);
        OrderDto lomOrder = LOM_ORDER_STEPS.getLomOrderData(orderExternalId);
        long lomOrderId = lomOrder.getId();
        checkCisesInLom(lomOrder);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        return LOM_ORDER_STEPS.verifyAllSegmentsAreCreated(lomOrderId);
    }

    private void checkCisesInLom(OrderDto lomOrder) {
        Map<String, String> instances = lomOrder.getItems().get(0).getInstances().get(0);
        Assertions.assertEquals(CIS_FULL, instances.get("CIS_FULL"), "CIS_FULL is not equal in LOM");
        Assertions.assertEquals(CIS, instances.get("CIS"), "CIS is not equal in LOM");
    }

    private void checkCisesInPvz(OrderDto lomOrder) {
        verifyUpdateItemsInstancesChangeOrderRequestFinished(lomOrder);
        PvzOrderDto pvzOrder = TPL_PVZ_STEPS.getOrder(TPL_PVZ_ID, lomOrder.getBarcode());
        Assertions.assertEquals(
            CIS_FULL_PVZ,
            pvzOrder.getItems().get(0).getCisValues().get(0),
            "CIS_FULL is not equal in TPL-PVZ"
        );
    }

    private void verifyUpdateItemsInstancesChangeOrderRequestFinished(OrderDto lomOrder) {
        LOM_ORDER_STEPS.verifyChangeRequest(
            lomOrder.getId(),
            ChangeOrderRequestType.UPDATE_ITEMS_INSTANCES,
            Set.of(ChangeOrderRequestStatus.SUCCESS)
        );
    }

    private void checkCisesInCourier(OrderDto lomOrder) {
        verifyUpdateItemsInstancesChangeOrderRequestFinished(lomOrder);
        long courierPartnerId = lomOrder.getWaybill().stream()
            .filter(segment -> segment.getSegmentType() == SegmentType.COURIER)
            .map(WaybillSegmentDto::getPartnerId)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
        Order dsOrder = LGW_STEPS.dsGetOrder(lomOrder.getBarcode(), courierPartnerId);
        Map<String, String> instances = dsOrder.getOrder().getItems().get(0).getInstances().get(0);
        Assertions.assertEquals(CIS_FULL, instances.get("CIS_FULL"), "CIS_FULL is not equal in MK");
        Assertions.assertEquals(CIS, instances.get("CIS"), "CIS is not equal in MK");
    }

    private void deliverOrderFlow(String corporateClientId, OrderDto lomOrder) {
        for (WaybillSegmentDto segment : lomOrder.getWaybill()) {
            SegmentType segmentType = segment.getSegmentType();

            if (segmentType == SegmentType.SORTING_CENTER) {
                ffApiFlow(lomOrder.getId(), segment.getId(), segment.getTrackerId());
            }

            if (DS_API_SEGMENT_TYPES.contains(segmentType)) {
                dsApiFlow(lomOrder.getId(), segment.getId(), segment.getTrackerId(), segmentType);
            }
        }
        LOG_PLATFORM_STEPS.verifyOrderHasStatus(corporateClientId, lomOrder.getExternalId(), "DELIVERY_DELIVERED");
    }

    private void cancelOrder(String corporateClientId, OrderDto lomOrder) {
        LOG_PLATFORM_STEPS.cancelOrder(corporateClientId, lomOrder.getExternalId());
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrder.getId(), OrderStatus.CANCELLED);
    }

    private void ffApiFlow(Long lomOrderId, Long segmentId, Long trackerId) {
        sendAndVerifyCheckpoints(lomOrderId, segmentId, trackerId, List.of(
            SORTING_CENTER_AT_START,
            SORTING_CENTER_PREPARED,
            SORTING_CENTER_TRANSMITTED
        ));
    }

    private void dsApiFlow(Long lomOrderId, Long segmentId, Long trackerId, SegmentType segmentType) {
        sendAndVerifyCheckpoints(lomOrderId, segmentId, trackerId, List.of(DELIVERY_AT_START));

        if (segmentType == SegmentType.COURIER) {
            sendAndVerifyCheckpoints(lomOrderId, segmentId, trackerId, List.of(
                DELIVERY_AT_START_SORT,
                DELIVERY_TRANSPORTATION_RECIPIENT,
                DELIVERY_TRANSMITTED_TO_RECIPIENT
            ));
        } else {
            sendAndVerifyCheckpoints(lomOrderId, segmentId, trackerId, List.of(
                DELIVERY_TRANSPORTATION,
                DELIVERY_ARRIVED_PICKUP_POINT
            ));
        }

        if (segmentType == SegmentType.PICKUP || segmentType == SegmentType.COURIER) {
            sendAndVerifyCheckpoints(lomOrderId, segmentId, trackerId, List.of(DELIVERY_DELIVERED));
        }
    }

    private void sendAndVerifyCheckpoints(
        Long lomOrderId,
        Long segmentId,
        Long trackerId,
        List<OrderDeliveryCheckpointStatus> checkpoints
    ) {
        for (OrderDeliveryCheckpointStatus checkpoint : checkpoints) {
            TRACKER_STEPS.addOrderCheckpointToTracker(trackerId, checkpoint);
            L4G_STEPS.verifySegmentHasCheckpointStatus(lomOrderId, segmentId, checkpoint);
        }
    }

    @Nonnull
    private static JsonNode getPvzRequest() {
        ObjectNode request = (ObjectNode) FileUtil.jsonNodeFromFile("yandex_go/create_pvz_order.json");
        long nextMondayTimestamp = LocalDate.now()
            .plusDays(1)
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atTime(14, 0)
            .toInstant(ZoneOffset.UTC)
            .getEpochSecond();
        ((ObjectNode) request.path("destination").path("interval"))
            .put("from", nextMondayTimestamp)
            .put("to", nextMondayTimestamp);
        ((ObjectNode) request.path("info")).put("operator_request_id", UUID.randomUUID().toString());
        return request;
    }

    @Nonnull
    private static JsonNode getCourierRequest() {
        ObjectNode request = (ObjectNode) FileUtil.jsonNodeFromFile("yandex_go/create_courier_order.json");
        Instant today = LocalDate.now().atTime(14, 0).toInstant(ZoneOffset.UTC);
        ((ObjectNode) request.path("destination").path("interval_utc"))
            .put("from", today.plus(1, ChronoUnit.DAYS).toString())
            .put("to", today.plus(10, ChronoUnit.DAYS).toString());
        ((ObjectNode) request.path("info")).put("operator_request_id", UUID.randomUUID().toString());
        return request;
    }
}
