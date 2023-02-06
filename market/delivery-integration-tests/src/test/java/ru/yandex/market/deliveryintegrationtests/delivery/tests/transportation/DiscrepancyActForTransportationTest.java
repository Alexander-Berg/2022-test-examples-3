package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import dto.responses.tm.TmCheckpointStatus;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;
import toolkit.Retrier;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest;
import ru.yandex.market.logistics4shops.client.model.Outbound;

@Slf4j
@Epic("TM")
@DisplayName("TM Test")
@Disabled(
    "Тест сильно зависит от времени выполнения. Запускать надо раз в день после того как такая возможность появится"
)
@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
public class DiscrepancyActForTransportationTest extends AbstractTransportationTest {

    private static final float PERCENT_ACCEPTING_ORDERS = 0.5f;

    private static final long DROPSHIP_ID = 47924;
    private static final long SC_ID = 47819;
    private static final long SHOP_ID = 10361574;
    private static final long USER_ID = 1003558477;

    private Long transportationId;

    @Test
    @DisplayName("Генерация транспортной накладной: заборная подтверждённая отгрузка")
    public void generateTransportationWaybill() {

        transportationId = TM_STEPS.getTransportationIdForDay(
            DROPSHIP_ID,
            SC_ID,
            LocalDate.now(),
            TransportationStatus.WAITING_DEPARTURE
        );

        long outboundRegisterId = TM_STEPS.getTransportationRegister(transportationId, 0);

        List<String> orderIdsInOutboundRegister = TM_STEPS.getOrderIdsInRegister(outboundRegisterId);

        int countOrdersForAccept = (int) (orderIdsInOutboundRegister.size() * PERCENT_ACCEPTING_ORDERS);

        List<Order> ordersInOutboundRegister = orderIdsInOutboundRegister.stream()
            .map(Long::parseLong)
            .map(CHECKOUTER_STEPS::getOrder)
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .collect(Collectors.toList());

        ordersInOutboundRegister.forEach(this::confirmOrder);

        List<Order> acceptingOrders = ordersInOutboundRegister.stream()
            .limit(countOrdersForAccept)
            .collect(Collectors.toList());

        var request = PartnerShipmentConfirmRequest.builder()
            .orderIds(
                ordersInOutboundRegister.stream()
                    .map(Order::getId)
                    .collect(Collectors.toList())
            )
            .build();
        Retrier.retry(() -> NESU_STEPS.confirmShipment(transportationId, USER_ID, SHOP_ID, request));

        //Дропшип отвозит заказы на дропофф
        processTransportation();

        //Принимаем часть заказов на дропоффе
        acceptingOrders.forEach(this::acceptOrder);

        String inboundExternalId = TM_STEPS.getInboundIdWithPrefix(transportationId);

        //Завершаем отгрузку
        SCINT_STEPS.fixInbound(inboundExternalId);
        long inboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                inboundExternalId,
                EntityType.INBOUND.getId()
            )
            .getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            inboundTrackerId,
            TmCheckpointStatus.INBOUND_ACCEPTED,
            EntityType.INBOUND
        );

        //Проверяем готовность акта расхождений
        NESU_STEPS.verifyDiscrepancyActDownloadAvailability(transportationId, USER_ID, SHOP_ID);

        //Проверяем существование пути до файла с актом расхождений
        Outbound l4sOutbound = L4S_STEPS.getOutbound(TM_STEPS.getOutboundIdWithPrefix(transportationId));
        Assertions.assertNotNull(l4sOutbound.getDiscrepancyActPath());
        Assertions.assertTrue(StringUtils.isNotBlank(l4sOutbound.getDiscrepancyActPath().getFilename()));
        Assertions.assertTrue(StringUtils.isNotBlank(l4sOutbound.getDiscrepancyActPath().getBucket()));
        Assertions.assertEquals(Boolean.TRUE, l4sOutbound.getDiscrepancyActIsReady());
    }

    private void processTransportation() {
        TM_STEPS.startTransportation(transportationId);

        TM_STEPS.getOutboundExternalId(transportationId);
        long outboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                TM_STEPS.getOutboundIdWithPrefix(transportationId),
                EntityType.OUTBOUND.getId()
            )
            .getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            outboundTrackerId,
            TmCheckpointStatus.OUTBOUND_ASSEMBLED,
            EntityType.OUTBOUND
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            outboundTrackerId,
            TmCheckpointStatus.OUTBOUND_TRANSFERRED,
            EntityType.OUTBOUND
        );
    }

    private void acceptOrder(Order order) {
        String orderExternalId = order.getId().toString();
        SCINT_STEPS.acceptPlace(orderExternalId, orderExternalId, SC_ID);
    }

    private void confirmOrder(Order order) {
        String orderExternalId = order.getId().toString();
        long trackCode = LOM_ORDER_STEPS.getLomOrderData(orderExternalId).getWaybill()
            .stream()
            .filter(s -> s.getPartnerType() == PartnerType.DROPSHIP)
            .findFirst()
            .map(WaybillSegmentDto::getTrackerId)
            .orElseThrow();

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackCode,
            OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackCode,
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackCode,
            OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED
        );
    }
}
