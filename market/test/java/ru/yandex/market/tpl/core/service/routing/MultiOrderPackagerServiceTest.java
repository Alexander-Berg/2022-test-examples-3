package ru.yandex.market.tpl.core.service.routing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class MultiOrderPackagerServiceTest extends TplAbstractTest {
    private final OrderGenerateService orderGenerateService;
    private final MultiOrderPackagerService multiOrderPackagerService;
    private final PickupPointRepository pickupPointRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    @AfterEach
    public void after() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMART_CONSOLIDATION_ORDERS_FOR_LAVKA, false);
    }

    @Test
    void pack() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientPhone("phone1")
                .build());

        List<MultiOrder> multiOrders = multiOrderPackagerService.pack(List.of(order));

        assertThat(multiOrders).hasSize(1);

        MultiOrder multiOrder = multiOrders.iterator().next();
        assertThat(multiOrder.getMultiOrderId()).isEqualTo(String.valueOf(order.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(order.getDelivery().getInterval().toRelativeTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(multiOrder.getMultiOrderExternalId()).isEqualTo(order.getExternalOrderId());
    }

    @Test
    void packPickupPointOrders() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMART_CONSOLIDATION_ORDERS_FOR_LAVKA, true);

        //Должны объединиться, так как лавка и одинаковые адреса 1
        PickupPoint pickupPointAddressA1 = PickupPointGenerator.generatePickupPoint(546467L);
        pickupPointAddressA1.setAddress("ADDRESS.  A,");
        pickupPointAddressA1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressA1);
        Order orderAddressA1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressA1)
                .build());

        PickupPoint pickupPointAddressA2 = PickupPointGenerator.generatePickupPoint(546468L);
        pickupPointAddressA2.setAddress("ADDRESSA");
        pickupPointAddressA2.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressA2);
        Order orderAddressA2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressA2)
                .build());

        //Нет объединения, адрес лавки уникален 2
        PickupPoint pickupPointAddressB1 = PickupPointGenerator.generatePickupPoint(546469L);
        pickupPointAddressB1.setAddress("ADDRESSB");
        pickupPointAddressB1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressB1);
        Order orderAddressB1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressB1)
                .build());

        //Нулевой адрес лавки, нет объединения, так как единственная привязка к PickupPoint 4
        PickupPoint pickupPointAddressNull1 = PickupPointGenerator.generatePickupPoint(546470L);
        pickupPointAddressNull1.setAddress(null);
        pickupPointAddressNull1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressNull1);
        Order orderAddressNull1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressNull1)
                .build());

        PickupPoint pickupPointAddressNull2 = PickupPointGenerator.generatePickupPoint(546471L);
        pickupPointAddressNull2.setAddress(null);
        pickupPointAddressNull2.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressNull2);
        Order orderAddressNull2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressNull2)
                .build());

        //Не лавка, должны объединиться по id PickupPoint 5
        PickupPoint pickupPointForNotLavka = PickupPointGenerator.generatePickupPoint(546472L);
        pickupPointForNotLavka.setAddress(null);
        pickupPointForNotLavka.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavka);
        Order orderNotLavka1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForNotLavka)
                .build());
        Order orderNotLavka2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForNotLavka)
                .build());


        //Лавка, должны объединиться по id PickupPoint, так как адрес нулевой и общий PickupPoint 6
        PickupPoint pickupPointForLavka = PickupPointGenerator.generatePickupPoint(546473L);
        pickupPointForLavka.setAddress(null);
        pickupPointForLavka.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointForLavka);
        Order orderForLavka1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForLavka)
                .build());
        Order orderForLavka2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForLavka)
                .build());

        //Не лавка, одинаковые адреса, разные id. Не объединяются 8
        PickupPoint pickupPointForNotLavkaSameAddress1 = PickupPointGenerator.generatePickupPoint(546474L);
        pickupPointForNotLavkaSameAddress1.setAddress("ADDRESS");
        pickupPointForNotLavkaSameAddress1.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavkaSameAddress1);
        Order orderForNotLavkaSameAddress1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .pickupPoint(pickupPointForNotLavkaSameAddress1)
                .build());

        PickupPoint pickupPointForNotLavkaSameAddress2 = PickupPointGenerator.generatePickupPoint(546475L);
        pickupPointForNotLavkaSameAddress2.setAddress("ADDRESS");
        pickupPointForNotLavkaSameAddress2.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavkaSameAddress2);
        Order orderForNotLavkaSameAddress2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .pickupPoint(pickupPointForNotLavkaSameAddress2)
                .build());

        List<MultiOrder> multiOrders = multiOrderPackagerService.pack(List.of(orderAddressA1, orderAddressA2,
                orderAddressB1, orderAddressNull1, orderAddressNull2, orderNotLavka1, orderNotLavka2, orderForLavka1,
                orderForLavka2, orderForNotLavkaSameAddress1, orderForNotLavkaSameAddress2));

        assertThat(multiOrders).hasSize(8);

    }

    @Test
    @DisplayName("Убираем заказ из списка если у него нулевые координаты")
    void packWithOneZeroCoordinatesOrderFromThePreviousWave() {

        GeoPoint geoPoint1 = GeoPoint.ofLatLon(0, 0);
        GeoPoint geoPoint2 = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());


        List<Long> list1 = List.of(order1.getId(), order2.getId());
        List<List<Long>> list = List.of(list1);
        HashMap<Long, List<List<Long>>> map = new HashMap<>();

        map.put(1L, list);
        var mults = multiOrderPackagerService.pack(List.of(order1, order2), BigDecimal.TEN, map);

        assertThat(mults).hasSize(1);
        assertThat(mults.get(0).getOrders()).hasSize(1);
        assertThat(mults.get(0).getOrders().get(0).getId()).isEqualTo(order2.getId());

    }
}
