package ru.yandex.market.tpl.core.domain.routing;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MultiOrderMapperTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TestDataFactory testDataFactory;
    private final MultiOrderMapper multiOrderMapper;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void mapOneOrderToOneMultiOrder() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientPhone("phone1")
                .build());

        List<MultiOrder> multiOrders = multiOrderMapper.map(List.of(order));

        assertThat(multiOrders).hasSize(1);

        MultiOrder multiOrder = multiOrders.iterator().next();
        assertThat(multiOrder.getMultiOrderId()).isEqualTo(String.valueOf(order.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(order.getDelivery().getInterval().toRelativeTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(multiOrder.getMultiOrderExternalId()).isEqualTo(order.getExternalOrderId());
    }

    @Test
    void mapTwoOrdersToOneMultiOrder() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order order = createClientOrder(geoPoint, "phone1", "10:00-14:00", 1);
        Order order2 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 1);

        List<MultiOrder> multiOrders = multiOrderMapper.map(List.of(order, order2));

        assertThat(multiOrders).hasSize(1);

        MultiOrder multiOrder = multiOrders.iterator().next();
        assertThat(multiOrder.getMultiOrderId()).isEqualTo(String.format("m_%d_%d", order.getId(), order2.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(RelativeTimeInterval.valueOf("12:00-14:00"));

        assertThat(multiOrder.getMultiOrderExternalId()).isEqualTo(String.format("%s_%s", order.getExternalOrderId(),
                order2.getExternalOrderId()));
    }

    @Test
    void mapBothClientAndLockerOrders() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order client1Order = createClientOrder(geoPoint, "phone1", "10:00-14:00", 1);
        Order client1Order2 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 1);

        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        Order lockerOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .pickupPoint(pickupPoint)
                .build());
        Order lockerOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("12:00-16:00"))
                .pickupPoint(pickupPoint)
                .build());

        List<MultiOrder> multiOrders = multiOrderMapper.map(List.of(
                client1Order, client1Order2, lockerOrder1, lockerOrder2
        ));

        assertThat(multiOrders).hasSize(2);
        Map<Boolean, List<MultiOrder>> isLockerToMultiOrder = multiOrders.stream()
                .collect(Collectors.partitioningBy(mo -> mo.getOrders().get(0).isPickupPointDelivery()));
        List<MultiOrder> lockerMultiOrders = isLockerToMultiOrder.get(true);
        List<MultiOrder> clientMultiOrders = isLockerToMultiOrder.get(false);
        assertThat(lockerMultiOrders).hasSize(1);
        assertThat(clientMultiOrders).hasSize(1);

        MultiOrder clientMultiOrder = clientMultiOrders.iterator().next();
        assertThat(clientMultiOrder.getMultiOrderId()).isEqualTo(String.format("m_%d_%d", client1Order.getId(),
                client1Order2.getId()));
        assertThat(clientMultiOrder.getInterval()).isEqualTo(RelativeTimeInterval.valueOf("12:00-14:00"));

        assertThat(clientMultiOrder.getMultiOrderExternalId()).isEqualTo(String.format("%s_%s",
                client1Order.getExternalOrderId(),
                client1Order2.getExternalOrderId()));

        MultiOrder lockerMultiOrder = lockerMultiOrders.iterator().next();
        assertThat(lockerMultiOrder.getMultiOrderId()).isEqualTo(String.format("m_%d_%d", lockerOrder1.getId(),
                lockerOrder2.getId()));

    }

    @Test
    @Disabled("MARKETTPL-2670")
    void mapByCapacity() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order priorClient1Order1Volume3 = createClientOrder(geoPoint, "phone1", "10:00-14:00", 3);
        Order priorClient1Order2Volume1 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 1);
        Order priorClient1Order3Volume3 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 3);
        Order priorClient1Order4Volume2 = createClientOrder(geoPoint, "phone1", "16:00-18:00", 2);
        Order priorClient2Order1Volume3 = createClientOrder(geoPoint, "phone2", "12:00-16:00", 3);

        Order newClient1Order1Volume6 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 6);
        Order newClient1Order2Volume1 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 1);
        Order newClient1Order3Volume4 = createClientOrder(geoPoint, "phone1", "16:00-18:00", 4);
        Order newClient2Order1Volume1 = createClientOrder(geoPoint, "phone2", "12:00-16:00", 1);

        BigDecimal courierCapacity = BigDecimal.valueOf(6);
        long courier1Id = 1L;
        long courier2Id = 2L;
        long courier3Id = 3L;
        List<Order> orders = List.of(
                priorClient1Order1Volume3,  //was on courier with id = 1, has volume = 3
                priorClient1Order2Volume1,  //was on courier with id = 1, has volume = 1
                priorClient1Order3Volume3,  //was on courier with id = 2, has volume = 3
                priorClient1Order4Volume2,  //was on courier with id = 3, has volume = 2
                priorClient2Order1Volume3,  //was on courier with id = 2, has volume = 3
                newClient1Order1Volume6,    //will be assigned at routing time, has volume = 6
                newClient1Order2Volume1,    //will be assigned to courier with id = 1, has volume = 1
                newClient1Order3Volume4,    //will be assigned to courier with id = 3, has volume = 4
                newClient2Order1Volume1     //will be assigned to courier with id = 2, has volume = 1
        );
        List<MultiOrder> multiOrders = multiOrderMapper.mapByCapacity(orders, courierCapacity, Map.of(
                courier1Id, List.of(List.of(priorClient1Order1Volume3.getId(), priorClient1Order2Volume1.getId())),
                courier2Id, List.of(List.of(priorClient1Order3Volume3.getId()),
                        List.of(priorClient2Order1Volume3.getId())),
                courier3Id, List.of(List.of(priorClient1Order4Volume2.getId()))
        ));

        assertThat(multiOrders)
                .filteredOn(mo -> Objects.equals(mo.getCourierId(), courier1Id))
                .is(new Condition<>(mos -> validateMultiOrdersAssigned(mos,
                        List.of(List.of(priorClient1Order1Volume3, priorClient1Order2Volume1,
                                newClient1Order2Volume1))),
                        "Первому курьеру добавился заказ объёмом 1 для первого клиента")
                );
        assertThat(multiOrders)
                .filteredOn(mo -> Objects.equals(mo.getCourierId(), courier2Id))
                .is(new Condition<>(mos -> validateMultiOrdersAssigned(mos,
                        List.of(List.of(priorClient1Order3Volume3), List.of(priorClient2Order1Volume3,
                                newClient2Order1Volume1))),
                        "Второму курьеру добавился заказ объёмом 1 для второго клиента")
                );
        assertThat(multiOrders)
                .filteredOn(mo -> Objects.equals(mo.getCourierId(), courier3Id))
                .is(new Condition<>(mos -> validateMultiOrdersAssigned(mos,
                        List.of(List.of(priorClient1Order4Volume2, newClient1Order3Volume4))),
                        "Третьему курьеру добавился заказ объёмом 4 для первого клиента")
                );
        assertThat(multiOrders)
                .filteredOn(mo -> mo.getCourierId() == null)
                .is(new Condition<>(mos -> validateMultiOrdersAssigned(mos,
                        List.of(List.of(newClient1Order1Volume6))),
                        "Пока не назначен заказ объёмом 6")
                );
    }

    private boolean validateMultiOrdersAssigned(List<? extends MultiOrder> multiOrders,
                                                List<List<Order>> expectedOrderGroups) {
        Set<Set<Long>> actualOrderIdGroups = StreamEx.of(multiOrders)
                .map(mo -> StreamEx.of(mo.getOrders())
                        .map(Order::getId)
                        .toSet())
                .toSet();
        Set<Set<Long>> expectedOrderIdGroups = StreamEx.of(expectedOrderGroups)
                .map(os -> StreamEx.of(os)
                        .map(Order::getId)
                        .toSet())
                .toSet();
        return actualOrderIdGroups.equals(expectedOrderIdGroups);
    }

    private Order createClientOrder(GeoPoint geoPoint,
                                    String clientPhone,
                                    String timeInterval,
                                    int volumeInCubicMeters) {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone(clientPhone)
                .deliveryInterval(LocalTimeInterval.valueOf(timeInterval))
                .dimensions(new Dimensions(BigDecimal.ONE, 100, 100, volumeInCubicMeters * 100))
                .build());
    }

    @Test
    void parseMultiOrderIdManySubTask() {
        String multiOrderId = String.format("m_%d_%d", 123456L, 789012L);

        List<Long> subTaskIds = MultiOrder.parseSubTaskIds(multiOrderId);

        assertThat(subTaskIds).hasSize(2);
        assertThat(subTaskIds).containsExactly(123456L, 789012L);
    }

    @Test
    void parseMultiOrderIdSingleSubTask() {
        String multiOrderId = "123456";

        List<Long> subTaskIds = MultiOrder.parseSubTaskIds(multiOrderId);

        assertThat(subTaskIds).hasSize(1);
        assertThat(subTaskIds.iterator().next()).isEqualTo(123456L);
    }

    @DisplayName("С флагом разделения b2b делим такой мульт на отдельные заказы")
    @Test
    void splitB2bOrdersByMultiOrderTest() {
        Map<String, OrderProperty> properties = Map.of(
                TplOrderProperties.Names.CUSTOMER_TYPE.name(),
                new OrderProperty(null, TplPropertyType.STRING, TplOrderProperties.Names.CUSTOMER_TYPE.name(),
                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name()
                )
        );
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order client1Order = createClientOrder(geoPoint, "phone1", "10:00-14:00", properties);
        Order client1Order2 = createClientOrder(geoPoint, "phone1", "12:00-16:00", properties);
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_SPLIT_MULTI_B2B_ORDER_ENABLED))
                .thenReturn(true);

        List<MultiOrder> multiOrders = multiOrderMapper.map(List.of(client1Order, client1Order2));

        assertThat(multiOrders).hasSize(2);

        Iterator<MultiOrder> iterator = multiOrders.iterator();
        MultiOrder multiOrder = iterator.next();
        assertThat(multiOrder.getMultiOrderId()).isEqualTo(String.valueOf(client1Order.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(client1Order.getDelivery().getInterval().toRelativeTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(multiOrder.getMultiOrderExternalId()).isEqualTo(client1Order.getExternalOrderId());

        multiOrder = iterator.next();
        assertThat(multiOrder.getMultiOrderId()).isEqualTo(String.valueOf(client1Order2.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(client1Order2.getDelivery().getInterval().toRelativeTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(multiOrder.getMultiOrderExternalId()).isEqualTo(client1Order2.getExternalOrderId());
    }

    @DisplayName("Когда флаг деления b2b выключени, b2b заказы объединяются в мульт")
    @Test
    void dontSplitB2bOrdersByMultiOrderTest() {
        Map<String, OrderProperty> properties = Map.of(
                TplOrderProperties.Names.CUSTOMER_TYPE.name(),
                new OrderProperty(null, TplPropertyType.STRING, TplOrderProperties.Names.CUSTOMER_TYPE.name(),
                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name()
                )
        );
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order client1Order = createClientOrder(geoPoint, "phone1", "10:00-14:00", properties);
        Order client1Order2 = createClientOrder(geoPoint, "phone1", "12:00-16:00", properties);
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_SPLIT_MULTI_B2B_ORDER_ENABLED))
                .thenReturn(false);

        List<MultiOrder> multiOrders = multiOrderMapper.map(List.of(client1Order, client1Order2));

        assertThat(multiOrders).hasSize(1);

        MultiOrder multiOrder = multiOrders.iterator().next();
        assertThat(multiOrder.getMultiOrderId())
                .isEqualTo(String.format("m_%d_%d", client1Order.getId(), client1Order2.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(RelativeTimeInterval.valueOf("12:00-14:00"));

        multiOrder = multiOrders.iterator().next();
        assertThat(multiOrder.getMultiOrderId())
                .isEqualTo(String.format("m_%d_%d", client1Order.getId(), client1Order2.getId()));
        assertThat(multiOrder.getInterval()).isEqualTo(RelativeTimeInterval.valueOf("12:00-14:00"));
    }

    @DisplayName("При включенном флаге разделения b2b мултизаказа, обычные заказы разбиваются на мульты правильно")
    @Test
    void mapTwoOrdersToTwoMultiOrder() {
        GeoPoint geoPoint = GeoPoint.ofLatLon(0, 0);
        List<Order> orders = List.of(
                createClientOrder(GeoPoint.ofLatLon(10, 0), "phone1", "10:00-14:00", 1),
                createClientOrder(GeoPoint.ofLatLon(20, 0), "phone2", "12:00-16:00", 1),
                createClientOrder(geoPoint, "phone", "10:00-14:00", 1),
                createClientOrder(geoPoint, "phone", "10:00-14:00", 1)

        );
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_SPLIT_MULTI_B2B_ORDER_ENABLED))
                .thenReturn(true);

        List<MultiOrder> multiOrders = multiOrderMapper.map(orders);

        assertThat(multiOrders).hasSize(3);
    }

    private Order createClientOrder(GeoPoint geoPoint,
                                    String clientPhone,
                                    String timeInterval,
                                    Map<String, OrderProperty> propertyMap) {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone(clientPhone)
                .deliveryInterval(LocalTimeInterval.valueOf(timeInterval))
                .properties(propertyMap)
                .build());
    }
}
