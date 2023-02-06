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
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
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
import ru.yandex.market.tpl.core.external.routing.api.MultiItem;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MultiItemMapperTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TestDataFactory testDataFactory;
    private final MultiOrderMapper multiOrderMapper;
    private final MultiItemMapper multiItemMapper;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnRepository clientReturnRepository;

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void mapOneOrderToOneMultiItem() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientPhone("phone1")
                .build());

        var multiItems = multiItemMapper.map(List.of(order), List.of());

        assertThat(multiItems).hasSize(1);

        var multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.valueOf(order.getId()));
    }

    @Test
    void mapOneClientReturnToOneMultiItem() {
        var cr = clientReturnGenerator.generateReturnFromClient();
        var multiItems = multiItemMapper.map(List.of(), List.of(cr));

        assertThat(multiItems).hasSize(1);

        var multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.valueOf(cr.getId()));
    }

    @Test
    void mapTwoOrdersToOneMultiItem() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order order = createClientOrder(geoPoint, "phone1", "10:00-14:00", 1);
        Order order2 = createClientOrder(geoPoint, "phone1", "12:00-16:00", 1);

        var multiItems = multiItemMapper.map(List.of(order, order2), List.of());

        assertThat(multiItems).hasSize(1);

        var multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.format("m_%d_%d", order.getId(), order2.getId()));
    }

    @Test
    void mapTwoClientReturnsToOneMultiItem() {
        var cr = clientReturnGenerator.generateReturnFromClient();
        var cr2 = clientReturnGenerator.generateReturnFromClient();

        var multiItems = multiItemMapper.map(List.of(), List.of(cr, cr2));

        assertThat(multiItems).hasSize(1);

        var multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.format("m_%d_%d", cr.getId(), cr2.getId()));
    }

    @Test
    void mapOrderAndMultipleClienReturns() {
        var geoPoint = GeoPointGenerator.generateLonLat();
        var gp = GeoPointGenerator.generateLonLat();
        var order = createClientOrder(geoPoint, "phone1", "10:00-14:00", 1);
        var order2 = createClientOrder(gp, "phone2", "10:00-14:00", 1);

        var cr = clientReturnGenerator.generateReturnFromClient();
        var cr2 = clientReturnGenerator.generateReturnFromClient();

        var multiItems = multiItemMapper.map(List.of(order, order2), List.of(cr, cr2));

        // should have 2 orders and 1 client return
        assertThat(multiItems).hasSize(3);
    }

    @Test
    void mapClientReturnAndOrderToDifferentMultiItems() {
        var geoPoint = GeoPointGenerator.generateLonLat();
        var order = createClientOrder(geoPoint, "phone1", "10:00-14:00", 1);
        var cr = clientReturnGenerator.generateReturnFromClient();

        var multiItems = multiItemMapper.map(List.of(order), List.of(cr));

        assertThat(multiItems).hasSize(2);
    }

    @Test
    void mapClientReturnAndOrderToOneMultiItem() {
        var geoPoint = GeoPointGenerator.generateLonLat();
        var order = createClientOrder(geoPoint, "phone1", "10:00-14:00", 1);
        var cr = clientReturnGenerator.generateReturnFromClient();
        cr.getLogisticRequestPointFrom().setOriginalLatitude(geoPoint.getLatitude());
        cr.getLogisticRequestPointFrom().setOriginalLongitude(geoPoint.getLongitude());
        cr.getClient().getClientData().setPhone("phone1");
        clientReturnRepository.save(cr);


        var multiItems = multiItemMapper.map(List.of(order), List.of(cr));

        assertThat(multiItems).hasSize(1);
        var multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.format("m_%d_%d", order.getId(), cr.getId()));
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

        var multiItems = multiItemMapper.map(List.of(
                client1Order, client1Order2, lockerOrder1, lockerOrder2
        ), List.of());

        assertThat(multiItems).hasSize(2);
        Map<Boolean, List<MultiItem>> isLockerToMultiOrder = multiItems.stream()
                .collect(Collectors.partitioningBy(mo -> mo.getOrders().get(0).isPickupPointDelivery()));
        var lockerMultiOrders = isLockerToMultiOrder.get(true);
        var clientMultiOrders = isLockerToMultiOrder.get(false);
        assertThat(lockerMultiOrders).hasSize(1);
        assertThat(clientMultiOrders).hasSize(1);

        var clientMultiItem = clientMultiOrders.iterator().next();
        assertThat(clientMultiItem.getMultiItemId()).isEqualTo(String.format("m_%d_%d", client1Order.getId(),
                client1Order2.getId()));

        var lockerMultiItem = lockerMultiOrders.iterator().next();
        assertThat(lockerMultiItem.getMultiItemId()).isEqualTo(String.format("m_%d_%d", lockerOrder1.getId(),
                lockerOrder2.getId()));

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

        var multiItems = multiItemMapper.map(List.of(client1Order, client1Order2), List.of());

        assertThat(multiItems).hasSize(2);

        Iterator<MultiItem> iterator = multiItems.iterator();
        var multiItem = iterator.next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.valueOf(client1Order.getId()));

        multiItem = iterator.next();
        assertThat(multiItem.getMultiItemId()).isEqualTo(String.valueOf(client1Order2.getId()));
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

        var multiItems = multiItemMapper.map(List.of(client1Order, client1Order2), List.of());

        assertThat(multiItems).hasSize(1);

        var multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId())
                .isEqualTo(String.format("m_%d_%d", client1Order.getId(), client1Order2.getId()));

        multiItem = multiItems.iterator().next();
        assertThat(multiItem.getMultiItemId())
                .isEqualTo(String.format("m_%d_%d", client1Order.getId(), client1Order2.getId()));
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

        var multiItems2 = multiOrderMapper.map(orders);
        var multiItems = multiItemMapper.map(orders, List.of());

        assertThat(multiItems).hasSize(3);
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
