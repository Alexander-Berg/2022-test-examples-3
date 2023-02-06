package ru.yandex.market.tpl.core.service.order.collector;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.routing.RoutableKey;
import ru.yandex.market.tpl.core.domain.routing.logistic_request.Routable;
import ru.yandex.market.tpl.core.domain.routing.logistic_request.RoutableCollector;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.service.order.OrderFeaturesResolver;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.routing.MultiOrderMapper.GEO_POINT_SCALE;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class OrderRoutableCollectorTest extends TplAbstractTest {

    private static final Set<Long> DS_IDS = Set.of(DeliveryService.DEFAULT_DS_ID);

    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;
    private final OrderRoutableCollector orderRoutableCollector;
    private final RoutingRequestOrderCollector orderCollector;
    private final SortingCenterService sortingCenterService;
    private final OrderGenerateService orderGenerateService;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final ShiftManager shiftManager;
    private final Clock clock;
    private final OrderFeaturesResolver orderFeaturesResolver;

    private LocalDate shiftDate;
    private Shift shift;

    @BeforeEach
    void setup() {
        var sc = sortingCenterService.findSortCenterForDs(DeliveryService.DEFAULT_DS_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sc, SortingCenterProperties.ROUTING_ORDERS_AS_ROUTABLE_ENABLED, true
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sc, SortingCenterProperties.ROUTABLE_ROUTING_ENABLED, true
        );
        shiftDate = LocalDate.now(clock).plusDays(1);
        shift = shiftManager.findOrCreate(shiftDate, sc.getId());
    }

    @Test
    void collectSameOrdersTest() {

        generateOrders();
        var now = Instant.now(clock);

        var collectedOrders = orderCollector.collect(shift, DS_IDS, RoutingMockType.REAL, now);
        var collectedRoutableItems = orderRoutableCollector.collect(
                RoutableCollector.Command.builder()
                        .shift(shift)
                        .dsIds(DS_IDS)
                        .mockType(RoutingMockType.REAL)
                        .requestCreatedAt(now)
                        .build()
        );

        assertThat(collectedOrders).hasSize(OrderRepository.STATUSES_TO_DELIVER.size());
        assertThat(collectedOrders.size()).isEqualTo(collectedRoutableItems.getRoutableItems().size());

    }

    @Test
    void collectForUserShiftTest() {

        var user = testUserHelper.findOrCreateUser(12345L);
        var userShiftId = testUserHelper.createEmptyShift(user, LocalDate.now()).getId();

        var orderTask1 = testDataFactory.addLockerDeliveryTask(userShiftId);
        var orderTask2 = testDataFactory.addDeliveryTaskAuto(user, userShiftId,
                OrderPaymentStatus.PAID, OrderPaymentType.PREPAID);
        var specialRequestTask = testDataFactory.addFlowTask(userShiftId, TaskFlowType.LOCKER_INVENTORY);

        var itemsToReroute = transactionTemplate.execute(s -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            return orderRoutableCollector.collect(userShift);
        });

        assertThat(itemsToReroute).hasSize(2);
        var routableIds = itemsToReroute.stream()
                .flatMap(i -> i.getRoutableItems().stream())
                .map(Routable::getId)
                .collect(Collectors.toSet());

        assertThat(routableIds).containsAll(orderTask1.getOrderIds());
        assertThat(routableIds).containsAll(orderTask2.getOrderIds());
    }

    @Test
    void mapRoutableTest() {
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate)
                .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                .flowStatus(OrderFlowStatus.CREATED)
                .build()
        );

        var collectedRoutableItems = orderRoutableCollector.collect(
                RoutableCollector.Command.builder()
                        .shift(shift)
                        .dsIds(DS_IDS)
                        .mockType(RoutingMockType.REAL)
                        .requestCreatedAt(Instant.now(clock))
                        .build()
        );

        assertThat(collectedRoutableItems.getRoutableItems()).hasSize(1);
        var routable = collectedRoutableItems.getRoutableItems().get(0);
        assertThatRoutableMatchOrder(routable, order);
    }

    @Test
    void shouldRemoveOrdersWithZeroCoordinates() {

        GeoPoint geoPoint1 = GeoPoint.ofLatLon(0,0);
        GeoPoint geoPoint2 = GeoPoint.ofLatLon(0,0);

        orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint1)
                        .build())
                .build());
        orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint2)
                        .build())
                .build());

        var collectedOrders = orderRoutableCollector.collect(
                RoutableCollector.Command.builder()
                        .shift(shift)
                        .dsIds(DS_IDS)
                        .mockType(RoutingMockType.REAL)
                        .requestCreatedAt(Instant.now(clock))
                        .build()
        );

        assertThat(collectedOrders.getRoutableItems()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void dropFarOrdersWhenEnabledForSc(boolean dropOrdersEnabled) {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.IS_DROP_FAR_ORDERS_FROM_ROUTING_ENABLED, dropOrdersEnabled);

        var order = transactionTemplate.execute(s -> {
            var newOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .deliveryDate(shiftDate)
                    .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                    .build());
            newOrder.setIsAddressValid(false);
            newOrder.setAddressValidatedAt(Instant.now());
            return orderRepository.save(newOrder);
        });

        var collectResult = orderRoutableCollector.collect(
                RoutableCollector.Command.builder()
                        .shift(shift)
                        .dsIds(DS_IDS)
                        .mockType(RoutingMockType.REAL)
                        .requestCreatedAt(Instant.now(clock))
                        .build()
        );

        if (dropOrdersEnabled) {
            assertThat(collectResult.getRoutableItems()).isEmpty();
            assertThat(collectResult.getDroppedItemIds()).hasSize(1);
        } else {
            assertThat(collectResult.getRoutableItems()).hasSize(1);
            assertThat(collectResult.getDroppedItemIds()).isEmpty();
        }
    }

    private void assertThatRoutableMatchOrder(Routable routable, Order order) {
        var offset = dsZoneOffsetCachingService.getOffsetForDs(order.getDeliveryServiceId());

        assertThat(routable.getId()).isEqualTo(order.getId());
        assertThat(routable.getDeliveryServiceId()).isEqualTo(order.getDeliveryServiceId());
        assertThat(routable.getGroupIdPrefix()).isEqualTo("m");
        assertThat(routable.getRoutingRequestItemType()).isEqualTo(RoutingRequestItemType.CLIENT);
        assertThat(routable.getRef()).isEqualTo(order.getExternalOrderId());
        assertThat(routable.getAddress()).isEqualTo(order.getDelivery().getRoutePointAddress());
        assertThat(routable.getVolume()).isEqualTo(order.getOrderVolume());
        assertThat(routable.getDimensionsClass()).isEqualTo(order.getDimensionsClass());
        assertThat(routable.getRegionId()).isEqualTo(order.getDelivery().getDeliveryAddress().getRegionId());
        assertThat(routable.isWithTrying()).isEqualTo(orderFeaturesResolver.isFashion(order));
        assertThat(routable.isDropOffReturn()).isFalse();
        assertThat(routable.isDropIfSingleOnLocation()).isFalse();
        assertThat(routable.isExcludedFromLocationGroups()).isFalse();
        assertThat(routable.getInterval()).isEqualTo(order.getDelivery().getInterval().toRelativeTimeInterval(offset));

        var geoPointScale = configurationServiceAdapter.getValue(ConfigurationProperties.GEO_POINT_SCALE, Integer.class)
                .orElse(GEO_POINT_SCALE);
        var latitude = order.getDelivery().getDeliveryAddress().getGeoPoint().getLatitude();
        var longitude = order.getDelivery().getDeliveryAddress().getGeoPoint().getLongitude();
        var geoPoint = GeoPoint.ofLatLon(geoPointScale, latitude, longitude);
        var expectedGroupingKey = new RoutableKey.ClientGroupKey(geoPoint, order.getDelivery().getRecipientPhone());

        assertThat(routable.getGroupingKey()).isInstanceOf(RoutableKey.ClientGroupKey.class);
        assertThat(routable.getGroupingKey()).isEqualTo(expectedGroupingKey);

        var tags = routable.getTags();
        assertThat(tags).hasSize(2);
        assertThat(tags).contains(RequiredRoutingTag.DELIVERY.getCode());
        if (order.isPrepaid()) {
            assertThat(tags).contains(RequiredRoutingTag.PREPAID.getCode());
        } else {
            assertThat(tags).contains(RequiredRoutingTag.POSTPAID.getCode());
        }
    }

    private void generateOrders() {

        var wrongDate = shiftDate.plusDays(3);

        for (var statusToDeliver : OrderRepository.STATUSES_TO_DELIVER) {
            // Заказ для доставки
            orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .deliveryDate(shiftDate)
                    .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                    .flowStatus(statusToDeliver)
                    .build()
            );
            // Заказ на другую дату
            orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .deliveryDate(wrongDate)
                    .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                    .flowStatus(statusToDeliver)
                    .build()
            );
        }

        // Заказы в других статусах
        orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate)
                .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .build()
        );
        orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate)
                .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                .flowStatus(OrderFlowStatus.CANCELLED)
                .build()
        );
        orderGenerateService.generateOrderCommand(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate)
                .deliveryServiceId(DeliveryService.DEFAULT_DS_ID)
                .flowStatus(OrderFlowStatus.PACK_RETURN_BOXES_AND_READY_FOR_RETURN)
                .build()
        );
    }

}
