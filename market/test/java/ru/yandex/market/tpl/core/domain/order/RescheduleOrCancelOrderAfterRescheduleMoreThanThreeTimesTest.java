package ru.yandex.market.tpl.core.domain.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.partner.DeliveryServiceFactory;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.sms.SmsTemplateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerOrderDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.service.dropoff.MovementCargoCollector;
import ru.yandex.market.tpl.core.service.task.ReturnItemsCollector;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus.CANCELLED;
import static ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus.NOT_DELIVERED;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.READY_FOR_RETURN;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.SORTING_CENTER_ARRIVED;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.SORTING_CENTER_CREATED;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.SORTING_CENTER_PREPARED;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.EXTRA_RESCHEDULING;
import static ru.yandex.market.tpl.api.model.task.Source.SYSTEM;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.core.domain.order.OrderDeliveryRescheduledEventListener.DEFAULT_AVAILABLE_RESCHEDULE;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class RescheduleOrCancelOrderAfterRescheduleMoreThanThreeTimesTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final OrderManager orderManager;
    private final Clock clock;
    private final DeliveryServiceFactory deliveryServiceFactory;
    private final OrderCommandService orderCommandService;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @MockBean
    private SortingCenterPropertyService sortingCenterPropertyService;

    @MockBean
    private ReturnItemsCollector itemsCollector;


    @SpyBean
    private SmsTemplateService smsTemplateService;

    private User user;
    private List<Order> orders;
    private Order orderCreated;
    private LocalDate initialDeliveryDate;
    private LocalDate now;


    @BeforeEach
    void init() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_RESCHEDULED_LIMITED)).thenReturn(true);
        when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED)).thenReturn(true);
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED), any())
        ).thenReturn(true);
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.RECALCULATE_MULTIPLIERS_ENABLED), any())
        ).thenReturn(true);
        when(itemsCollector.isSkipDirectDropoffScanOnScEnabled(any())).thenReturn(false);

        now = LocalDate.now(clock);
        initialDeliveryDate = now;
        user = userHelper.findOrCreateUser(35338L, now);

        orders = Stream.generate(orderGenerateService::createOrder).limit(11)
                .collect(Collectors.toList());

        orderCreated = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(239L)
                        .flowStatus(OrderFlowStatus.CREATED)
                        .build()
        );

        deliveryServiceFactory.createScheduledIntervalsForDeliveryServiceAllTimeAvailable();
    }

    private UserShift prepareShift(LocalDate date, Order order) {
        Shift shift = userHelper.findOrCreateOpenShift(date);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", order.getDelivery().getDeliveryDate(DEFAULT_ZONE_ID), 12,
                        order.getId(), false))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        UserShift userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);

        return userShift;
    }


    @Test
    void shouldCancelAfterRescheduleMoreThanThreeTimes() {
        Order order = orders.get(0);
        UserShift us = prepareShift(now, order);
        rescheduleWithNewDate(1, Source.CLIENT, order, 20, 22);

        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(1, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(1, ChronoUnit.DAYS));

        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(1), order);
        rescheduleWithNewDate(2, Source.DELIVERY, order, 20, 22);
        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(2, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(2, ChronoUnit.DAYS));
        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);
        us = prepareShift(initialDeliveryDate.plusDays(2), order);
        rescheduleWithNewIntervalByCourier(3, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(3, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(3, ChronoUnit.DAYS));

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(3), order);
        rescheduleWithNewIntervalByCourier(4, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        verify(smsTemplateService).cancelExtraRescheduledOrder(order.getExternalOrderId());
    }

    @Test
    void shouldCancelAfterRescheduleMoreThanThreeTimesFromCourier() {
        Order order = orders.get(1);
        UserShift us = prepareShift(now, order);
        //1
        rescheduleWithNewIntervalByCourier(1, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(1, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(1, ChronoUnit.DAYS));
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(1), order);
        //2
        rescheduleWithNewIntervalByCourier(2, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(2, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(2, ChronoUnit.DAYS));
        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);
        us = prepareShift(initialDeliveryDate.plusDays(2), order);
        //3
        rescheduleWithNewIntervalByCourier(3, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(3, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(3, ChronoUnit.DAYS));

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(3), order);
        //4
        rescheduleWithNewIntervalByCourier(4, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        // дата доставки не обновлялась
        verify(smsTemplateService).cancelExtraRescheduledOrder(order.getExternalOrderId());

    }

    @Test
    @Disabled("MARKETTPL-2517")
    void testOrderRescheduleAndThenCancelOrderAfter3TimesReschedulesByCourier() {
        Order order = orders.get(9);
        UserShift us = prepareShift(now, order);
        RoutePoint routePointOrderDeliveryTask = us.getCurrentRoutePoint();
        OrderDeliveryTask task = routePointOrderDeliveryTask.streamOrderDeliveryTasks().findFirst().orElseThrow();

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        //Первый перенос
        rescheduleWithNewIntervalByCourier(1, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, 20, 22);
        checkStatusesAfterRescheduled(order, task);

        //Первое переоткрытие
        reopenTaskByCourier(us, routePointOrderDeliveryTask, task);
        checkStatusesAfterReopen(order, task);

        rescheduleWithNewIntervalByCourier(2, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, 14, 16);
        checkStatusesAfterRescheduled(order, task);

        reopenTaskByCourier(us, routePointOrderDeliveryTask, task);
        checkStatusesAfterReopen(order, task);

        rescheduleWithNewIntervalByCourier(3, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        checkStatusesAfterRescheduled(order, task);

        reopenTaskByCourier(us, routePointOrderDeliveryTask, task);
        checkStatusesAfterReopen(order, task);

        //После попытки 4 раз перенести - фейлим всё
        rescheduleWithNewIntervalByCourier(1, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(READY_FOR_RETURN);
        assertThat(order.getDeliveryStatus()).isEqualTo(CANCELLED);
        assertThat(task.getFailReason().getType()).isEqualTo(EXTRA_RESCHEDULING);
        assertThat(task.getFailReason().getSource()).isEqualTo(SYSTEM);

        //Курьер не сможет переоткрыть таску, так как более 3 раз было перенесено
        assertThrows(CommandFailedException.class, () -> reopenTaskByCourier(us, routePointOrderDeliveryTask, task));
    }

    @Test
    void shouldNotCancelAfter3ReschedulesWithoutSortingCenterArrivedStatus() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .flowStatus(SORTING_CENTER_PREPARED)
                        .build()
        );
        UserShift us = prepareShift(now, order);

        for (int i = 1; i < DEFAULT_AVAILABLE_RESCHEDULE; i++) {
            rescheduleWithNewIntervalByCourier(i, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);
            var returnRoutePoint = us.getCurrentRoutePoint();
            var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
            commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                    returnRoutePoint.getId(), taskForReturn.getId()));
            commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
            Interval deliveryInterval = new Interval(
                    DateTimeUtil.todayAtHour(20, clock).plus(i, ChronoUnit.DAYS),
                    DateTimeUtil.todayAtHour(22, clock).plus(i, ChronoUnit.DAYS));
            assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

            us = prepareShift(initialDeliveryDate.plusDays(i), order);
        }

        rescheduleWithNewIntervalByCourier(6, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);

        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(SORTING_CENTER_PREPARED);
    }

    @Test
    void shouldCancelAfter3ReschedulesWithSortingCenterArrivedStatus() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .flowStatus(SORTING_CENTER_CREATED)
                        .build()
        );
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_ARRIVED), SYSTEM);
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_PREPARED), SYSTEM);
        orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream()
                .peek(history -> history.setOrderFlowStatusUpdatedAt(Instant.now(clock)))
                .forEach(orderFlowStatusHistoryRepository::saveAndFlush);

        UserShift us = prepareShift(now, order);
        for (int i = 1; i < DEFAULT_AVAILABLE_RESCHEDULE; i++) {
            rescheduleWithNewIntervalByCourier(i, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);
            var returnRoutePoint = us.getCurrentRoutePoint();
            var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
            commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                    returnRoutePoint.getId(), taskForReturn.getId()));
            commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
            Interval deliveryInterval = new Interval(
                    DateTimeUtil.todayAtHour(20, clock).plus(i, ChronoUnit.DAYS),
                    DateTimeUtil.todayAtHour(22, clock).plus(i, ChronoUnit.DAYS));
            assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

            us = prepareShift(initialDeliveryDate.plusDays(i), order);
        }

        rescheduleWithNewIntervalByCourier(6, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);

        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(READY_FOR_RETURN);
    }

    @Test
    void shouldCancelPickupOrderAfter3ReschedulesWithSortingCenterArrivedStatus() {
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        Order order = lockerOrderDataHelper.getPickupOrder(
                shift, "pickup_external_id_123", pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_ARRIVED), SYSTEM);
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_PREPARED), SYSTEM);
        orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream()
                .peek(history -> history.setOrderFlowStatusUpdatedAt(Instant.now(clock)))
                .forEach(orderFlowStatusHistoryRepository::saveAndFlush);

        UserShift us = prepareShift(now, order);
        for (int i = 1; i < DEFAULT_AVAILABLE_RESCHEDULE; i++) {
            rescheduleWithNewIntervalByCourier(i, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);
            var returnRoutePoint = us.getCurrentRoutePoint();
            var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
            commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                    returnRoutePoint.getId(), taskForReturn.getId()));
            commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
            Interval deliveryInterval = new Interval(
                    DateTimeUtil.todayAtHour(20, clock).plus(i, ChronoUnit.DAYS),
                    DateTimeUtil.todayAtHour(22, clock).plus(i, ChronoUnit.DAYS));
            assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

            us = prepareShift(initialDeliveryDate.plusDays(i), order);
        }

        rescheduleWithNewIntervalByCourier(6, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);

        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(READY_FOR_RETURN);
    }

    @Test
    void shouldNotCancelAfter3ReschedulesWithoutDeliveryTask() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .flowStatus(SORTING_CENTER_CREATED)
                        .build()
        );
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_ARRIVED), SYSTEM);
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_PREPARED), SYSTEM);
        orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream()
                .peek(history -> history.setOrderFlowStatusUpdatedAt(Instant.now(clock)))
                .forEach(orderFlowStatusHistoryRepository::saveAndFlush);
        UserShift us = prepareShift(now, order);

        for (int i = 1; i < DEFAULT_AVAILABLE_RESCHEDULE + 1; i++) {
            rescheduleWithNewDate(i, SYSTEM, order, 20, 22);
        }

        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    @Test
    void shouldCancelDeliveryTaskExtraRescheduledByCourierAndClientRequest() {
        Order order = orders.get(2);
        UserShift us = prepareShift(now, order);
        //1
        rescheduleWithNewIntervalByCourier(1, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(1, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(1, ChronoUnit.DAYS));
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(1), order);
        //2
        rescheduleWithNewIntervalByCourier(2, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(2, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(2, ChronoUnit.DAYS));
        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);
        us = prepareShift(initialDeliveryDate.plusDays(2), order);
        //3
        rescheduleWithNewIntervalByCourier(3, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(3, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(3, ChronoUnit.DAYS));

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(3), order);
        //4
        RoutePoint routePoint = us.getCurrentRoutePoint();
        DeliveryTask deliveryTask = routePoint.streamDeliveryTasks().findFirst().get();
        rescheduleWithNewIntervalByCourier(4, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        assertThat(us.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);

        Long userShiftId = us.getId();
        assertThrows(CommandFailedException.class,
                () -> commandService.reopenDeliveryTask(user,
                        new UserShiftCommand.ReopenOrderDeliveryTask(
                                userShiftId, routePoint.getId(), deliveryTask.getId(), Source.COURIER)));
    }

    @Test
    void shouldNoCancelAfterRescheduleMoreThanThreeTimesIfReasonIsNotClient() {
        Order order = orders.get(3);
        UserShift us = prepareShift(now, order);
        //1
        rescheduleWithNewIntervalByCourier(1, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(1, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(1, ChronoUnit.DAYS));
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(1), order);
        //2
        rescheduleWithNewIntervalByCourier(2, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(2, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(2, ChronoUnit.DAYS));
        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);
        us = prepareShift(initialDeliveryDate.plusDays(2), order);
        //3
        rescheduleWithNewIntervalByCourier(3, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(3, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(3, ChronoUnit.DAYS));

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

        us = prepareShift(initialDeliveryDate.plusDays(3), order);
        //4
        rescheduleWithNewIntervalByCourier(4, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        returnRoutePoint = us.getCurrentRoutePoint();
        taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }


    @Test
    void shouldCancelAfterRescheduleMoreThanThreeTimesWithoutDeliveringBeforeFinishingShift() {
        Order order = orders.get(4);
        UserShift us = prepareShift(now, order);

        rescheduleWithNewDate(1, Source.CLIENT, order, 20, 22);
        rescheduleWithNewDate(2, Source.DELIVERY, order, 20, 22);
        rescheduleWithNewDate(3, Source.DELIVERY, order, 20, 22);
        rescheduleWithNewDate(4, Source.CLIENT, order, 20, 22);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        verify(smsTemplateService).cancelExtraRescheduledOrder(order.getExternalOrderId());
    }

    @Test
    void shouldNoCancelAfterRescheduleMoreThanThreeTimesBeforeFinishingShiftIfLastReshedulingSameDate() {
        Order order = orders.get(5);
        UserShift us = prepareShift(now, order);

        rescheduleWithNewDate(1, Source.CLIENT, order, 20, 22);
        rescheduleWithNewDate(2, Source.DELIVERY, order, 20, 22);
        rescheduleWithNewDate(3, Source.DELIVERY, order, 20, 22);
        rescheduleWithNewDate(3, Source.CLIENT, order, 18, 20);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    @Test
    void shouldNoCancelAfterRescheduleMoreThanThreeTimesInOrderWithFlowStatusCreated() {
        Order order = orderCreated;
        rescheduleWithNewDate(1, Source.CLIENT, orderCreated, 20, 22);
        rescheduleWithNewDate(2, Source.COURIER, orderCreated, 20, 22);
        rescheduleWithNewDate(3, Source.DELIVERY, orderCreated, 20, 22);
        rescheduleWithNewDate(4, Source.CLIENT, orderCreated, 20, 22);
        assertThat(orderCreated.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CREATED);
    }

    @Test
    void shouldCancelAfterRescheduleMoreThanThreeTimesInFinishedUserShift() {
        Order order = orders.get(6);
        UserShift us = prepareShift(now, order);
        rescheduleWithNewDate(1, Source.CLIENT, order, 20, 22);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);

        rescheduleWithNewDate(2, Source.CLIENT, order, 20, 22);
        rescheduleWithNewDate(3, Source.DELIVERY, order, 20, 22);
        rescheduleWithNewDate(4, Source.DELIVERY, order, 20, 22);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        verify(smsTemplateService).cancelExtraRescheduledOrder(order.getExternalOrderId());

    }

    @Test
    void shouldNoCancelAfterRescheduleMoreThanThreeTimesIfDataDoesntChange() {
        Order order = orders.get(7);
        UserShift us = prepareShift(now, order);
        //1
        rescheduleWithNewDate(1, Source.CLIENT, order, 20, 22);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        //2
        rescheduleWithNewDate(2, Source.CLIENT, order, 20, 22);
        //3 (same date)
        rescheduleWithNewDate(2, Source.DELIVERY, order, 9, 18);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        //4 заказ не должн быть закрыть так как дата не менялась
        rescheduleWithNewDate(3, Source.DELIVERY, order, 20, 22);
        Interval lastDeliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(3, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(3, ChronoUnit.DAYS));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(lastDeliveryInterval);
        //5. заказ должны быть закрыть
        rescheduleWithNewDate(4, Source.DELIVERY, order, 20, 22);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
    }

    @Test
    void shouldNoCancelAfterRescheduleMoreThanThreeTimesIfSourceIsUnavailable() {
        Order order = orders.get(8);
        UserShift us = prepareShift(now, order);        //1
        rescheduleWithNewDate(1, Source.CLIENT, order, 20, 22);
        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);

        rescheduleWithNewDate(2, SYSTEM, order, 20, 22);
        // оператор может менять сколько угодно раз
        rescheduleWithNewDate(3, Source.OPERATOR, order, 20, 22);
        // 2
        rescheduleWithNewDate(4, Source.DELIVERY, order, 20, 22);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        // 3
        rescheduleWithNewDate(5, Source.DELIVERY, order, 20, 22);
        Interval lastDeliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(5, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(5, ChronoUnit.DAYS));
        // заказ не отменен (из лимитированных source на кол-во переносов только 3 переноса было)
        assertThat(order.getDelivery().getInterval()).isEqualTo(lastDeliveryInterval);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);

    }

    @Test
    void shouldCancelAfter3ReschedulesWithSortingCenterPreparedStatus() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .flowStatus(SORTING_CENTER_CREATED)
                        .build()
        );
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), SORTING_CENTER_PREPARED), SYSTEM);
        orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream()
                .peek(history -> history.setOrderFlowStatusUpdatedAt(Instant.now(clock)))
                .forEach(orderFlowStatusHistoryRepository::saveAndFlush);

        UserShift us = prepareShift(now, order);
        for (int i = 1; i < DEFAULT_AVAILABLE_RESCHEDULE; i++) {
            rescheduleWithNewIntervalByCourier(i, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);
            var returnRoutePoint = us.getCurrentRoutePoint();
            var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
            commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                    returnRoutePoint.getId(), taskForReturn.getId()));
            commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
            Interval deliveryInterval = new Interval(
                    DateTimeUtil.todayAtHour(20, clock).plus(i, ChronoUnit.DAYS),
                    DateTimeUtil.todayAtHour(22, clock).plus(i, ChronoUnit.DAYS));
            assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);

            us = prepareShift(initialDeliveryDate.plusDays(i), order);
        }

        rescheduleWithNewIntervalByCourier(6, us, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);

        var returnRoutePoint = us.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(us, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(us.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(us.getId()));
        assertThat(order.getOrderFlowStatus()).isEqualTo(READY_FOR_RETURN);
    }

    private void rescheduleWithNewDate(int days, Source source, Order order, int startHour, int finishHour) {
        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(startHour, clock).plus(days, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(finishHour, clock).plus(days, ChronoUnit.DAYS));

        orderManager.rescheduleOrder(
                order,
                deliveryInterval,
                source);
    }

    private void rescheduleWithNewIntervalByCourier(int days, UserShift us,
                                                    OrderDeliveryRescheduleReasonType rescheduleReasonType) {
        rescheduleWithNewIntervalByCourier(days, us, rescheduleReasonType, 20, 22);
    }

    private void rescheduleWithNewIntervalByCourier(int days, UserShift us,
                                                    OrderDeliveryRescheduleReasonType rescheduleReasonType,
                                                    int hourStart, int hourEnd) {
        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(hourStart, clock).plus(days, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(hourEnd, clock).plus(days, ChronoUnit.DAYS));
        RoutePoint rp = us.getCurrentRoutePoint();

        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                us.getId(), rp.getId(), rp.streamDeliveryTasks().findFirst().orElseThrow().getId(),
                DeliveryReschedule.fromCourier(user, deliveryInterval.getStart(),
                        deliveryInterval.getEnd(),
                        rescheduleReasonType), todayAtHour(9, clock), DEFAULT_ZONE_ID));
    }

    private void reopenTaskByCourier(UserShift userShift, RoutePoint routePoint, OrderDeliveryTask task) {
        commandService.reopenDeliveryTask(user,
                new UserShiftCommand.ReopenOrderDeliveryTask(userShift.getId(),
                        routePoint.getId(), task.getId(), Source.COURIER)
        );
    }

    private void checkStatusesAfterRescheduled(Order order, OrderDeliveryTask orderDeliveryTask) {
        assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(TRANSPORTATION_RECIPIENT);
        assertThat(order.getDeliveryStatus()).isEqualTo(NOT_DELIVERED);
    }

    private void checkStatusesAfterReopen(Order order, OrderDeliveryTask orderDeliveryTask) {
        assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(TRANSPORTATION_RECIPIENT);
        assertThat(order.getDeliveryStatus()).isEqualTo(NOT_DELIVERED);
    }

}
