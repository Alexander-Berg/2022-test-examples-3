package ru.yandex.market.tpl.core.query.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import yandex.market.combinator.v0.CombinatorGrpc;
import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.RescheduleDatesDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftPayStatisticsDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstancePurchaseStatus;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceItemDto;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.reschedule.RescheduleService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.OrderGenerateParam;

/**
 * @author valter
 */
@RequiredArgsConstructor
class UserShiftQueryServiceTest extends TplAbstractTest {

    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandDataHelper helper;
    private final Clock clock;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterRepository sortingCenterRepository;
    private final CombinatorGrpc.CombinatorBlockingStub combinatorBlockingStub;

    private final TransactionTemplate transactionTemplate;

    private User user1;
    private User user2;
    private UserShift userShift1;
    private UserShift userShift2;
    private LocalTime loadingStartTime;

    @BeforeEach
    void init() {
        user1 = userHelper.findOrCreateUser(1L);
        user2 = userHelper.findOrCreateUser(2L);

        OrderPlaceDto orderPlaceDto1 = OrderPlaceDto.builder()
                .yandexId("124632914")
                .items(List.of(OrderPlaceItemDto.builder()
                        .count(1)
                        .build()))
                .build();
        OrderPlaceDto orderPlaceDto2 = OrderPlaceDto.builder()
                .yandexId("124632915")
                .items(List.of(OrderPlaceItemDto.builder()
                        .count(1)
                        .build()))
                .build();
        Order orderB2c = orderGenerateService.createOrder(OrderGenerateParam.builder()
                .places(List.of(orderPlaceDto1, orderPlaceDto2))
                .items(OrderGenerateParam.Items.builder()
                        .itemsItemCount(1)
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(300))
                        .build())
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CASH)
                .build());

        String customerTypeProperty = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        Order orderB2b = orderGenerateService.createOrder(OrderGenerateParam.builder()
                .places(List.of(orderPlaceDto1, orderPlaceDto2))
                .items(OrderGenerateParam.Items.builder()
                        .itemsItemCount(1)
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(300))
                        .build())
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CASH)
                .properties(Map.of(
                        customerTypeProperty,
                        new OrderProperty(null, TplPropertyType.STRING, customerTypeProperty,
                                TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name()))
                )
                .build());

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, false);

        loadingStartTime = LocalTime.now(clock).plusMinutes(10);
        UserScheduleData scheduleData = new UserScheduleData(
                CourierVehicleType.CAR,
                new RelativeTimeInterval(LocalTime.now(clock), LocalTime.now(clock).plusHours(2))
        );
        scheduleData.setLoadingStartTime(loadingStartTime);

        var user1CreateCommand = UserShiftCommand.Create.builder()
                .userId(user1.getId())
                .shiftId(shift.getId())
                .scheduleData(scheduleData)
                .routePoint(helper.taskUnpaid("addr1", 12, orderB2c.getId()))
                .active(true)
                .build();

        var user2CreateCommand = UserShiftCommand.Create.builder()
                .userId(user2.getId())
                .shiftId(shift.getId())
                .scheduleData(scheduleData)
                .routePoint(helper.taskUnpaid("addr1", 12, orderB2b.getId()))
                .active(true)
                .build();

        userShift1 = userShiftRepository.findByIdOrThrow(userShiftCommandService.createUserShift(user1CreateCommand));
        userShift2 = userShiftRepository.findByIdOrThrow(userShiftCommandService.createUserShift(user2CreateCommand));
    }

    @Test
    void getRoutePointInfoByTaskId() {
        UserShift userShift = userShiftRepository.findByIdWithRoutePoints(this.userShift1.getId()).orElseThrow();
        var routePointId = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow().getId();
        var taskId = userShift.streamDeliveryTasks().findFirst().orElseThrow().getId();
        assertThat(userShiftQueryService.getRoutePointId(user1, taskId)).isEqualTo(routePointId);
    }

    /**
     * Проверяем, что в информации о заказе не дублируются позиции заказа
     */
    @Test
    void getDeliveryTaskInfo() {
        UserShift userShift = userShiftRepository.findByIdWithRoutePoints(this.userShift1.getId()).orElseThrow();
        userShift.streamOrderDeliveryTasks().findFirst(task -> {
            OrderDeliveryTaskDto orderDeliveryTaskDto =
                    userShiftQueryService.getDeliveryTaskInfo(user1, task.getRoutePoint().getId(), task.getId());
            assertThat(orderDeliveryTaskDto.getOrder().getItems().size()).isEqualTo(4);
            assertThat(orderDeliveryTaskDto.getOrder().getPlaces().size()).isEqualTo(2);
            assertThat(orderDeliveryTaskDto.getOrder().getOrderPrice().doubleValue()).isEqualTo(1200.);
            return true;
        }).orElseThrow();
    }

    @Test
    void getRoutePointsSummariesReturnsLoadingStartTime() {
        RoutePointListDto result = userShiftQueryService.getRoutePointsSummaries(user1);

        assertThat(result).isNotNull();
        assertThat(result.getLoadingStartTime()).isEqualTo(loadingStartTime);
    }

    @Test
    void getRescheduleIntervalsReturnAllIntervals() {
        doReturn(Boolean.TRUE)
                .when(configurationProviderAdapter).isBooleanEnabled(ConfigurationProperties
                        .RESCHEDULE_TO_SAME_DAY_ENABLED);

        RescheduleDatesDto rescheduleIntervals = userShiftQueryService.getRescheduleIntervals(user1, userShift1.getId());

        assertThat(rescheduleIntervals.getDays())
                .hasSize(RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT);
    }

    @Test
    void getRescheduleIntervalsNotReturnTodayIntervals() {
        doReturn(Boolean.FALSE)
                .when(configurationProviderAdapter).isBooleanEnabled(ConfigurationProperties
                        .RESCHEDULE_TO_SAME_DAY_ENABLED);

        RescheduleDatesDto rescheduleIntervals = userShiftQueryService.getRescheduleIntervals(user1, userShift1.getId());

        assertThat(rescheduleIntervals.getDays())
                .hasSize(RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT - 1);

        assertThat(rescheduleIntervals.getDays())
                .extracting(RescheduleDatesDto.Day::getDate)
                .doesNotContain(ISO_DATE.format(userShift1.getShift().getShiftDate()));
    }

    @Test
    void getRescheduleIntervalFromCombinator() {
        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(userShift1.getSortingCenterId());
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter,
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, true);
        RescheduleDatesDto.HourInterval hourInterval1 = new RescheduleDatesDto.HourInterval(10, 0, 19, 0);
        RescheduleDatesDto.HourInterval hourInterval2 = new RescheduleDatesDto.HourInterval(14, 0, 20, 0);


        Mockito.when(combinatorBlockingStub.postponeDelivery(any()))
                .thenReturn(CombinatorOuterClass.PostponeDeliveryResponse.newBuilder()
                        .addAllOptions(List.of(
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(2022)
                                                .setMonth(3)
                                                .setDay(24)
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(hourInterval1.getFrom())
                                                        .setMinute(hourInterval1.getMinutesFrom())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(hourInterval1.getTo())
                                                        .setMinute(hourInterval1.getMinutesTo())
                                                        .build())
                                                .build())
                                        .build(),
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(2022)
                                                .setMonth(3)
                                                .setDay(22)
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(hourInterval2.getFrom())
                                                        .setMinute(hourInterval2.getMinutesFrom())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(hourInterval2.getTo())
                                                        .setMinute(hourInterval2.getMinutesTo())
                                                        .build())
                                                .build())
                                        .build(),
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(2022)
                                                .setMonth(3)
                                                .setDay(22)
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(hourInterval1.getFrom())
                                                        .setMinute(hourInterval1.getMinutesFrom())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(hourInterval1.getTo())
                                                        .setMinute(hourInterval1.getMinutesTo())
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .build());

        Long callTaskId = transactionTemplate.execute(a -> {
            UserShift us = userShiftRepository.findByIdWithRoutePoints(this.userShift1.getId()).orElseThrow();
            return us.getCallToRecipientTasks().stream().findFirst().orElseThrow().getId();
        });

        RescheduleDatesDto rescheduleIntervals = userShiftQueryService
                .getRescheduleIntervalsForMultiOrder(user1, userShift1.getId(), callTaskId);

        assertThat(rescheduleIntervals.getDays().size()).isEqualTo(2);
        assertThat(rescheduleIntervals.getDays()).extracting(RescheduleDatesDto.Day::getDate).containsExactlyInAnyOrder(
                "2022-03-22", "2022-03-24"
        );
        assertThat(rescheduleIntervals.getDays()).flatExtracting(RescheduleDatesDto.Day::getIntervals)
                .containsExactlyInAnyOrder(
                        hourInterval1, hourInterval1, hourInterval2
                );
    }

    @Test
    void getRescheduleIntervalsNotReturnWeekendsIntervalsForB2bCustomerOrder() {
        doReturn(Boolean.FALSE)
                .when(configurationProviderAdapter).isBooleanEnabled(ConfigurationProperties
                .RESCHEDULE_TO_SAME_DAY_ENABLED);

        Long callTaskId = transactionTemplate.execute(a -> {
            UserShift us = userShiftRepository.findByIdWithRoutePoints(this.userShift2.getId()).orElseThrow();
            return us.getCallToRecipientTasks().stream().findFirst().orElseThrow().getId();
        });

        RescheduleDatesDto rescheduleIntervals = userShiftQueryService
                .getRescheduleIntervalsForMultiOrder(user2, userShift2.getId(), callTaskId);
        LocalDate shiftDate = userShift2.getShift().getShiftDate();
        List<String> weekendDays = IntStream.range(1, 7)
                .mapToObj(shiftDate::plusDays)
                .sorted()
                .filter(d ->
                        d.getDayOfWeek().equals(DayOfWeek.SATURDAY) ||
                        d.getDayOfWeek().equals(DayOfWeek.SUNDAY)
                )
                .map(ISO_DATE::format)
                .collect(Collectors.toList());

        assertThat(rescheduleIntervals.getDays())
                .extracting(RescheduleDatesDto.Day::getDate)
                .doesNotContainAnyElementsOf(weekendDays);
    }

    @Test
    void getRescheduleIntervalsReturnWeekendsIntervalsForB2cCustomerOrder() {
        doReturn(Boolean.FALSE)
                .when(configurationProviderAdapter).isBooleanEnabled(ConfigurationProperties
                .RESCHEDULE_TO_SAME_DAY_ENABLED);

        Long callTaskId = transactionTemplate.execute(a -> {
            UserShift us = userShiftRepository.findByIdWithRoutePoints(this.userShift1.getId()).orElseThrow();
            return us.getCallToRecipientTasks().stream().findFirst().orElseThrow().getId();
        });

        RescheduleDatesDto rescheduleIntervals = userShiftQueryService
                .getRescheduleIntervalsForMultiOrder(user1, userShift1.getId(), callTaskId);
        LocalDate shiftDate = userShift1.getShift().getShiftDate();
        List<String> weekendDays = IntStream.range(1, RescheduleService.ORDER_NUMBER_OF_DAYS_FOR_RESCHEDULING_DEFAULT)
                .mapToObj(shiftDate::plusDays)
                .sorted()
                .filter(d ->
                        d.getDayOfWeek().equals(DayOfWeek.SATURDAY) ||
                                d.getDayOfWeek().equals(DayOfWeek.SUNDAY)
                )
                .map(ISO_DATE::format)
                .collect(Collectors.toList());

        assertThat(rescheduleIntervals.getDays())
                .extracting(RescheduleDatesDto.Day::getDate)
                .containsAll(weekendDays);
    }


    @Test
    void testUserShiftPayStatistics_withoutReturns() {
        //given
        List<Order> orders = List.of(
                generateOrder(OrderPaymentType.PREPAID),
                generateOrder(OrderPaymentType.PREPAID),
                generateOrder(OrderPaymentType.CARD),
                generateOrder(OrderPaymentType.CARD),
                generateOrder(OrderPaymentType.CASH),
                generateOrder(OrderPaymentType.CASH));

        UserShift openedShift = prepareFinishedUserShiftWithOrders(orders);


        //when
        UserShiftPayStatisticsDto userShiftPayStatistics =
                userShiftQueryService.getUserShiftPayStatistics(openedShift.getId());

        //then
        Arrays.stream(OrderPaymentType.values())
                .forEach(
                        paymentType -> {
                            BigDecimal expectedAmount = getAmountFromOrder(orders, paymentType);
                            BigDecimal actualAmount = getAmountFromStatistics(userShiftPayStatistics, paymentType);

                            assertEquals(0, expectedAmount.compareTo(actualAmount));
                        }
                );

    }

    @Test
    void testUserShiftPayStatistics_withPartialReturn() {
        //given
        List<Order> orders = List.of(
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.MIXED),
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.MIXED),
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.MIXED),
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.MIXED));

        UserShift openedShift = prepareFinishedUserShiftWithOrders(orders);

        //when
        UserShiftPayStatisticsDto userShiftPayStatistics =
                userShiftQueryService.getUserShiftPayStatistics(openedShift.getId());

        //then
        BigDecimal expectedAmount = getAmountFromOrder(orders, OrderPaymentType.CASH);
        BigDecimal actualAmount = getAmountFromStatistics(userShiftPayStatistics, OrderPaymentType.CASH);

        assertEquals(0, expectedAmount.compareTo(actualAmount));
        assertEquals(1, userShiftPayStatistics.getPayments().size());
        assertEquals(orders.size(), userShiftPayStatistics.getPayments().get(0).getQuantity());
    }

    @Test
    void testUserShiftPayStatistics_withAllReturns_withoutDelivery() {
        //given
        List<Order> orders = List.of(
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, true, OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY));

        UserShift openedShift = prepareFinishedUserShiftWithOrders(orders,
                OrderDeliveryTaskFailReasonType.CANCEL_ORDER);

        //when
        UserShiftPayStatisticsDto userShiftPayStatistics =
                userShiftQueryService.getUserShiftPayStatistics(openedShift.getId());

        //then
        BigDecimal actualAmount = getAmountFromStatistics(userShiftPayStatistics, OrderPaymentType.CASH);

        assertEquals(0, BigDecimal.ZERO.compareTo(actualAmount));
        assertEquals(0, userShiftPayStatistics.getPayments().size());
    }


    @Test
    void testUserShiftPayStatistics_withAllReturns_withDelivery() {
        //given
        List<Order> orders = List.of(
                generateOrderAllReturned(OrderPaymentType.CASH),
                generateOrderAllReturned(OrderPaymentType.CASH),
                generateOrderAllReturned(OrderPaymentType.CASH),
                generateOrderAllReturned(OrderPaymentType.CASH));

        //необходимо отменить заказы и проверить на них
        UserShift openedShift = prepareFinishedUserShiftWithOrders(orders,
                OrderDeliveryTaskFailReasonType.CANCEL_ORDER);

        //when
        UserShiftPayStatisticsDto userShiftPayStatistics =
                userShiftQueryService.getUserShiftPayStatistics(openedShift.getId());

        //then
        BigDecimal actualAmount = getAmountFromStatistics(userShiftPayStatistics, OrderPaymentType.CASH);

        assertEquals(0, BigDecimal.ZERO.compareTo(actualAmount));
        assertEquals(0, userShiftPayStatistics.getPayments().size());
    }

    @Test
    void testUserShiftPayStatistics_withOutInstances() {
        //given
        List<Order> orders = List.of(
                generateOrder(OrderPaymentType.CASH, false,
                        OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, false,
                        OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, false,
                        OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, false,
                        OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY),
                generateOrder(OrderPaymentType.CASH, false,
                        OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY));

        UserShift openedShift = prepareFinishedUserShiftWithOrders(orders);

        //when
        UserShiftPayStatisticsDto userShiftPayStatistics =
                userShiftQueryService.getUserShiftPayStatistics(openedShift.getId());

        //then
        BigDecimal expectedAmount = getAmountFromOrder(orders, OrderPaymentType.CASH);
        BigDecimal actualAmount = getAmountFromStatistics(userShiftPayStatistics, OrderPaymentType.CASH);

        assertEquals(0, expectedAmount.compareTo(actualAmount));
        assertEquals(1, userShiftPayStatistics.getPayments().size());
        assertEquals(orders.size(), userShiftPayStatistics.getPayments().get(0).getQuantity());
    }

    private BigDecimal getAmountFromStatistics(UserShiftPayStatisticsDto userShiftPayStatistics,
                                               OrderPaymentType paymentType) {
        return userShiftPayStatistics
                .getPayments()
                .stream()
                .filter(payDtoEntry -> paymentType.equals(payDtoEntry.getPaymentType()))
                .map(UserShiftPayStatisticsDto.PayDtoEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getAmountFromOrder(List<Order> orders, OrderPaymentType paymentType) {
        return orders
                .stream()
                .filter(order -> paymentType.equals(order.getPaymentType()))
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .map(orderItem -> orderItem
                        .getPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getCount() - getNotPurchasedItemsCount(orderItem))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long getNotPurchasedItemsCount(OrderItem orderItem) {
        return Optional.ofNullable(orderItem.getInstances())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(oii -> !OrderItemInstancePurchaseStatus.PURCHASED.equals(oii.getPurchaseStatus()))
                .count();
    }

    @NotNull
    private UserShift prepareFinishedUserShiftWithOrders(List<Order> orders) {
        return prepareFinishedUserShiftWithOrders(orders, null);
    }

    @NotNull
    private UserShift prepareFinishedUserShiftWithOrders(List<Order> orders, OrderDeliveryTaskFailReasonType fail) {
        UserShift openedShift = userHelper.createOpenedShift(user1, orders, LocalDate.now());

        transactionTemplate.execute(tt -> {
            userHelper.checkinAndFinishPickup(openedShift);
            userShiftRepository.findByIdWithRoutePoints(openedShift.getId()).get()
                    .streamDeliveryRoutePoints()
                    .remove(rp -> rp.getStatus().isTerminal())
                    .forEach(rp -> userHelper.finishAllOrderDeliveryTasks(rp, fail, true, orders
                            .stream()
                            .collect(Collectors.toMap(
                                    Order::getId,
                                    Order::getPaymentType
                            ))));
            return null;
        });
        return openedShift;
    }

    private Order generateOrder(OrderPaymentType paymentType) {
        return generateOrder(paymentType, true,
                OrderGenerateParam.PurchaseStrategy.PURCHASED_ONLY);
    }

    private Order generateOrder(OrderPaymentType paymentType, boolean withItemInstance,
                                OrderGenerateParam.PurchaseStrategy purchaseStrategy) {
        return generateOrder(paymentType, withItemInstance, purchaseStrategy, false);
    }

    private Order generateOrderAllReturned(OrderPaymentType paymentType) {
        return generateOrder(paymentType, true, OrderGenerateParam.PurchaseStrategy.RETURNED_ONLY, true);
    }

    private Order generateOrder(OrderPaymentType paymentType, boolean withItemInstance,
                                OrderGenerateParam.PurchaseStrategy purchaseStrategy,
                                boolean withDelivery) {
        int itemsCount = RandomUtils.nextInt(5, 10);
        int itemsItemCount = RandomUtils.nextInt(2, 5);
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .itemsCount(itemsCount)
                                        .itemsItemCount(itemsItemCount)
                                        .itemsPrice(BigDecimal.valueOf(RandomUtils.nextInt(100, 1000)))
                                        .build()
                        )
                        .deliveryPrice(withDelivery ? BigDecimal.valueOf(49) : BigDecimal.ZERO)
                        .paymentType(paymentType)
                        .withItemInstance(withItemInstance)
                        .purchaseStrategy(purchaseStrategy)
                        .build()
        );
    }

}
