package ru.yandex.market.tpl.core.domain.sc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ORDER_WAREHOUSE_DEFAULT_SCHEDULE_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FulfillmentLgwDtoFactoryTest {

    private final DsRepository dsRepository;
    private final FulfillmentLgwDtoFactory fulfillmentLgwDtoFactoryUnderTest;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final OrderWarehouseRepository orderWarehouseRepository;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    private LocalDate deliveryDate;

    @BeforeEach
    void setup() {
        deliveryDate = LocalDate.now();
    }

    @Test
    void createOrder() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(deliveryDate)
                        .build());
        DeliveryService deliveryService = dsRepository.getOne(order.getDeliveryServiceId());
        var fulfillmentOrder = fulfillmentLgwDtoFactoryUnderTest.createOrder(order, deliveryDate, null, null);
        assertThat(fulfillmentOrder.getWarehouse().getIncorporation()).isEqualTo("ООО Ромашка");
        assertThat(getShipmentDate(fulfillmentOrder)).isEqualTo(getDeliveryDate(order));
        assertThat(fulfillmentOrder.getDelivery().getCourier().getPersons().get(0))
                .isEqualTo(new Person.PersonBuilder("UNKNOWN_COURIER").setId(404L).build());
        assertThat(fulfillmentOrder.getDeliveryType()).isEqualTo(DeliveryType.COURIER);
        assertThat(fulfillmentOrder.getDelivery().getName()).isEqualTo(deliveryService.getName());
    }

    @Test
    void createOrderPickupPoint() {
        PickupPoint lockerPickupPoint = new PickupPoint();
        lockerPickupPoint.setCode("test1");
        lockerPickupPoint.setPartnerSubType(PartnerSubType.LOCKER);
        lockerPickupPoint.setType(PickupPointType.LOCKER);
        lockerPickupPoint.setLogisticPointId(1L);
        PickupPoint savedLockerPickupPoint = pickupPointRepository.save(lockerPickupPoint);

        Order orderLocker = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(savedLockerPickupPoint)
                        .deliveryDate(deliveryDate)
                        .build()
        );
        var fulfillmentOrderLocker = fulfillmentLgwDtoFactoryUnderTest.createOrder(orderLocker, deliveryDate, null,
                null);
        assertThat(fulfillmentOrderLocker.getDeliveryType()).isEqualTo(DeliveryType.PICKUP_POINT);

        PickupPoint pvzPickupPoint = new PickupPoint();
        pvzPickupPoint.setCode("test2");
        pvzPickupPoint.setPartnerSubType(PartnerSubType.PVZ);
        pvzPickupPoint.setType(PickupPointType.PVZ);
        pvzPickupPoint.setLogisticPointId(2L);
        PickupPoint savedPvzPickupPoint = pickupPointRepository.save(pvzPickupPoint);

        Order orderPvz = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(savedPvzPickupPoint)
                        .deliveryDate(deliveryDate)
                        .build()
        );
        var fulfillmentOrderPvz = fulfillmentLgwDtoFactoryUnderTest.createOrder(orderPvz, deliveryDate, null, null);
        assertThat(fulfillmentOrderPvz.getDeliveryType()).isEqualTo(DeliveryType.PICKUP_POINT);
    }

    @Test
    void createOrder2SCFlowParentSc() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(deliveryDate)
                        .build());
        DeliveryService deliveryService = dsRepository.getOne(order.getDeliveryServiceId());
        int distanceInDays = 3;
        ScEdge scEdge = new ScEdge(false, new ScEdge.Params(order.getDeliveryServiceId(), 456L, 789L, distanceInDays));
        var fulfillmentOrder = fulfillmentLgwDtoFactoryUnderTest.createOrder(order,
                deliveryDate.minusDays(distanceInDays), null, scEdge);
        assertThat(fulfillmentOrder.getWarehouse().getIncorporation()).isEqualTo("ООО Ромашка");
        assertThat(getShipmentDate(fulfillmentOrder)).isEqualTo(getDeliveryDate(order).minusDays(distanceInDays));
        assertThat(fulfillmentOrder.getDelivery().getCourier().getPersons().get(0))
                .isEqualTo(new Person.PersonBuilder(deliveryService.getName())
                        .setId(1100000000000000L + deliveryService.getId())
                        .build()
                );
    }

    @Test
    void createOrder2SCFlowChildSc() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(deliveryDate)
                        .build());
        ScEdge scEdge = new ScEdge(true, new ScEdge.Params(order.getDeliveryServiceId(), SortingCenter.DEFAULT_SC_ID,
                789L, 1));
        var fulfillmentOrder = fulfillmentLgwDtoFactoryUnderTest.createOrder(order, deliveryDate, null, scEdge);
        assertThat(fulfillmentOrder.getWarehouse().getIncorporation()).isEqualTo("Маркет ПВЗ");
        assertThat(getShipmentDate(fulfillmentOrder)).isEqualTo(getDeliveryDate(order));
        assertThat(fulfillmentOrder.getDelivery().getCourier().getPersons().get(0))
                .isEqualTo(new Person.PersonBuilder("UNKNOWN_COURIER").setId(404L).build());
    }

    @Test
    void defaultOrderWarehause() {
        when(configurationProviderAdapter.isBooleanEnabled(ORDER_WAREHOUSE_DEFAULT_SCHEDULE_ENABLED))
                .thenReturn(true);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(deliveryDate)
                        .build());
        OrderWarehouse warehouse = new OrderWarehouse("123", "corp", new OrderWarehouseAddress(
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                1,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ), Map.of(), Collections.emptyList(), null, null);
        orderWarehouseRepository.saveAndFlush(warehouse);
        order.setWarehouse(warehouse);
        order.setWarehouseReturn(warehouse);
        ScEdge scEdge = new ScEdge(true, new ScEdge.Params(order.getDeliveryServiceId(), SortingCenter.DEFAULT_SC_ID,
                789L, 1));
        var fulfillmentOrder = fulfillmentLgwDtoFactoryUnderTest.createOrder(order, deliveryDate, null, scEdge);
        assertThat(fulfillmentOrder.getWarehouseFrom().getIncorporation()).isEqualTo("corp");
        assertThat(fulfillmentOrder.getWarehouseFrom().getSchedule().size()).isEqualTo(7);


    }

    private LocalDate getDeliveryDate(Order order) {
        return order.getDelivery().getDeliveryDate(TimeZoneUtil.DEFAULT_OFFSET);
    }

    private LocalDate getShipmentDate(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order fulfillmentOrder) {
        return fulfillmentOrder.getShipmentDate().getOffsetDateTime().toLocalDate();
    }
}
