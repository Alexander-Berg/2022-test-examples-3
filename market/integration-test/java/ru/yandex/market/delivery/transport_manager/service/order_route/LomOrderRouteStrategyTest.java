package ru.yandex.market.delivery.transport_manager.service.order_route;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.LocationDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.ShipmentDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.WaybillSegmentDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRoute;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRouteType;
import ru.yandex.market.delivery.transport_manager.domain.enums.SegmentType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;

public class LomOrderRouteStrategyTest extends AbstractContextualTest {
    @Autowired
    private LomOrderRouteStrategy strategy;

//        LOM waybill segments for test

//        49691, FULFILLMENT, 0, 70562798,,2021 - 10 - 18, 10002464677, 10001781848
//        98851, SORTING_CENTER, 1, 32098076, IMPORT, 2021 - 10 - 19, 10002464677, 10001781848
//        101366, SORTING_CENTER, 2, 32098060, WITHDRAW, 2021 - 10 - 20, 10001781848, 10001804390
//        49784, SORTING_CENTER, 3, 32098008, WITHDRAW, 2021 - 10 - 20, 10001804390, 10001640163
//        1005705, MOVEMENT, 4, 25270688, WITHDRAW, 2021 - 10 - 20, 10001640163, 10001698701
//        1006455, PICKUP, 5, 5263477,,2021 - 10 - 21, 10001640163, 10001698701

    private static final List<WaybillSegmentDto> SEGMENTS = List.of(
        new WaybillSegmentDto()
            .setPartnerId(49691L)
            .setExternalId("70562798")
            .setPartnerType(PartnerType.DROPSHIP)
            .setSegmentType(SegmentType.FULFILLMENT)
            .setShipment(
                new ShipmentDto()
                    .setType(null)
                    .setDate(LocalDate.of(2021, 10, 18))
                    .setLocationFrom(new LocationDto().setWarehouseId(10002464677L))
                    .setLocationTo(new LocationDto().setWarehouseId(10001781848L))
            ),
        new WaybillSegmentDto()
            .setPartnerId(98851L)
            .setExternalId("32098076")
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setShipment(
                new ShipmentDto()
                    .setType(ShipmentType.IMPORT)
                    .setDate(LocalDate.of(2021, 10, 19))
                    .setLocationFrom(new LocationDto().setWarehouseId(10002464677L))
                    .setLocationTo(new LocationDto().setWarehouseId(10001781848L))
            ),
        new WaybillSegmentDto()
            .setPartnerId(101366L)
            .setExternalId("32098060")
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setShipment(
                new ShipmentDto()
                    .setType(ShipmentType.WITHDRAW)
                    .setDate(LocalDate.of(2021, 10, 20))
                    .setLocationFrom(new LocationDto().setWarehouseId(10001781848L))
                    .setLocationTo(new LocationDto().setWarehouseId(10001804390L))
            ),
        new WaybillSegmentDto()
            .setPartnerId(49784L)
            .setExternalId("32098008")
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setShipment(
                new ShipmentDto()
                    .setType(ShipmentType.WITHDRAW)
                    .setDate(LocalDate.of(2021, 10, 20))
                    .setLocationFrom(new LocationDto().setWarehouseId(10001804390L))
                    .setLocationTo(new LocationDto().setWarehouseId(10001640163L))
            ),
        new WaybillSegmentDto()
            .setPartnerId(1005705L)
            .setExternalId("25270688")
            .setSegmentType(SegmentType.MOVEMENT)
            .setPartnerType(PartnerType.DELIVERY)
            .setShipment(
                new ShipmentDto()
                    .setType(ShipmentType.WITHDRAW)
                    .setDate(LocalDate.of(2021, 10, 20))
                    .setLocationFrom(new LocationDto().setWarehouseId(10001640163L))
                    .setLocationTo(new LocationDto().setWarehouseId(10001698701L))
            ),
        new WaybillSegmentDto()
            .setPartnerId(1006455L)
            .setExternalId("5263477")
            .setPartnerType(PartnerType.FULFILLMENT)
            .setSegmentType(SegmentType.PICKUP)
            .setShipment(
                new ShipmentDto()
                    .setType(null)
                    .setDate(LocalDate.of(2021, 10, 21))
                    .setLocationFrom(new LocationDto().setWarehouseId(10001640163L))
                    .setLocationTo(new LocationDto().setWarehouseId(10001698701L))
            )
    );

