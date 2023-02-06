package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

/**
 * @author kukabara
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderRepositoryTest {

    private final OrderRepository orderRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper userHelper;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final OrderCommandService orderCommandService;
    private final DsRepository dsRepository;

    @MockBean
    private Clock clock;

    private Shift shift;
    private User user;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, LocalDateTime.of(2002, 1, 1, 0, 0, 0));
        user = userHelper.findOrCreateUser(35236L);
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userHelper.sortingCenterWithDs(shift.getId(), DELIVERY_SERVICE_ID);
    }

    @Test
    void save() {
        String externalOrderId = "3513orderID!";
        int itemsCount = 3;
        Long orderId = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(externalOrderId)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsCount(itemsCount)
                                .build())
                        .places(List.of(
                                OrderPlaceDto.builder()
                                        .dimensions(new Dimensions(BigDecimal.ONE, 10, 15, 20))
                                        .build())
                        )
                        .build()).getId();
        assertThat(orderId).isNotNull();
        testDataFactory.flushAndClear();

        Order savedOrder = orderRepository.findById(orderId).orElseThrow();

        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getExternalOrderId()).isEqualTo(externalOrderId);

        assertThat(savedOrder.getItems()).hasSize(itemsCount);
        assertThat(savedOrder.getDelivery().getId()).isNotNull();
        assertEquals(savedOrder.getDimensionsClass(), DimensionsClass.REGULAR_CARGO);

        List<Order> orders = orderRepository.findAllBetweenDatesWithRelatedEntities(
                savedOrder.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID),
                savedOrder.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID),
                Set.of(savedOrder.getDeliveryServiceId()),
                EnumSet.of(savedOrder.getOrderFlowStatus())
        );

        for (Order order : orders) {
            assertThat(order.getExternalOrderId()).isEqualTo(savedOrder.getExternalOrderId());
        }
    }

    @Test
    void findOrdersByDeliveryDate() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        assertThat(order.getId()).isNotNull();
        testDataFactory.flushAndClear();

        LocalDate deliveryDate = order.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID);
        List<Order> ordersForToday = orderRepository.findOrdersToDeliverOnDate(deliveryDate,
                Set.of(DeliveryService.FAKE_DS_ID)
        );

        assertThat(ordersForToday).hasSize(1);
        assertThat(ordersForToday).extracting(Order::getId).containsExactly(order.getId());
    }

    @Test
    void findAllByIdTest() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        assertThat(order.getId()).isNotNull();
        testDataFactory.flushAndClear();

        assertThat(orderRepository.findAllById(List.of(order.getId()))).hasSize(1);
        assertThat(orderRepository.findMapByIds(StreamEx.of(order).map(BaseJpaEntity::getId).toSet())).hasSize(1);
    }

    @Test
    void findExternalIdByIdTest() {
        String externalOrderId = "ex01";
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(externalOrderId)
                .build()
        );
        assertThat(order.getId()).isNotNull();
        testDataFactory.flushAndClear();

        assertEquals(orderRepository.findExternalOrderIdByOrderId(order.getId()), Optional.of(externalOrderId));
    }

    @Test
    void findNotCanceledOrdersByShiftIdTest() {
        //создаём заказы
        Order order1 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());
        Order order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());

        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Крыжополь")
                        .build())
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(OrderFlowStatus.CANCELLED)
                .build());

        assertThat(order1.getId()).isNotNull();
        assertThat(order2.getId()).isNotNull();

        //создаём смены курьеров
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, order1.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        var createCommand2 = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr2", 14, order2.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        UserShift userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        UserShift userShift2 =
                userShiftRepository.findById(commandService.createUserShift(createCommand2)).orElseThrow();

        //два активных заказа сейчас
        List<OrderRepository.OrderUser> orders =
                orderRepository.findExpectedOnScOrdersByShiftId(shift.getId());
        assertThat(orders.size()).isEqualTo(2);

        orderCommandService.forceUpdateFlowStatus(new OrderCommand.UpdateFlowStatus(
                order2.getId(),
                OrderFlowStatus.CANCELLED
        ));

        //один активный заказ после отмены второго
        List<OrderRepository.OrderUser> ordersUpdated =
                orderRepository.findExpectedOnScOrdersByShiftId(shift.getId());
        assertThat(ordersUpdated.size()).isEqualTo(1);

        //создаём задачи на доставку
        when(clock.withZone(userShift.getZoneId())).thenReturn(clock);
        userHelper.checkinAndFinishPickup(userShift);
        testDataFactory.flushAndClear();

        //нет активных после старта доставки
        List<OrderRepository.OrderUser> ordersAfterPickup =
                orderRepository.findExpectedOnScOrdersByShiftId(shift.getId());
        assertThat(ordersAfterPickup.size()).isEqualTo(0);
    }

    @Test
    void findMultiOrdersByOrderIdTest() {
        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        Order multiOrder1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(5000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        Order multiOrder2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .build());


        Order order1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        Order order2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder().build());

        assertThat(multiOrder1.getId()).isNotNull();
        assertThat(multiOrder2.getId()).isNotNull();
        assertThat(order1.getId()).isNotNull();
        assertThat(order2.getId()).isNotNull();
        testDataFactory.flushAndClear();

        User user = userHelper.findOrCreateUser(999777L, LocalDate.now(clock));
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);
        userHelper.checkinAndFinishPickup(userShift);

        assertThat(orderRepository.findAllById(List.of(multiOrder1.getId(), multiOrder2.getId()))).hasSize(2);
        assertThat(orderRepository.findAllOrdersInMultiOrder(multiOrder1.getId())).hasSize(2)
                .extracting("id").containsOnly(multiOrder1.getId(), multiOrder2.getId());
        assertThat(orderRepository.findAllOrdersInMultiOrder(multiOrder2.getId())).hasSize(2)
                .extracting("id").containsOnly(multiOrder1.getId(), multiOrder2.getId());
    }

    @Test
    void findAllOrdersInMultiOrderWithPropertiesTest() {
        User user = userHelper.findOrCreateUser(100L);
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        String verificationCodeProperty = TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING.name();
        String customerTypeProperty = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        Map<String, OrderProperty> propertyMap = Map.of(
                verificationCodeProperty,
                new OrderProperty(null, TplPropertyType.STRING, verificationCodeProperty, "verificationCode"),
                customerTypeProperty,
                new OrderProperty(null, TplPropertyType.STRING, customerTypeProperty,
                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name())
        );
        var order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("1000")
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsCount(2)
                                .build())
                        .places(List.of(
                                OrderPlaceDto.builder()
                                        .dimensions(new Dimensions(BigDecimal.ONE, 10, 15, 20))
                                        .build())
                        )
                        .properties(propertyMap)
                        .build());
        var taskId = userHelper.addDeliveryTaskToShift(user, userShift, order).getId();

        List<Order> orders = orderRepository.findAllOrdersInMultiOrderWithProperties(taskId);

        assertNotNull(orders);
        if (orders.size() > 0) {
            Order orderFromSearch = orders.get(0);
            assertThat(orderFromSearch.getId()).isEqualTo(order.getId());
            assertThat(orderFromSearch.getProperties()).containsKey(TplOrderProperties.VERIFICATION_CODE_BEFORE_HANDING.getName());
            assertThat(orderFromSearch.getProperties()).containsKey(TplOrderProperties.CUSTOMER_TYPE.getName());
        }
    }

    @Test
    void findByExternalOrderIdAndDeliveryServiceIdTest() {
        //given
        Order givenOrder = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("33")
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build()
        );
        assertThat(givenOrder.getId()).isNotNull();
        testDataFactory.flushAndClear();

        //when
        Order foundOrder = orderRepository.findByExternalOrderIdAndDeliveryServiceId("33", DELIVERY_SERVICE_ID).orElse(null);

        //then
        assertThat(foundOrder).isNotNull().isEqualTo(givenOrder);
    }

    @Test
    void findByExternalOrderIdAndSortingCenterIdTest() {
        //given
        Order givenOrder = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("33")
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build()
        );

        DeliveryService givenDeliveryService = dsRepository.findByIdOrThrow(DELIVERY_SERVICE_ID);
        Long scId = givenDeliveryService.getSortingCenter().getId();

        assertThat(givenOrder.getId()).isNotNull();
        testDataFactory.flushAndClear();

        //when
        Order foundOrder = orderRepository.findByExternalOrderIdAndSortingCenterId("33", scId).orElse(null);

        //then
        assertThat(foundOrder).isNotNull().isEqualTo(givenOrder);
    }
}
