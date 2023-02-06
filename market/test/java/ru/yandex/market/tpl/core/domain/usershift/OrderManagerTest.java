package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import yandex.market.combinator.v0.CombinatorGrpc;
import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderAddressDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonTypeDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplOrderValidationException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.AutoRescheduleService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstancePurchaseStatus;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.Address;
import ru.yandex.market.tpl.core.domain.order.address.AddressQueryService;
import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.order.validator.TplOrderRescheduleValidator;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrder;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.holiday.PickupPointHoliday;
import ru.yandex.market.tpl.core.domain.pickup.holiday.PickupPointHolidayRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.TplOrdersRescheduleException;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.service.pickup.PickupHolidayConfigurationAdapter;
import ru.yandex.market.tpl.core.service.reschedule.RescheduleService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CANCEL_ORDER;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.OTHER;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.SUCCESS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING;
import static ru.yandex.market.tpl.core.domain.order.OrderManager.DEFAULT_TIME_TO_RESCHEDULE_AFTER;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

/**
 * @author kukabara
 */
@RequiredArgsConstructor
public class OrderManagerTest extends TplAbstractTest {

    public static final LocalTimeInterval DELIVERY_INTERVAL_19_00_TO_23_59 = LocalTimeInterval.valueOf("19:00-23:59");
    private static final int DAYS_TO_FUTURE_ORDER = 10;
    private static final String ORDER_IN_PVZ_YESTERDAY = "ORDER_IN_PVZ_YESTERDAY";
    private static final String ORDER_IN_PVZ_TODAY = "ORDER_IN_PVZ_TODAY";
    private static final String ORDER_IN_PVZ_TOMORROW = "ORDER_IN_PVZ_TOMORROW";

    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final OrderManager orderManager;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final TplOrderRescheduleValidator spiedRescheduleValidator;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final TransactionTemplate transactionTemplate;
    private final PickupPointHolidayRepository pickupPointHolidayRepository;
    private final PickupPointRepository pickupPointRepository;
    private final OrderCommandService orderCommandService;
    private final AutoRescheduleService autoRescheduleService;
    private final CombinatorGrpc.CombinatorBlockingStub combinatorBlockingStub;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final AddressQueryService addressQueryService;

    private UserShift userShift;
    private UserShift userShift2;
    private UserShift userShift3;
    private PickupPoint pvzPickupPoint;
    private List<Order> orders;
    private Map<String, Order> ordersMap;
    private Long userId1 = 35236L;
    private Long userId2 = 35237L;
    private Long userId3 = 35238L;