    @Test
    void testConversion() {
        List<OrderRoute> routes = strategy.convert(new OrderRouteCreationData(1L, SEGMENTS, null));

        softly.assertThat(routes).containsExactly(
            new OrderRoute()
                .setOutboundPartnerId(49691L)
                .setOutboundPointId(10002464677L)
                .setOriginOutboundPointId(10002464677L)
                .setInboundPartnerId(98851L)
                .setInboundPointId(10001781848L)
                .setOriginInboundPointId(10001781848L)
                .setMovingPartnerId(49691L)
                .setInboundExternalId("32098076")
                .setOutboundPartnerType(ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.DROPSHIP)
                .setInboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setIndex(0)
                .setOrderId(1L)
                .setShipmentDate(LocalDateTime.of(2021, 10, 18, 0, 0))
                .setType(OrderRouteType.LOM),
            new OrderRoute()
                .setOutboundPartnerId(98851L)
                .setOutboundPointId(10001781848L)
                .setOriginOutboundPointId(10001781848L)
                .setInboundPartnerId(101366L)
                .setInboundPointId(10001804390L)
                .setOriginInboundPointId(10001804390L)
                .setMovingPartnerId(101366L)
                .setInboundExternalId("32098060")
                .setOutboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setInboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setIndex(1)
                .setOrderId(1L)
                .setShipmentDate(LocalDateTime.of(2021, 10, 20, 0, 0))
                .setType(OrderRouteType.LOM),
            new OrderRoute()
                .setOutboundPartnerId(101366L)
                .setOutboundPointId(10001804390L)
                .setOriginOutboundPointId(10001804390L)
                .setInboundPartnerId(49784L)
                .setInboundPointId(10001640163L)
                .setOriginInboundPointId(10001640163L)
                .setMovingPartnerId(49784L)
                .setInboundExternalId("32098008")
                .setOutboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setInboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setIndex(2)
                .setOrderId(1L)
                .setShipmentDate(LocalDateTime.of(2021, 10, 20, 0, 0))
                .setType(OrderRouteType.LOM),
            new OrderRoute()
                .setOutboundPartnerId(49784L)
                .setOutboundPointId(10001640163L)
                .setOriginOutboundPointId(10001640163L)
                .setInboundPartnerId(1006455L)
                .setInboundPointId(10001698701L)
                .setOriginInboundPointId(10001698701L)
                .setMovingPartnerId(1005705L)
                .setInboundExternalId("5263477")
                .setOutboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setInboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.FULFILLMENT
                )
                .setIndex(3)
                .setOrderId(1L)
                .setShipmentDate(LocalDateTime.of(2021, 10, 20, 0, 0))
                .setType(OrderRouteType.LOM)
        );

    }

    @Test
    void noPointIfDeliveryPickup() {
        List<WaybillSegmentDto> segments = List.of(
            new WaybillSegmentDto()
                .setPartnerId(1005705L)
                .setExternalId("25270688")
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setShipment(
                    new ShipmentDto()
                        .setType(ShipmentType.WITHDRAW)
                        .setDate(LocalDate.of(2021, 10, 20))
                        .setLocationFrom(new LocationDto().setWarehouseId(10001640163L))
                        .setLocationTo(new LocationDto().setWarehouseId(10001698701L))
                ),
            new WaybillSegmentDto()
                .setPartnerId(1006455L)
                .setExternalId("5263477")
                .setPartnerType(PartnerType.DELIVERY)
                .setSegmentType(SegmentType.PICKUP)
                .setShipment(
                    new ShipmentDto()
                        .setType(ShipmentType.WITHDRAW)
                        .setDate(LocalDate.of(2021, 10, 21))
                        .setLocationFrom(new LocationDto().setWarehouseId(10001640163L))
                        .setLocationTo(new LocationDto().setWarehouseId(10001698701L))
                )
        );

        List<OrderRoute> routes = strategy.convert(new OrderRouteCreationData(1L, segments, null));

        softly.assertThat(routes).containsExactly(
            new OrderRoute()
                .setOutboundPartnerId(1005705L)
                .setOutboundPointId(10001640163L)
                .setOriginOutboundPointId(10001640163L)
                .setInboundPartnerId(1006455L)
                .setInboundPointId(null)
                .setMovingPartnerId(1006455L)
                .setInboundExternalId("5263477")
                .setIndex(0)
                .setOrderId(1L)
                .setOutboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.SORTING_CENTER
                )
                .setInboundPartnerType(
                    ru.yandex.market.delivery.transport_manager.model.enums.PartnerType.DELIVERY
                )
                .setShipmentDate(LocalDateTime.of(2021, 10, 21, 0, 0))
                .setType(OrderRouteType.LOM)
        );
    }
}
