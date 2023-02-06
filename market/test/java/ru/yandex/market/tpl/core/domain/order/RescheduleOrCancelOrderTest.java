package ru.yandex.market.tpl.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.ClientReturnRescheduleReasonTypeDto;
import ru.yandex.market.tpl.api.model.task.ClientReturnTaskFailReasonTypeDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonTypeDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonTypeDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.sms.SmsTemplateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus.CANCELLED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.CLIENT_REQUEST;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.COURIER_CLIENT_RETURN_RESCHEDULE_REASONS;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.COURIER_CLIENT_RETURN_FAIL_REASONS;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class RescheduleOrCancelOrderTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final UserShiftQueryService userShiftQueryService;
    private final OrderCommandService orderCommandService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final OrderManager orderManager;
    private final Clock clock;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterService sortingCenterService;
    private final SortingCenterPropertyService sortingCenterPropertyService;

    @SpyBean
    private SmsTemplateService smsTemplateService;

    private User user;
    private UserShift userShift;
    private Order order;
    private LocalDate initialDeliveryDate;

    @BeforeEach
    void init() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_RESCHEDULED_LIMITED, true);
        LocalDate now = LocalDate.now(clock);
        initialDeliveryDate = now;
        user = userHelper.findOrCreateUser(35336L, now);

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(now)
                .buyerYandexUid(-2L)
                .build());

        userShift = prepareShift(now, order);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterService.findSortCenterForDs(order.getDeliveryServiceId()),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED,
                false
        );
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
    void shouldUnassignOrdersWhenDeliveredFailedByOrderWasLost() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();
        assertThat(orderRepository.findCurrentUserOrders(user.getId())).contains(order);
        Interval deliveryInterval = order.getDelivery().getInterval();

        commandService.failDeliverySubtask(
                null,
                new UserShiftCommand.FailOrderDeliverySubtask(
                        task.getDeliveryTask().getRoutePoint().getUserShift().getId(),
                        task.getDeliveryTask().getRoutePoint().getId(),
                        task,
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST, "ORDER_WAS_LOST!")
                ));
        userHelper.finishFullReturnAtEnd(userShift);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.LOST);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);
        assertThat(orderRepository.findCurrentUserOrders(user.getId())).doesNotContain(order);

    }

    @Test
    void shouldCancelOrdersWhenDeliveredFailedByNoPassport() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();
        assertThat(orderRepository.findCurrentUserOrders(user.getId())).contains(order);

        commandService.failDeliverySubtask(
                null,
                new UserShiftCommand.FailOrderDeliverySubtask(
                        task.getDeliveryTask().getRoutePoint().getUserShift().getId(),
                        task.getDeliveryTask().getRoutePoint().getId(),
                        task,
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_PASSPORT, null)
                ));
        userHelper.finishFullReturnAtEnd(userShift);
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        assertThat(order.getDeliveryStatus()).isEqualTo(CANCELLED);

    }

    @Test
    void shouldRescheduleToTomorrowWhenDeliveredFailedByCourierNeedsHelp() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "HELP!")
        ));
        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_BY_TASK, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_BY_TASK);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(
                LocalDate.ofInstant(order.getDelivery().getDeliveryIntervalFrom().truncatedTo(ChronoUnit.DAYS),
                        ZoneId.systemDefault())).isEqualTo(initialDeliveryDate.plusDays(1));
    }

    @Test
    void shouldRescheduleIn2DaysForwardWhenDeliveredFailedByWrongAddress() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS, null)
        ));
        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_BY_TASK, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_BY_TASK);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(LocalDate.ofInstant(order.getDelivery().getDeliveryIntervalFrom().truncatedTo(ChronoUnit.DAYS),
                ZoneId.systemDefault())).isEqualTo(initialDeliveryDate.plusDays(2));
    }

    @Test
    void shouldRescheduleIn2DaysForwardWhenDeliveredFailedByWrongAddressByClient() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT, null)
        ));
        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream().filter(e -> e.getDsApiCheckpoint() == 46).collect(Collectors.toList())).isEmpty();
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_BY_TASK, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_BY_TASK);
        assertThat(orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream().filter(e -> e.getDsApiCheckpoint() == 46).collect(Collectors.toList())).isNotEmpty();
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(LocalDate.ofInstant(order.getDelivery().getDeliveryIntervalFrom().truncatedTo(ChronoUnit.DAYS),
                ZoneId.systemDefault())).isEqualTo(initialDeliveryDate.plusDays(2));
    }

    @Test
    void shouldRescheduleToTomorrowWhenNoContact() {
        finishUserShiftWithNoContact(userShift);

        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(
                LocalDate.ofInstant(order.getDelivery().getDeliveryIntervalFrom().truncatedTo(ChronoUnit.DAYS),
                        ZoneId.systemDefault())).isEqualTo(initialDeliveryDate.plusDays(1));
    }

    @Test
    void shouldNotRescheduleToTomorrowWhenNoContactButOrderDelivered() {
        failOrderDeliveryWithNoContact(userShift);
        orderCommandService.forceUpdateFlowStatus(new OrderCommand.UpdateFlowStatus(order.getId(),
                OrderFlowStatus.DELIVERED_TO_RECIPIENT));

        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_RECIPIENT);
    }

    @Test
    void shouldReturnOrderDeliveryTaskFailReasons() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_WRONG_ADDRESS_REASON_UPDATED, false);
        var failReasonTypes = userShiftQueryService.getOrderDeliveryTaskFailReasons().stream()
                .map(OrderDeliveryTaskFailReasonTypeDto::getName).collect(Collectors.toList());
        assertThat(failReasonTypes).contains(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS.name());
        assertThat(failReasonTypes).doesNotContain(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT.name());
        assertThat(failReasonTypes).doesNotContain(OrderDeliveryTaskFailReasonType.WRONG_COORDINATES.name());
        assertThat(failReasonTypes).doesNotContainAnyElementsOf(Set.of(
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_MISMATCH.name(),
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH.name()
        ));

        configurationServiceAdapter.updateValue(ConfigurationProperties.IS_WRONG_ADDRESS_REASON_UPDATED, true);
        failReasonTypes = userShiftQueryService.getOrderDeliveryTaskFailReasons().stream()
                .map(OrderDeliveryTaskFailReasonTypeDto::getName).collect(Collectors.toList());
        assertThat(failReasonTypes).doesNotContain(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS.name());
        assertThat(failReasonTypes).contains(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT.name());
        assertThat(failReasonTypes).contains(OrderDeliveryTaskFailReasonType.WRONG_COORDINATES.name());
        assertThat(failReasonTypes).doesNotContainAnyElementsOf(Set.of(
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_MISMATCH.name(),
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH.name()
        ));
        configurationServiceAdapter.deleteValue(ConfigurationProperties.IS_WRONG_ADDRESS_REASON_UPDATED);
    }

    @Test
    void shouldReturnOrderDeliveryTaskRescheduleReasons() {
        var rescheduleReasonTypes = userShiftQueryService.getOrderDeliveryTaskRescheduleReasons().stream()
                .map(OrderDeliveryRescheduleReasonTypeDto::getName).collect(Collectors.toList());
        assertThat(rescheduleReasonTypes.size()).isEqualTo(4);
        assertThat(rescheduleReasonTypes).contains(CLIENT_REQUEST.name());
        assertThat(rescheduleReasonTypes).doesNotContain(OrderDeliveryRescheduleReasonType.COORDINATES_UPDATED.name());
        assertThat(rescheduleReasonTypes).doesNotContainAnyElementsOf(
                COURIER_CLIENT_RETURN_RESCHEDULE_REASONS
                        .stream()
                        .filter(type -> type != CLIENT_REQUEST)
                        .map(OrderDeliveryRescheduleReasonType::name)
                        .collect(Collectors.toList())
        );
    }

    @Test
    void shouldReturnClientReturnTaskResheduleReasons() {
        var clientReturnRescheduleTypes = userShiftQueryService.getClientReturnTaskRescheduleReasons()
                .stream()
                .map(ClientReturnRescheduleReasonTypeDto::getName)
                .collect(Collectors.toList());

        assertThat(clientReturnRescheduleTypes).containsExactlyInAnyOrderElementsOf(
                COURIER_CLIENT_RETURN_RESCHEDULE_REASONS
                        .stream()
                        .map(OrderDeliveryRescheduleReasonType::name)
                        .collect(Collectors.toList())
        );
    }

    @Test
    void shouldReturnClientReturnTaskFailedReasons() {
        var clientReturnFailedTypes = userShiftQueryService.getClientReturnTaskFailReasons()
                .stream()
                .map(ClientReturnTaskFailReasonTypeDto::getName)
                .collect(Collectors.toList());

        assertThat(clientReturnFailedTypes).containsExactlyInAnyOrderElementsOf(
                COURIER_CLIENT_RETURN_FAIL_REASONS
                        .stream()
                        .map(OrderDeliveryTaskFailReasonType::name)
                        .collect(Collectors.toList())
        );
    }

    @Test
    void shouldNotRescheduleToTomorrowWhenNoContactAfterRescheduledByClient() {
        failOrderDeliveryWithNoContact(userShift);

        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(10, clock).plus(2, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(10, clock).plus(2, ChronoUnit.DAYS));

        orderManager.rescheduleOrder(
                order,
                deliveryInterval,
                Source.DELIVERY);
        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(deliveryInterval);
    }

    @Test
    void shouldCancelAfterNoContactThreeTimesT() {
        RoutePoint rp = userShift.getCurrentRoutePoint();

        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                userShift.getId(), rp.getId(), rp.streamDeliveryTasks().findFirst().orElseThrow().getId(),
                DeliveryReschedule.fromCourier(user, tomorrowAtHour(20, clock), tomorrowAtHour(22, clock),
                        CLIENT_REQUEST), todayAtHour(9, clock),
                userShift.getZoneId()
        ));
        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        UserShift userShiftFirstAttempt = prepareShift(initialDeliveryDate.plusDays(1), order);
        finishUserShiftWithNoContact(userShiftFirstAttempt);
        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);
        // вторая попытка доставки с "недозвоном"
        UserShift userShiftSecondAttempt = prepareShift(initialDeliveryDate.plusDays(2), order);
        finishUserShiftWithNoContact(userShiftSecondAttempt);
        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(
                LocalDate.ofInstant(order.getDelivery().getDeliveryIntervalFrom().truncatedTo(ChronoUnit.DAYS),
                        ZoneId.systemDefault())).isEqualTo(initialDeliveryDate.plusDays(3));
        // третья попытка доставки с "недозвоном"
        UserShift userShiftThirdAttempt = prepareShift(initialDeliveryDate.plusDays(3), order);
        finishUserShiftWithNoContact(userShiftThirdAttempt);

        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 0);

        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);

        // проверяем, что отправляли смс об отмене заказа
        verify(smsTemplateService).cancelOrder();
    }

    @Test
    void shouldCancelAfterNoContactThreeTimes() {
        finishUserShiftWithNoContact(userShift);

        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);
        // вторая попытка доставки с "недозвоном"
        UserShift userShiftSecondAttempt = prepareShift(initialDeliveryDate.plusDays(1), order);
        finishUserShiftWithNoContact(userShiftSecondAttempt);

        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(
                LocalDate.ofInstant(order.getDelivery().getDeliveryIntervalFrom().truncatedTo(ChronoUnit.DAYS),
                        ZoneId.systemDefault())).isEqualTo(initialDeliveryDate.plusDays(2));

        // третья попытка доставки с "недозвоном"
        UserShift userShiftThirdAttempt = prepareShift(initialDeliveryDate.plusDays(2), order);
        finishUserShiftWithNoContact(userShiftThirdAttempt);

        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 0);

        // проверяем, что отправляли смс об отмене заказа
        verify(smsTemplateService).cancelOrder();

        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
    }

    @Test
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void shouldCancelAfterNoContactThreeTimesAndSendCommunicationFromTriggersPlatform() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_CANCELLED_MORE_3_CALLED_ENABLED, true);

        finishUserShiftWithNoContact(userShift);

        // вторая попытка доставки с "недозвоном"
        UserShift userShiftSecondAttempt = prepareShift(initialDeliveryDate.plusDays(1), order);
        finishUserShiftWithNoContact(userShiftSecondAttempt);

        // третья попытка доставки с "недозвоном"
        UserShift userShiftThirdAttempt = prepareShift(initialDeliveryDate.plusDays(2), order);
        finishUserShiftWithNoContact(userShiftThirdAttempt);

        // проверяем, что отправляли смс об отмене заказа
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    void shouldSendSmsAboutRescheduleOrder() {
        finishUserShiftWithNoContact(userShift);

        verify(smsTemplateService)
                .rescheduleOrder(
                        eq(order.getExternalOrderId()),
                        eq(initialDeliveryDate.plusDays(1)),
                        eq(DateTimeUtil.toLocalTime(order.getDelivery().getDeliveryIntervalFrom()).getHour()),
                        eq(DateTimeUtil.toLocalTime(order.getDelivery().getDeliveryIntervalTo()).getHour()));
    }

    @Test
    void shouldSendSmsAboutRescheduleOrderAndSendCommunicationFromTriggersPlatform() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_RESCHEDULED_ENABLED, true);

        finishUserShiftWithNoContact(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 3);
    }

    private void finishUserShiftWithNoContact(UserShift userShift) {
        failOrderDeliveryWithNoContact(userShift);
        var returnRoutePoint = userShift.getCurrentRoutePoint();
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
    }

    private void failOrderDeliveryWithNoContact(UserShift userShift) {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT, "Недозвон")
        ));
    }
}
