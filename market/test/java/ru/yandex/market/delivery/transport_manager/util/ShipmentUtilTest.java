package ru.yandex.market.delivery.transport_manager.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderOperationTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ShipmentUtilTest {
    @Test
    void getShipmentTypeTest_intake() {
        Movement movement = new Movement().setPartnerId(1L);
        TransportationUnit inbound = new TransportationUnit().setPartnerId(1L);
        TransportationUnit outbound = new TransportationUnit().setPartnerId(2L);
        Transportation transportation =
            new Transportation().setMovement(movement).setInboundUnit(inbound).setOutboundUnit(outbound);
        OrderOperationTransportationType shipmentType = ShipmentUtil.getShipmentType(transportation);
        assertThat(shipmentType).isEqualTo(OrderOperationTransportationType.INTAKE);
    }

    @Test
    void getShipmentTypeTest_selfExport() {
        Movement movement = new Movement().setPartnerId(1L);
        TransportationUnit inbound = new TransportationUnit().setPartnerId(2L);
        TransportationUnit outbound = new TransportationUnit().setPartnerId(1L);
        Transportation transportation =
            new Transportation().setMovement(movement).setInboundUnit(inbound).setOutboundUnit(outbound);
        OrderOperationTransportationType shipmentType = ShipmentUtil.getShipmentType(transportation);
        assertThat(shipmentType).isEqualTo(OrderOperationTransportationType.SELF_EXPORT);
    }

    @Test
    void getShipmentTypeTest_thirdPartner() {
        Movement movement = new Movement().setPartnerId(1L);
        TransportationUnit inbound = new TransportationUnit().setPartnerId(2L);
        TransportationUnit outbound = new TransportationUnit().setPartnerId(3L);
        Transportation transportation =
            new Transportation().setMovement(movement).setInboundUnit(inbound).setOutboundUnit(outbound);
        OrderOperationTransportationType shipmentType = ShipmentUtil.getShipmentType(transportation);
        assertThat(shipmentType).isEqualTo(OrderOperationTransportationType.THIRD_PARTNER);
    }

    @Test
    void getShipmentTypeTest_notFound() {
        Movement movement = new Movement();
        TransportationUnit inbound = new TransportationUnit().setPartnerId(2L);
        TransportationUnit outbound = new TransportationUnit().setPartnerId(3L);
        Transportation transportation =
            new Transportation().setMovement(movement).setInboundUnit(inbound).setOutboundUnit(outbound);
        OrderOperationTransportationType shipmentType = ShipmentUtil.getShipmentType(transportation);
        assertThat(shipmentType).isEqualTo(OrderOperationTransportationType.UNKNOWN_YET);
    }
}
