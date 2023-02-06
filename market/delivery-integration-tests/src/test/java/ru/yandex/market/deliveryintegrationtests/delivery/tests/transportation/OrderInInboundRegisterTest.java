package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import dto.responses.tm.TmCheckpointStatus;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Slf4j
@Epic("TM")
@DisplayName("TM Test")
@Disabled(
    "Тест сильно зависит от времени выполнения. Запускать надо раз в день после того как такая возможность появится"
)
@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
public class OrderInInboundRegisterTest extends AbstractTransportationTest {

    private static final float PERCENT_ACCEPTING_ORDERS = 0.5f;

    private static final long WITHDRAW_OUTBOUND_PARTNER_ID = 48423; //DropShip_Эксперт по детскому питанию
    private static final long WITHDRAW_INBOUND_PARTNER_ID = 48; //Стриж

    private Long transportationId;

    @Test
    @DisplayName("Генерация транспортной накладной: заборная подтверждённая отгрузка")
    public void generateTransportationWaybill() {

        transportationId = TM_STEPS.getTransportationIdForDay(
            WITHDRAW_OUTBOUND_PARTNER_ID,
            WITHDRAW_INBOUND_PARTNER_ID,
            LocalDate.now(),
            null
        );

        long outboundRegisterId = TM_STEPS.getTransportationRegister(transportationId, 0);

        List<String> orderIdsInOutboundRegister = TM_STEPS.getOrderIdsInRegister(outboundRegisterId);

        int countOrdersForAccept = (int) (orderIdsInOutboundRegister.size() * PERCENT_ACCEPTING_ORDERS);

        List<String> acceptingOrderIds = orderIdsInOutboundRegister.stream()
            .limit(countOrdersForAccept)
            .collect(Collectors.toList());

        List<String> nonAcceptingOrderIds = orderIdsInOutboundRegister.stream()
            .filter(o -> !acceptingOrderIds.contains(o))
            .collect(Collectors.toList());

        //3PL партнер отвозит заказы на дропофф
        processTransportation();

        //Принимаем часть заказов на дропоффе
        acceptingOrderIds.forEach(this::acceptOrder);

        long inboundRegisterId = TM_STEPS.getTransportationRegister(transportationId, 1);

        List<String> orderIdsInInboundRegister = TM_STEPS.getOrderIdsInRegister(inboundRegisterId);

        Assertions.assertTrue(
            orderIdsInInboundRegister.containsAll(acceptingOrderIds),
            "В фактическом реестре нет заказов принятых на СЦ"
        );

        Assertions.assertTrue(
            orderIdsInInboundRegister.stream().noneMatch(nonAcceptingOrderIds::contains),
            "В фактическом реесте появились не принятые на СЦ заказы"
        );
    }

    private void processTransportation() {
        TM_STEPS.startTransportation(transportationId);
        TM_STEPS.getMovementExternalId(transportationId);
        long movementTrackerId =
            DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(TM_STEPS.getMovementIdWithPrefix(transportationId)).getId();

        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_COURIER_FOUND,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_OUTBOUND_WAREHOUSE_REACHED,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_DELIVERING,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_INBOUND_WAREHOUSE_REACHED,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_DELIVERED,
            EntityType.MOVEMENT
        );

        TM_STEPS.getMovementId(transportationId);
        TM_STEPS.getOutboundExternalId(transportationId);
        long outboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                TM_STEPS.getOutboundIdWithPrefix(transportationId),
                EntityType.OUTBOUND.getId()
            )
            .getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            outboundTrackerId,
            TmCheckpointStatus.OUTBOUND_TRANSFERRED,
            EntityType.OUTBOUND
        );
        TM_STEPS.verifyOutboundStatus(transportationId, "PROCESSED");

        long inboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                TM_STEPS.getInboundIdWithPrefix(transportationId),
                EntityType.INBOUND.getId()
            )
            .getId();

        TM_STEPS.verifyInboundStatus(transportationId, "ACCEPTED");

        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            inboundTrackerId,
            TmCheckpointStatus.INBOUND_ARRIVED,
            EntityType.INBOUND
        );
    }

    private void acceptOrder(String orderId) {
        Long dsSegmentOrderId = LOM_ORDER_STEPS.getLomOrderData(orderId).getWaybill()
            .stream()
            .filter(s -> s.getSegmentType() == SegmentType.SORTING_CENTER && s.getPartnerType() == PartnerType.DELIVERY)
            .findFirst()
            .map(WaybillSegmentDto::getId)
            .orElseThrow();

        String trackCode = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
            String.valueOf(dsSegmentOrderId),
            EntityType.ORDER.getId()
        ).getTrackCode();

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            Long.parseLong(trackCode),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            Long.parseLong(trackCode),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            Long.parseLong(trackCode),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED
        );
    }

}
