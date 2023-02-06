package ru.yandex.market.pvz.core.test.factory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentRepository;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentItem;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;

public class TestShipmentsFactory {
    public static final long OPERATOR_ID = 1337;
    public static final String OPERATOR_LOGIN = "vasiliy_pupkin";

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Clock clock;

    @Transactional
    public Shipment createPendingShipment(Order... orders) {
        Order anyOrder = orders[0];
        Shipment pendingShipment = Shipment.builder()
                .type(ShipmentType.RECEIVE)
                .status(ShipmentStatus.PENDING)
                .responsibleUserId(OPERATOR_ID)
                .shipmentDate(LocalDate.now(clock))
                .transferId(UUID.randomUUID().toString())
                .pickupPointId(anyOrder.getPickupPoint().getId())
                .build();
        List<ShipmentItem> shipmentItems = StreamEx.of(orders)
                .map(order -> ShipmentItem.builder()
                        .createdAt(Instant.now(clock))
                        .shipment(pendingShipment)
                        .order(orderRepository.findByIdOrThrow(order.getId()))
                        .build())
                .toList();
        pendingShipment.setItems(shipmentItems);
        return shipmentRepository.save(pendingShipment);
    }
}