    @BeforeEach
    void init() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                partnerRepository.findByIdOrThrow(DELIVERY_SERVICE_ID).getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, false
        );
        ClockUtil.initFixed(clock);
        User user = testUserHelper.findOrCreateUser(userId1);
        User user2 = testUserHelper.findOrCreateUser(userId2);
        User user3 = testUserHelper.findOrCreateUser(userId3);

        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        Shift shift2 = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock).plusDays(DAYS_TO_FUTURE_ORDER));

        testUserHelper.sortingCenterWithDs(shift.getId(), DELIVERY_SERVICE_ID);

        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());
        Order order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock).plusDays(DAYS_TO_FUTURE_ORDER))
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build()
        );
        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);

        Order order3 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                        .pickupPoint(pickupPoint)
                        .build());
        Order order4 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DELIVERY_INTERVAL_19_00_TO_23_59)
                        .deliveryDate(LocalDate.now(clock).minusDays(1))
                        .build()
        );
        Order order5 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .recipientNotes(DO_NOT_CALL_DELIVERY_PREFIX)
                        .build());
        pvzPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 2L, 1L);
        Order orderToPvzYersterdayDelivery = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                        .recipientNotes("pvzPickupPoint")
                        .pickupPoint(pvzPickupPoint)
                        .deliveryDate(LocalDate.now(clock).minusDays(1))
                        .build());
        Order orderToPvzToday = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                        .recipientNotes("pvzPickupPoint")
                        .pickupPoint(pvzPickupPoint)
                        .deliveryDate(LocalDate.now(clock))
                        .build());
        Order orderToPvzTomorrow = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                        .recipientNotes("pvzPickupPoint")
                        .pickupPoint(pvzPickupPoint)
                        .deliveryDate(LocalDate.now(clock).plusDays(1L))
                        .build());

        ordersMap = Map.of(
                ORDER_IN_PVZ_YESTERDAY, orderToPvzYersterdayDelivery,
                ORDER_IN_PVZ_TODAY, orderToPvzToday,
                ORDER_IN_PVZ_TOMORROW, orderToPvzTomorrow
        );
        orders = new ArrayList<>();
        orders.add(order);
        orders.add(order2);
        orders.add(order3);
        orders.add(order4);
        orders.add(order5);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        var createCommand2 = UserShiftCommand.Create.builder()
                .userId(user2.getId())
                .shiftId(shift2.getId())
                .routePoint(helper.taskOrderPickup(clock.instant().plus(Duration.ofDays(DAYS_TO_FUTURE_ORDER))))
                .routePoint(helper.taskPrepaid("addr2", orders.get(1).getId(),
                        DateTimeUtil.atStartOfDay(LocalDate.now(clock).plusDays(DAYS_TO_FUTURE_ORDER)), false))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        var createCommand3 = UserShiftCommand.Create.builder()
                .userId(user3.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, orders.get(4).getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        userShift2 = userShiftRepository.findById(commandService.createUserShift(createCommand2)).orElseThrow();
        userShift3 = userShiftRepository.findById(commandService.createUserShift(createCommand3)).orElseThrow();
        when(clock.withZone(userShift.getZoneId())).thenReturn(clock);
        testUserHelper.checkinAndFinishPickup(userShift);
        testUserHelper.checkinAndFinishPickup(userShift2);
        testUserHelper.checkinAndFinishPickup(userShift3);

        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
        pickupPointHolidayRepository.deleteAll();
        clearAfterTest(pickupPoint);
        clearAfterTest(pvzPickupPoint);
        resetMocks();
    }

    @AfterEach
    void afterEach() {
        resetMocks();
    }

    @DisplayName("Отмена заказа без задания, который не приехал в СЦ")
    @Test
    void shouldCancelOrderWithoutTaskCancel() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                .build());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_CREATED);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order);
            return null;
        });

        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderCheck.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CANCELLED);
    }

    @DisplayName("Отмена заказа без задания")
    @Test
    void shouldCancelOrderWithoutTaskReturn() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .build());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order);
            return null;
        });

        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderCheck.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
    }

    @DisplayName("Отмена заказа с заданием")
    @Test
    void shouldCancelOrderWithTask() {
        Order order = orderRepository.findByIdOrThrow(orders.get(0).getId());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order);
            return null;
        });

        Order orderCheck = orderRepository.findByIdOrThrow(orders.get(0).getId());
        assertThat(orderCheck.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(orderCheck.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);

        OrderDeliveryTask task = transactionTemplate.execute(ts ->
                userShiftRepository.findById(userShift.getId()).orElseThrow()
                        .streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), orderCheck.getId()))
                        .findFirst().orElseThrow()
        );
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
    }

    @DisplayName("Получение всех причин для отмены")
    @Test
    void shouldReturnAllFailReasons() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .build());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())
                .stream().map(OrderDeliveryTaskFailReasonTypeDto::getName)).contains(ORDER_WAS_LOST.name());
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())
                .stream().map(OrderDeliveryTaskFailReasonTypeDto::getName)).contains(ORDER_IS_DAMAGED.name());
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())
                .stream().map(OrderDeliveryTaskFailReasonTypeDto::getName)).contains(OTHER.name());

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order, new OrderDeliveryFailReason(ORDER_WAS_LOST, null, null, Source.OPERATOR));
            return null;
        });


        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())).hasSize(0);
    }

    @DisplayName("Получение причин для отмены после отмены заказа")
    @Test
    void shouldReturnOnlyFailReasonsLost() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .build());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())
                .stream().map(OrderDeliveryTaskFailReasonTypeDto::getName)).contains(ORDER_WAS_LOST.name());
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())
                .stream().map(OrderDeliveryTaskFailReasonTypeDto::getName)).contains(ORDER_IS_DAMAGED.name());
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())
                .stream().map(OrderDeliveryTaskFailReasonTypeDto::getName)).contains(OTHER.name());

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order, new OrderDeliveryFailReason(ORDER_IS_DAMAGED, null, null, Source.OPERATOR));
            return null;
        });


        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId())).hasSize(1);
        assertThat(orderManager.getOrderDeliveryTaskFailReasons(order.getId()).stream()
                .map(OrderDeliveryTaskFailReasonTypeDto::getName))
                .containsOnlyOnce(ORDER_WAS_LOST.name());
    }

    @DisplayName("Отмена потерянного заказа с заданием")
    @Test
    void shouldCancelOrderWithoutTaskReasonOrderWasLost() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .build());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order, new OrderDeliveryFailReason(ORDER_WAS_LOST, "потерался", null,
                    Source.OPERATOR));
            return null;
        });

        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderCheck.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.LOST);
    }

    @DisplayName("Отмена потерянного заказа без заданий")
    @Test
    void shouldCancelOrderWithTaskReasonOrderWasLost() {
        Order order = orderRepository.findByIdOrThrow(orders.get(0).getId());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order, new OrderDeliveryFailReason(ORDER_WAS_LOST, null, null,
                    Source.OPERATOR));
            return null;
        });


        Order orderCheck = orderRepository.findByIdOrThrow(orders.get(0).getId());
        assertThat(orderCheck.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.LOST);
        assertThat(orderCheck.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);

        OrderDeliveryTask task = transactionTemplate.execute(ts ->
                userShiftRepository.findById(userShift.getId()).orElseThrow()
                        .streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), orderCheck.getId()))
                        .findFirst().orElseThrow()
        );
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
    }

    @DisplayName("Валидность доступных дат для переноса заказа")
    @Test
    void getAvailableIntervalForOrder() {
        Map<String, List<String>> map = orderManager.getAvailableReschedulingIntervalsForPartner(orders.get(1).getId());
        assertThat(map.values()).flatExtracting(e -> e)
                .areAtLeast(
                        RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT,
                        new Condition<>("14:00 - 18:00"::equals, "Equals 14 - 18 hour interval")
                );
        assertThat(map.values()).flatExtracting(e -> e)
                .areAtLeast(
                        RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT,
                        new Condition<>("09:00 - 18:00"::equals, "Equals 09:00 - 18:00 hour interval")
                );
        assertThat(map.get(LocalDate.now(clock).plusDays(DAYS_TO_FUTURE_ORDER).toString()))
                .containsOnlyOnce("14:00 - 18:00");
    }

    @DisplayName("Возвращает количество доступных дат для переноса заказа согласно параметру")
    @Test
    void getAvailableDaysForOrderRescheduling() {
        var days = 15;
        configurationServiceAdapter.mergeValue(ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING, days);
        Order order = orders.get(1);

        Map<String, List<String>> map = orderManager.getAvailableReschedulingIntervalsForPartner(order.getId());

        long daysToDeliveryDate = Math.max(
                ChronoUnit.DAYS.between(LocalDate.now(clock), order.getDelivery().getDeliveryDate(clock.getZone())),
                0
        );
        assertThat(map.keySet().size()).isEqualTo(daysToDeliveryDate + days);
    }

    @Disabled //TODO: MARKETTPL-8929
    @DisplayName("Интервалы для самовывоза")
    @Test
    void shouldGetAvailableIntervalForPickupOrder() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.DEFAULT_INTERVAL_LOCKER, "11:00-17:00");

        Map<String, List<String>> map = orderManager.getAvailableReschedulingIntervalsForPartner(orders.get(2).getId());
        when(clock.withZone(userShift.getZoneId())).thenReturn(clock);
        assertThat(map.values()).flatExtracting(e -> e)
                .areAtLeast(
                        RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT,
                        new Condition<>("11:00 - 17:00"::equals, "Not equals pickup interval")
                );
    }

    @DisplayName("Интервалы для ПИ")
    @Test
    void shouldIncludeOrderInterval() {
        Order order = orders.get(1);
        var intervals = orderManager
                .getAvailableReschedulingIntervalsForPartner(order.getId());

        var orderDate = ISO_DATE.format(order.getDelivery().getDeliveryDate(clock.getZone()));
        var orderInterval = new LocalTimeInterval(
                LocalTime.ofInstant(order.getDelivery().getDeliveryIntervalFrom(), clock.getZone()),
                LocalTime.ofInstant(order.getDelivery().getDeliveryIntervalTo(), clock.getZone())
        ).toPrettyDashStringNoSeconds();

        assertThat(intervals).hasSize(
                DAYS_TO_FUTURE_ORDER + RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT
        );
        assertThat(intervals.get(orderDate)).isNotNull().contains(orderInterval);
    }

    @DisplayName("Интервалы для трекинга, нельзя перенести на текущую дату")
    @Test
    void getAvailableIntervalsForTracking_deliveryDateisCurrentDate() {
        //given
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build()
        );

        //when
        var intervals = orderManager
                .getAvailableReschedulingIntervalsForTracking(order);

        //then
        var currentDate = ISO_DATE.format(LocalDate.now(clock));
        assertThat(intervals).hasSize(RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT - 1);
        assertThat(intervals.get(currentDate)).isNull();
    }

    @DisplayName("Интервалы для партнерки, можно перенести на текущую дату")
    @Test
    void getAvailableIntervalsForPartner_deliveryDateisCurrentDate() {
        //given
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build()
        );

        //when
        var intervals = orderManager
                .getAvailableReschedulingIntervalsForPartner(order.getId());

        //then
        var currentDate = ISO_DATE.format(LocalDate.now(clock));
        assertThat(intervals).hasSize(RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT);
        assertThat(intervals.get(currentDate)).isNotNull();
    }

    @DisplayName("Валидность доступных дат для переноса заказа с учётом выходных")
    @Test
    void getAvailablePartnerIntervalForOrderWithHolidays() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        var now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        Map<String, List<String>> resultMap = orderManager.getAvailableReschedulingIntervalsForPartner(
                ordersMap.get(ORDER_IN_PVZ_TOMORROW).getId()
        );

        //Проверяем, что в мапе нет выходных дней на сегодня и завтра
        Set<String> holidaySets = List.of(today, today.plusDays(1)).stream()
                .map(ISO_DATE::format)
                .collect(Collectors.toSet());
        Set<String> resultIntervalDate = resultMap.keySet();
        assertThat(resultIntervalDate.containsAll(holidaySets)).isFalse();
    }

    @DisplayName("Валидность доступных дат для переноса заказа с учётом выходных, но выходных нет")
    @Test
    void getAvailablePartnerIntervalForOrderWithEmptyHolidays() {
        //Добавляем флаг проверки выходных
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        var now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        //Удаляем всю информацию информацию о выходных
        transactionTemplate.execute(t -> {
            pickupPointHolidayRepository.deleteAll();
            return null;
        });

        Map<String, List<String>> resultMap = orderManager.getAvailableReschedulingIntervalsForPartner(
                ordersMap.get(ORDER_IN_PVZ_TODAY).getId()
        );

        //Проверяем, что в мапе нет выходных дней и количество дней равно NUMBER_OF_DAYS_FOR_RESCHEDULING
        assertThat(resultMap.keySet()).hasSize(RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT);
    }

    @DisplayName("Валидность доступных дат для переноса заказа, выходные есть, но не включены")
    @Test
    void getAvailablePartnerIntervalForOrderWithHolidaysButFlagsOff() {
        //Добавляем выключенные флаг проверки выходных
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, false
        );
        var now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        Map<String, List<String>> resultMap = orderManager.getAvailableReschedulingIntervalsForPartner(
                ordersMap.get(ORDER_IN_PVZ_TOMORROW).getId()
        );

        //Проверяем, что в мапе интервалов есть выходные дни, потому что мы их не учитываем - флаги выключены
        Set<String> holidaySets = List.of(today, today.plusDays(1)).stream()
                .map(ISO_DATE::format)
                .collect(Collectors.toSet());
        Set<String> resultIntervalDate = resultMap.keySet();
        assertThat(resultIntervalDate.containsAll(holidaySets)).isTrue();
    }

    @DisplayName("Валидность доступных дат для переноса заказа, из комбинатора")
    @Test
    void getAvailablePartnerIntervalForOrderFromCombinator() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                partnerRepository.findByIdOrThrow(DELIVERY_SERVICE_ID).getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, true
        );
        LocalTimeInterval interval1 = new LocalTimeInterval(LocalTime.of(10, 0), LocalTime.of(19, 0));
        LocalTimeInterval interval2 = new LocalTimeInterval(LocalTime.of(14, 0), LocalTime.of(20, 0));
        LocalDate nowMinusOneDay = LocalDate.now(clock).minusDays(1);
        LocalDate now = LocalDate.now(clock);

        Mockito.when(combinatorBlockingStub.postponeDelivery(any()))
                .thenReturn(CombinatorOuterClass.PostponeDeliveryResponse.newBuilder()
                        .addAllOptions(List.of(
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(nowMinusOneDay.getYear())
                                                .setMonth(nowMinusOneDay.getMonthValue())
                                                .setDay(nowMinusOneDay.getDayOfMonth())
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval1.getStart().getHour())
                                                        .setMinute(interval1.getStart().getMinute())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval1.getEnd().getHour())
                                                        .setMinute(interval1.getEnd().getMinute())
                                                        .build())
                                                .build())
                                        .build(),
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(now.getYear())
                                                .setMonth(now.getMonthValue())
                                                .setDay(now.getDayOfMonth())
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval2.getStart().getHour())
                                                        .setMinute(interval2.getStart().getMinute())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval2.getEnd().getHour())
                                                        .setMinute(interval2.getEnd().getMinute())
                                                        .build())
                                                .build())
                                        .build(),
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(now.getYear())
                                                .setMonth(now.getMonthValue())
                                                .setDay(now.getDayOfMonth())
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval1.getStart().getHour())
                                                        .setMinute(interval1.getStart().getMinute())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval1.getEnd().getHour())
                                                        .setMinute(interval1.getEnd().getMinute())
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .build());

        Map<String, List<String>> resultMap = orderManager.getAvailableReschedulingIntervalsForPartner(
                orders.get(0).getId()
        );

        //Проверяем что все данные нормально обработались и смапились
        assertThat(resultMap.size()).isEqualTo(1);
        assertThat(resultMap.keySet()).containsExactly(
                ISO_DATE.format(now)
        );
        assertThat(resultMap.values())
                .containsExactly(List.of(
                                interval1.toPrettyDashStringNoSeconds(),
                                interval2.toPrettyDashStringNoSeconds()
                        )
                );
    }

    @DisplayName("Валидность доступных дат для переноса заказа, выходные есть, " +
            "потому что интервалы для трекинга не учитываются")
    @Test
    void getAvailableTrackingIntervalForOrderWithHolidaysButFlagsOff() {
        //Добавляем выключенные флаг проверки выходных
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        var now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(2)));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(3)));
            return null;
        });

        Map<String, List<String>> resultMap = orderManager.getAvailableReschedulingIntervalsForTracking(
                ordersMap.get(ORDER_IN_PVZ_TOMORROW)
        );

        //Проверяем, что в мапе интервалов есть выходные дни, потому что мы их не учитываем в трекинге
        //даже когда включены флаги проверки
        Set<String> holidaySets = List.of(today.plusDays(1), today.plusDays(2), today.plusDays(3))
                .stream()
                .map(ISO_DATE::format)
                .collect(Collectors.toSet());
        Set<String> resultIntervalDate = resultMap.keySet();
        assertThat(resultIntervalDate.containsAll(holidaySets)).isTrue();
    }

    @DisplayName("Перенос даты заказа с самовывозом")
    @Test
    void shouldReschedulePickupOrder() {
        //given
        Order order = orders.get(2);

        //reinit time...
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(clock),
                DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1L)));

        var now = LocalDateTime.now(clock);

        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(order.getDeliveryServiceId());

        //when
        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        //then
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderCheck.getDelivery().getInterval())
                .isEqualTo(DateTimeUtil.PICKUP_INTERVAL.toInterval(tomorrow, DateTimeUtil.DEFAULT_ZONE_ID));
    }


    @DisplayName("Перенос даты заказа с ошибкой")
    @Test
    void shouldReschedulePickupOrder_withException() {
        //given
        Order order = orders.get(2);

        //reinit time...
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(clock),
                DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1L)));

        var now = LocalDateTime.now(clock);


        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(order.getDeliveryServiceId());

        //На перенос два заказа - перенос одного из упадет с ошибкой
        doThrow(new TplOrderValidationException("Error")).when(spiedRescheduleValidator).validate(eq(orders.get(3)),
                any());

        //when
        assertThrows(TplOrdersRescheduleException.class,
                () -> autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService));


        //then
        LocalDate tomorrow = now.toLocalDate().plusDays(1);

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getDelivery().getInterval())
                .isEqualTo(DateTimeUtil.PICKUP_INTERVAL.toInterval(tomorrow, DateTimeUtil.DEFAULT_ZONE_ID));
    }

    @DisplayName("Перенос заказа после 00:00")
    @Test
    void shouldRescheduleOrderAfterMidnight() {
        Order order = orders.get(3);
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        LocalTime.of(0, 30)
                )
        );
        var now = LocalDateTime.now(clock).plusHours(1L);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(order.getDeliveryServiceId());

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getDelivery().getInterval())
                .isEqualTo(DELIVERY_INTERVAL_19_00_TO_23_59.toInterval(now.toLocalDate(),
                        DateTimeUtil.DEFAULT_ZONE_ID));
    }

    @DisplayName("Перенос заказа с учётом выходного дня точки")
    @Test
    void shouldRescheduleOrderWithHoliday() {
        Order orderYesterday = ordersMap.get(ORDER_IN_PVZ_YESTERDAY);
        Order orderToday = ordersMap.get(ORDER_IN_PVZ_TODAY);
        Order orderTomorrow = ordersMap.get(ORDER_IN_PVZ_TOMORROW);
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)
                )
        );
        var now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(orderYesterday.getDeliveryServiceId());
        //Добавляем флаг проверки выходного при переносе
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        orderYesterday = orderRepository.findByIdOrThrow(orderYesterday.getId());
        orderToday = orderRepository.findByIdOrThrow(orderToday.getId());
        orderTomorrow = orderRepository.findByIdOrThrow(orderTomorrow.getId());
        LocalDate expectedLocaleDate = now.plusDays(2).toLocalDate();
        assertRescheduleLocaleDate(orderYesterday, expectedLocaleDate);
        assertRescheduleLocaleDate(orderToday, expectedLocaleDate);
        assertRescheduleLocaleDate(orderTomorrow, expectedLocaleDate);
    }

    @DisplayName("Перенос заказа с учётом выходного дня точки и настройки пропускать некоторые ПВЗ")
    @Test
    void shouldRescheduleOrderWithHolidaySkipPartner_checkSettings() {
        Order orderYesterday = ordersMap.get(ORDER_IN_PVZ_YESTERDAY);
        Order orderToday = ordersMap.get(ORDER_IN_PVZ_TODAY);
        Order orderTomorrow = ordersMap.get(ORDER_IN_PVZ_TOMORROW);
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)
                )
        );
        var now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(orderYesterday.getDeliveryServiceId());
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PARTNER_IDS_SKIP_CHECK_HOLIDAY_TO_RESCHEDULE,
                pvzPickupPoint.getPartnerId()
        );
        //Добавляем флаг проверки выходного при переносе
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PARTNER_IDS_SKIP_CHECK_HOLIDAY_LOGIC_IS_ENABLED, true
        );
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        configurationServiceAdapter.deleteValue(ConfigurationProperties.PARTNER_IDS_SKIP_CHECK_HOLIDAY_TO_RESCHEDULE);
        configurationServiceAdapter.deleteValue(ConfigurationProperties.PARTNER_IDS_SKIP_CHECK_HOLIDAY_LOGIC_IS_ENABLED);

        orderYesterday = orderRepository.findByIdOrThrow(orderYesterday.getId());
        orderToday = orderRepository.findByIdOrThrow(orderToday.getId());
        orderTomorrow = orderRepository.findByIdOrThrow(orderTomorrow.getId());

        assertRescheduleLocaleDate(orderYesterday, now.toLocalDate());
        assertRescheduleLocaleDate(orderToday, now.plusDays(1).toLocalDate());
        assertRescheduleLocaleDate(orderTomorrow, now.plusDays(1).toLocalDate());
    }

    @DisplayName("Перенос заказа в режиме с учетов выходных дней для игнорируемых партнеров")
    @Test
    void shouldRescheduleOrderWithHolidaySkipPartner_checkDefaultValue() {

        //Пункт ПВЗ от партнера, которые не поддерживает API выходных дней
        var pvzPickupPointSkipHolidaysCheck = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 3L,
                PickupHolidayConfigurationAdapter.DEFAULT_SKIP_PARTNER_ID);
        clearAfterTest(pvzPickupPointSkipHolidaysCheck);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                        .recipientNotes("pvzPickupPointSkipHolidaysCheck")
                        .pickupPoint(pvzPickupPointSkipHolidaysCheck)
                        .deliveryDate(LocalDate.now(clock))
                        .build());

        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)
                )
        );
        var now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(order.getDeliveryServiceId());
        //Добавляем флаг проверки выходного при переносе
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PARTNER_IDS_SKIP_CHECK_HOLIDAY_LOGIC_IS_ENABLED, true
        );
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPointSkipHolidaysCheck.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);
        configurationServiceAdapter.deleteValue(ConfigurationProperties.PARTNER_IDS_SKIP_CHECK_HOLIDAY_LOGIC_IS_ENABLED);

        order = orderRepository.findByIdOrThrow(order.getId());
        LocalDate expectedLocaleDateSkipHolidaysCheck = now.plusDays(1).toLocalDate();
        assertRescheduleLocaleDate(order, expectedLocaleDateSkipHolidaysCheck);
    }

    @DisplayName("Перенос заказа с учётом выходного дня точки, " +
            "но глобальный флаг выключен и не наступило время переноса на завтра")
    @Test
    void shouldRescheduleOrderWithHolidayButFlagOff() {
        Order orderToday = ordersMap.get(ORDER_IN_PVZ_TODAY);
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.minusHours(1)
                )
        );
        var now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(orderToday.getDeliveryServiceId());
        //Добавляем флаг проверки выходного при переносе заказа в пвз, но не глобальный
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        orderToday = orderRepository.findByIdOrThrow(orderToday.getId());
        assertRescheduleLocaleDate(orderToday, today);
    }

    @DisplayName("Перенос b2b заказа с учётом выходного дня")
    @Test
    void shouldRescheduleB2bOrderWhenTomorrowIsSaturday() {
        //Находим ближайшую пятницу и выставляем ее в качестве текущего дня
        var now = LocalDateTime.now(clock);
        while (!now.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            now =  now.plusDays(1);
        }
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        now.toLocalDate(),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.minusHours(1)
                )
        );
        Order b2bOrder = createB2bOrder(false);
        //Переводим время на вечер, когда переносить заказы
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        now.toLocalDate(),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)
                )
        );
        now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(b2bOrder.getDeliveryServiceId());

        LocalDate monday = now.toLocalDate().plusDays(3);
        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        b2bOrder = orderRepository.findByIdOrThrow(b2bOrder.getId());
        assertRescheduleLocaleDate(b2bOrder, monday);
    }

    @DisplayName("Перенос b2b заказа в ПВЗ на следующий день субботу")
    @Test
    void shouldRescheduleB2bPvzOrderOnSaturdayWhenTodaySaturday() {
        //Находим ближайшую пятницу и выставляем ее в качестве текущего дня
        var now = LocalDateTime.now(clock);
        while (!now.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            now =  now.plusDays(1);
        }
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        now.toLocalDate(),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.minusHours(1)
                )
        );
        Order b2bOrder = createB2bOrder(true);
        //Переводим время на вечер, когда переносить заказы
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        now.toLocalDate(),
                        DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)
                )
        );
        now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(b2bOrder.getDeliveryServiceId());

        LocalDate monday = now.toLocalDate().plusDays(1);
        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        b2bOrder = orderRepository.findByIdOrThrow(b2bOrder.getId());
        assertRescheduleLocaleDate(b2bOrder, monday);
    }

    @DisplayName("Перенос заказа с учётом выходного дня точки, после 00")
    @Test
    void shouldRescheduleOrderWithHolidayAfterMidnight() {
        Order orderYesterday = ordersMap.get(ORDER_IN_PVZ_YESTERDAY);
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        LocalTime.of(0, 30)
                )
        );
        var now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(orderYesterday.getDeliveryServiceId());
        //Добавляем флаг проверки выходного при переносе
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.PVZ_CHECK_HOLIDAY_TO_RESCHEDULE, true
        );
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            return null;
        });

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        orderYesterday = orderRepository.findByIdOrThrow(orderYesterday.getId());
        LocalDate expectedLocaleDate = now.plusDays(1).toLocalDate();
        assertRescheduleLocaleDate(orderYesterday, expectedLocaleDate);
    }

    @DisplayName("Перенос заказа без учётом выходного дня точки")
    @Test
    void shouldRescheduleOrderWithoutHoliday() {
        Order orderTomorrow = ordersMap.get(ORDER_IN_PVZ_TOMORROW);
        ClockUtil.initFixed(
                clock,
                LocalDateTime.of(
                        LocalDate.now(clock),
                        LocalTime.of(0, 30)
                )
        );
        var now = LocalDateTime.now(clock);
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(orderTomorrow.getDeliveryServiceId());
        LocalDate today = now.toLocalDate();
        //Добавляем информацию о выходных
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = pickupPointRepository.findById(pvzPickupPoint.getId()).get();
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today));
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, today.plusDays(1)));
            return null;
        });

        autoRescheduleService.rescheduleOrdersByDeliveryService(deliveryService);

        orderTomorrow = orderRepository.findByIdOrThrow(orderTomorrow.getId());
        LocalDate expectedLocaleDate = now.plusDays(1).toLocalDate();
        assertRescheduleLocaleDate(orderTomorrow, expectedLocaleDate);
    }

    private void assertRescheduleLocaleDate(Order order, LocalDate expectedLocaleDate) {
        Instant instantStartDay = order.getDelivery().getInterval().getStart();
        assertThat(LocalDate.ofInstant(instantStartDay, DateTimeUtil.DEFAULT_ZONE_ID))
                .isEqualTo(expectedLocaleDate);
    }

    @DisplayName("Обновление заказа без изменения интервала")
    @Test
    void shouldNotRescheduledWhenIntervalNotChanged() {
        //given
        Order order = orders.get(0);
        //
        LocalDate deliveryDate = order.getDelivery().getDeliveryDateAtDefaultTimeZone();
        LocalTime intervalFrom = LocalTime.ofInstant(order.getDelivery().getInterval().getStart(), clock.getZone());
        LocalTime intervalTo = LocalTime.ofInstant(order.getDelivery().getInterval().getEnd(), clock.getZone());

        PartnerkaCommand.UpdateOrder updateOrder = PartnerkaCommand.UpdateOrder.builder()
                .deliveryDate(deliveryDate)
                .intervalFrom(intervalFrom)
                .intervalTo(intervalTo)
                .externalOrderId(order.getExternalOrderId())
                .build();
        //when
        orderManager.updateOrderData(updateOrder, Source.SYSTEM,
                Map.of(deliveryDate, List.of(new LocalTimeInterval(intervalFrom, intervalTo))));

        //then
        verify(spiedRescheduleValidator, never()).validate(any(), any());
    }

    @DisplayName("Перенос даты заказа с самовывозом и отмена задания")
    @Test
    void shouldReschedulePickupOrderWithTask() {
        Order order = orders.get(2);
        transactionTemplate.execute(ts -> {
            userShiftReassignManager.assign(userShiftRepository.findById(userShift.getId()).orElseThrow(), order);
            return null;
        });

        var now = LocalDateTime.now(clock);
        LocalDate tomorrow = now.toLocalDate().plusDays(1);

        Interval updatedInterval = DateTimeUtil.PICKUP_INTERVAL.toInterval(tomorrow, DateTimeUtil.DEFAULT_ZONE_ID);

        //when
        transactionTemplate.execute(ts -> {
            orderManager.rescheduleOrder(order, updatedInterval, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                    Source.OPERATOR);
            return null;
        });


        //then
        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderCheck.getDelivery().getInterval())
                .isEqualTo(DateTimeUtil.PICKUP_INTERVAL.toInterval(tomorrow, DateTimeUtil.DEFAULT_ZONE_ID));
    }

    @DisplayName("Перенос даты заказа с самовывозом после отмены задания курьером")
    @Test
    void shouldReschedulePickupOrderAfterCourierFailsTask() {
        Order order = orders.get(2);
        transactionTemplate.execute(ts -> {
            userShiftReassignManager.assign(userShiftRepository.findById(userShift.getId()).orElseThrow(), order);
            return null;
        });


        LockerDeliveryTask lockerDeliveryTask = transactionTemplate.execute(ts ->
                userShiftRepository.findById(userShift.getId()).orElseThrow()
                        .streamDeliveryTasks()
                        .select(LockerDeliveryTask.class)
                        .findFirst()
                        .orElseThrow()
        );

        var now = LocalDateTime.now(clock);
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        UserShiftCommand.FailOrderDeliveryTask command = new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(),
                lockerDeliveryTask.getRoutePoint().getId(),
                lockerDeliveryTask.getId(),
                new OrderDeliveryFailReason(LOCKER_NOT_WORKING, "Beep boop")
        );

        commandService.failDeliveryTask(userShift.getUser(), command);
        transactionTemplate.execute(ts -> {
            UserShift userShift = userShiftRepository.findById(this.userShift.getId()).orElseThrow();
            testUserHelper.finishAllDeliveryTasks(userShift);
            testUserHelper.finishFullReturnAtEnd(userShift);
            testUserHelper.finishUserShift(userShift);
            return null;
        });

        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_BY_TASK, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_BY_TASK);

        Order orderCheck = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderCheck.getDeliveryStatus())
                .isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(orderCheck.getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(orderCheck.getDelivery().getInterval())
                .isEqualTo(DateTimeUtil.PICKUP_INTERVAL.toInterval(tomorrow, DateTimeUtil.DEFAULT_ZONE_ID));
    }

    @DisplayName("Изменение чекпоинта на 60 [READY_FOR_RETURN] после отмены заказа с включенным флагом")
    @Test
    void testOrderFlowStatusAfterCancelOrderWithFlagEnabled() {
        OrderFlowStatus expected = OrderFlowStatus.READY_FOR_RETURN;
        Order order = orders.get(0);
        Long orderId = order.getId();
        User user = testUserHelper.findOrCreateUser(35236L);
        OrderDeliveryTask task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));

        Order order1 = orderRepository.findByIdOrThrow(orderId);
        assertThat(order1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order,
                    new OrderDeliveryFailReason(OTHER, "comment", null, Source.COURIER));
            return null;
        });

        Order order2 = orderRepository.findByIdOrThrow(orderId);
        task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));
        assertThat(order2.getOrderFlowStatus()).isEqualTo(expected);
        assertThat(order2.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), task.getRoutePoint().getId(), task.getId(), Source.COURIER
        ));

        Order order3 = orderRepository.findByIdOrThrow(orderId);
        task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(order3.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order, new OrderDeliveryFailReason(CANCEL_ORDER, "comment", null, Source.CLIENT));
            return null;
        });

        Order order4 = orderRepository.findByIdOrThrow(orderId);
        task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));
        assertThat(order4.getOrderFlowStatus()).isEqualTo(expected);
        assertThat(order4.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
    }

    @DisplayName("Проверка статуса задачи на звонок после восстановления")
    @Test
    void testCallTaskStatusAfterReopen() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        OrderFlowStatus expected = OrderFlowStatus.READY_FOR_RETURN;
        Order order = orders.get(4);
        Long orderId = order.getId();
        User user = testUserHelper.findOrCreateUser(userId3);
        OrderDeliveryTask task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));

        Order order1 = orderRepository.findByIdOrThrow(orderId);
        assertThat(order1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(SUCCESS);

        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order,
                    new OrderDeliveryFailReason(OTHER, "comment", null, Source.COURIER));
            return null;
        });

        Order order2 = orderRepository.findByIdOrThrow(orderId);
        task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));
        assertThat(order2.getOrderFlowStatus()).isEqualTo(expected);
        assertThat(order2.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(SUCCESS);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift3.getId(), task.getRoutePoint().getId(), task.getId(), Source.COURIER
        ));

        task = transactionTemplate.execute(ts -> userShiftRepository.findTasksByOrderId(orderId)
                .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class)));

        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(CLIENT_ASK_NOT_TO_CALL);
    }

    @DisplayName("При отмене заказа всем товарам в заказе проставлять Возвращается")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testItemInstancesAreUpdatedOnOrderCancelAndBackToPurchased(boolean fashion) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(fashion)
                                        .build()
                        )
                        .build()
        );

        order.getItems().stream()
                .map(OrderItem::getInstances)
                .flatMap(Collection::stream)
                .forEach(instance -> assertThat(instance.getPurchaseStatus()).isEqualTo(OrderItemInstancePurchaseStatus.PURCHASED));


        transactionTemplate.execute(ts -> {
            orderManager.cancelOrder(order,
                    new OrderDeliveryFailReason(OTHER, "comment", null, Source.SORT_CENTER));
            var cancelledOrder = orderRepository.findByIdOrThrow(order.getId());
            cancelledOrder.getItems().stream()
                    .map(OrderItem::getInstances)
                    .flatMap(Collection::stream)
                    .forEach(instance -> assertThat(instance.getPurchaseStatus()).isEqualTo(OrderItemInstancePurchaseStatus.RETURNED));
            return null;
        });

        Optional<PartialReturnOrder> partialReturnOrder = partialReturnOrderRepository.findByOrderId(order.getId());
        assertThat(partialReturnOrder).isEmpty();

        transactionTemplate.execute(ts -> {
            orderCommandService.revertCourierCancel(order.getId());
            var cancelledOrder = orderRepository.findByIdOrThrow(order.getId());
            cancelledOrder.getItems().stream()
                    .map(OrderItem::getInstances)
                    .flatMap(Collection::stream)
                    .forEach(instance -> assertThat(instance.getPurchaseStatus()).isEqualTo(OrderItemInstancePurchaseStatus.PURCHASED));
            return null;
        });
    }

    @DisplayName("При отмене заказа всем товарам в заказе проставлять Возвращается с нулловыми уитами")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testItemInstancesAreUpdatedOnOrderCancelWithNullUitsAndBackToPurchased(boolean fashion) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(fashion)
                                        .build()
                        )
                        .build()
        );

        order.getItems().stream()
                .map(OrderItem::getInstances)
                .flatMap(Collection::stream)
                .forEach(instance -> {
                    assertThat(instance.getPurchaseStatus()).isEqualTo(OrderItemInstancePurchaseStatus.PURCHASED);
                    instance.setUit(null);
                });

        transactionTemplate.execute(ts -> {
            var updatedOrder = orderRepository.save(order);
            orderManager.cancelOrder(updatedOrder,
                    new OrderDeliveryFailReason(OTHER, "comment", null, Source.SORT_CENTER));
            var cancelledOrder = orderRepository.findByIdOrThrow(updatedOrder.getId());
            cancelledOrder.getItems().stream()
                    .map(OrderItem::getInstances)
                    .flatMap(Collection::stream)
                    .forEach(instance -> {
                        assertThat(instance.getPurchaseStatus()).isEqualTo(OrderItemInstancePurchaseStatus.RETURNED);
                        assertThat(instance.getUit()).isNull();
                    });
            return null;
        });
        Optional<PartialReturnOrder> partialReturnOrder = partialReturnOrderRepository.findByOrderId(order.getId());
        assertThat(partialReturnOrder).isEmpty();

        transactionTemplate.execute(ts -> {
            orderCommandService.revertCourierCancel(order.getId());
            var cancelledOrder = orderRepository.findByIdOrThrow(order.getId());
            cancelledOrder.getItems().stream()
                    .map(OrderItem::getInstances)
                    .flatMap(Collection::stream)
                    .forEach(instance -> {
                        assertThat(instance.getPurchaseStatus()).isEqualTo(OrderItemInstancePurchaseStatus.PURCHASED);
                        assertThat(instance.getUit()).isNull();
                    });
            return null;
        });
    }

    @DisplayName("Обновление адреса отправляет событие в LES")
    @Test
    void shouldSendEventToLesWhenAddressUpdate() {
        //given
        Order order = orders.get(0);

        LocalDate deliveryDate = order.getDelivery().getDeliveryDateAtDefaultTimeZone();
        LocalTime intervalFrom = LocalTime.ofInstant(order.getDelivery().getInterval().getStart(), clock.getZone());
        LocalTime intervalTo = LocalTime.ofInstant(order.getDelivery().getInterval().getEnd(), clock.getZone());

        PartnerkaCommand.UpdateOrder updateOrder = PartnerkaCommand.UpdateOrder.builder()
                .deliveryDate(deliveryDate)
                .intervalFrom(intervalFrom)
                .intervalTo(intervalTo)
                .address(PartnerOrderAddressDto.builder()
                        .city("city")
                        .street("street")
                        .house("house")
                        .build())
                .externalOrderId(order.getExternalOrderId())
                .build();
        var newAddress = new Address(
                AddressString.builder()
                        .city("city")
                        .street("street")
                        .house("house")
                        .build(),
                order.getDelivery().getDeliveryAddress().getGeoPoint(),
                order.getDelivery().getDeliveryAddress().getRegionId()
        );
        doReturn(Optional.of(newAddress)).when(addressQueryService).queryByAddressString(any());

        //when
        orderManager.updateOrderData(updateOrder, Source.OPERATOR,
                Map.of(deliveryDate, List.of(new LocalTimeInterval(intervalFrom, intervalTo))));

        //then
        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    private Order createB2bOrder(boolean isPickup) {
        String customerTypeProperty = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .pickupPoint(isPickup ? pvzPickupPoint : null)
                        .properties(Map.of(
                                customerTypeProperty,
                                new OrderProperty(null, TplPropertyType.STRING, customerTypeProperty,
                                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name())
                                )
                        )
                        .deliveryDate(LocalDate.now(clock))
                        .build()
        );
    }

    private void resetMocks() {
        reset(spiedRescheduleValidator);
    }
}
