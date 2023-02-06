package ru.yandex.market.tpl.core.domain.order.postpone;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.MultiOrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingCancelOrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingOrderCancelReason;
import ru.yandex.market.tpl.api.model.tracking.TrackingRescheduleDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.sms.SmsClient;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.order.PostponeOrderService;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.RECALL_REQUIRED;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class PostponeMultiOrderTest {
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftQueryService userShiftQueryService;
    private final PostponeOrderService postponeOrderService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TrackingRepository trackingRepository;
    private final TrackingService trackingService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderHistoryEventRepository orderHistoryEventRepository;

    @Autowired
    @Qualifier("yaSmsClient")
    private SmsClient yaSmsClient;

    @Captor
    private ArgumentCaptor<String> smsTextCaptor;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order1;
    private Order order2;
    private Order multiOrder1;
    private Order multiOrder2;
    private List<CallToRecipientTask> callTasks;
    RoutePoint routePointOrderDelivery1;
    RoutePoint routePointOrderDelivery2;
    String multiOrderId;


    @BeforeEach
    void init() {
        Mockito.clearInvocations(yaSmsClient);
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);
        configurationServiceAdapter.insertValue(ConfigurationProperties.SEND_SMS_WHEN_POSTPONE_ORDER_ENABLED, true);

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .buyerYandexUid(1L)
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321234")
                .buyerYandexUid(1L)
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("5345323")
                .buyerYandexUid(1L)
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .recipientPhone("89295372775")
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("43533432")
                .buyerYandexUid(1L)
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .recipientPhone("89295372774")
                .deliveryInterval(LocalTimeInterval.valueOf("12:00-19:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);
        userShiftReassignManager.assign(userShift, order1);
        userShiftReassignManager.assign(userShift, order2);
        callTasks = userShift.streamCallTasks().collect(Collectors.toList());
        testUserHelper.checkinAndFinishPickup(userShift);

        List<RoutePoint> routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        assertThat(routePoints).hasSize(4);
        routePointOrderDelivery1 = routePoints.get(1);
        routePointOrderDelivery2 = routePoints.get(2);
        multiOrderId = callTasks.get(0).getId().toString();
    }

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(yaSmsClient);
    }

    @Test
    void postponeMultiOrder() {
        //отложили
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(5), user);
        //тут же переотложили заказ на другое время
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(6), user);

        //проверяем, что показываем отложенные задания на экране "Задания на сегодня"
        RemainingOrderDeliveryTasksDto remainingTasksInfo = userShiftQueryService.getRemainingTasksInfo(user);
        assertThat(remainingTasksInfo.getOrders()).hasSize(4);
        remainingTasksInfo.getOrders().stream()
                .filter(orderSummaryDto -> orderSummaryDto.getExternalOrderId().equals(multiOrder1.getExternalOrderId())
                        || orderSummaryDto.getExternalOrderId().equals(multiOrder2.getExternalOrderId()))
                .forEach(orderSummaryDto -> assertThat(orderSummaryDto.getPostponed()).isNotNull());

        MultiOrderDeliveryTaskDto orderDeliveryTaskDto =
                userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTasks.get(1).getId().toString());

        //Проверяем, что 2 таски (мультик) перенеслина другой роутпоинт, а одну не из мультика оставили
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePointOrderDelivery1);
        assertThat(userShift.getCurrentRoutePoint().streamTasks().collect(Collectors.toList())).hasSize(1);
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(1);
        orderDeliveryTaskDto.getTasks().forEach(task -> assertThat(task.getPostponed()).isNull());
        //+1 роут поинт
        assertThat(userShift.streamRoutePoints().collect(Collectors.toList())).hasSize(5);

        //Завершили задание на доставку из первого роутпоинта
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePointOrderDelivery2);

        //Завершили обычное задание на доставку
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);

        //завершили доставку мультика в другое время
        orderDeliveryTaskDto = userShiftQueryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(2);
        assertThat(userShift.getCurrentRoutePoint().streamTasks().collect(Collectors.toList())).hasSize(2);
        orderDeliveryTaskDto.getTasks().forEach(task -> {
            assertThat(task.getPostponed()).isNotNull();
            assertThat(task.getPostponed().getDuration()).isEqualTo(Duration.ofHours(6));
        });
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
    }

    @Test
    void revertPostponeMultiOrder_WhenArriveToCurrentRoutePoint() {
        //отложили
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(5), user);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePointOrderDelivery1);
        testUserHelper.arriveAtRoutePoint(routePointOrderDelivery1);

        //восстановили и проверяем, что сейчас активен тот же роут поинт
        MultiOrderDeliveryTaskDto multiOrderDeliveryTaskDto =
                postponeOrderService.revertPostponeMultiOrder(multiOrderId, user);
        assertThat(multiOrderDeliveryTaskDto.getNotifications()).hasSize(1);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePointOrderDelivery1);

        //завершаем доставку
        OrderDeliveryTask orderDeliveryTask = routePointOrderDelivery1.streamOrderDeliveryTasks().findFirst().get();
        userShiftCommandService.printCheque(user,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(),
                        routePointOrderDelivery1.getId(),
                        orderDeliveryTask.getId(),
                        userShiftCommandDataHelper.getChequeDto(OrderPaymentType.PREPAID),
                        orderDeliveryTask.getExpectedDeliveryTime(),
                        false,
                        null,
                        Optional.empty()
                ));
        testUserHelper.finishCallTasksAtRoutePoint(routePointOrderDelivery1);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);

        //переключили роут поинт на "отложенный"
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(routePointOrderDelivery1);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(userShift.getCurrentRoutePoint().streamOrderDeliveryTasks().collect(Collectors.toList())).hasSize(2);
        MultiOrderDeliveryTaskDto orderDeliveryTaskDto =
                userShiftQueryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(2);
        orderDeliveryTaskDto.getTasks().forEach(task -> {
            assertThat(task.getFailReason()).isNull();
            assertThat(task.getPostponed().isActive()).isFalse();
        });
    }

    @Test
    void revertPostponeMultiOrderWhenArriveToCurrentRoutePointImmediateSwitch() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.ORDER_REVERT_POSTPONE_IMMEDIATE_ENABLED, true);
        //отложили
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(5), user);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePointOrderDelivery1);
        testUserHelper.arriveAtRoutePoint(routePointOrderDelivery1);

        //восстановили и проверяем, что сейчас активен другой роут поинт
        postponeOrderService.revertPostponeMultiOrder(multiOrderId, user);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(routePointOrderDelivery1);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        MultiOrderDeliveryTaskDto orderDeliveryTaskDto =
                userShiftQueryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(2);
        orderDeliveryTaskDto.getTasks().forEach(task -> assertThat(task.getFailReason()).isNull());
    }

    @Test
    void postponeMultiOrderAfterCall() {
        //when
        CallToRecipientTask callTask = callTasks.get(0);
        Instant expectedCallTime = callTask.getExpectedCallTime();
        OrderDeliveryTask orderDeliveryTask = callTask.getOrderDeliveryTasks().get(0);

        //action
        postponeOrderService.postponeMultiOrderAfterCall(
                callTask.getId(),
                orderDeliveryTask.getRoutePoint().getUserShift().getId(),
                orderDeliveryTask.getRoutePoint().getId(),
                Duration.ofHours(5),
                user
        );

        //then
        assertThat(callTask.getStatus()).isEqualTo(RECALL_REQUIRED);
        assertThat(callTask.getExpectedCallTime()).isEqualTo(expectedCallTime.plus(Duration.ofHours(5)));
    }

    @Test
    void revertPostponeMultiOrder_WhenNotArriveToCurrentRoutePoint() {
        //отложили
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(5), user);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(routePointOrderDelivery1);

        //восстановили и проверяем, что сейчас активен другой роут поинт
        postponeOrderService.revertPostponeMultiOrder(multiOrderId, user);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(routePointOrderDelivery1);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        MultiOrderDeliveryTaskDto orderDeliveryTaskDto =
                userShiftQueryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(2);
        orderDeliveryTaskDto.getTasks().forEach(task -> assertThat(task.getFailReason()).isNull());
    }

    @Test
    @DisplayName("Тест, чтобы была возможность перенести отложенный мультизаказ")
    void tryReschedulePostponedMultiOrder() {
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(6), user);
        long orderId = callTasks.get(0).getOrderDeliveryTasks().get(0).getOrderId();

        Tracking tracking = trackingRepository.findByOrderId(orderId).get();
        trackingService.rescheduleOrder(
                tracking.getId(),
                new TrackingRescheduleDto(tomorrowAtHour(14, clock), tomorrowAtHour(18, clock)),
                null);
        callTasks.get(0).getOrderDeliveryTasks().forEach((task -> {
            assertThat(task.isFailed()).isTrue();
            assertThat(task.isPostponedSubtask()).isFalse();
        }));

        userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTasks.get(0).getId().toString())
                .getTasks()
                .forEach(task -> assertThat(task.getPostponed().isActive()).isFalse());
    }

    @Test
    @DisplayName("Тест, чтобы была возможность отменить отложенный мультизаказ")
    void tryCancelPostponedMultiOrder() {
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(6), user);
        long orderId = callTasks.get(0).getOrderDeliveryTasks().get(0).getOrderId();

        Tracking tracking = trackingRepository.findByOrderId(orderId).get();
        trackingService.cancelOrder(
                tracking.getId(),
                new TrackingCancelOrderDto(TrackingOrderCancelReason.CHANGED_MIND, ""),
                null);
        callTasks.get(0).getOrderDeliveryTasks().forEach((task -> {
            assertThat(task.isFailed()).isTrue();
            assertThat(task.isPostponedSubtask()).isFalse();
        }));

        userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTasks.get(0).getId().toString())
                .getTasks()
                .forEach(task -> assertThat(task.getPostponed().isActive()).isFalse());
    }

    @Test
    @DisplayName("Отправляем смс, когда отложили мультизаказ")
    void sendSms_WhenPostponeMultiOrder() {
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofHours(6), user);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);
        Mockito.verify(yaSmsClient, Mockito.atLeastOnce()).send(Mockito.anyString(), smsTextCaptor.capture());

        boolean containsPostponeSms = smsTextCaptor.getAllValues().stream()
                .anyMatch(messageText -> messageText.contains("Время доставки заказов"));
        assertThat(containsPostponeSms).isTrue();
    }

    @Test
    @DisplayName("Показывать в истории заказа переносы в течение дня")
    void showPostponedEventsInOrderHistory() {
        postponeOrderService.postponeMultiOrder(multiOrderId, Duration.ofMinutes(390), user);

        //В историю записали, что заказ отложен
        callTasks.get(0).getOrderDeliveryTasks().forEach((task -> {
            List<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository.findAllByOrderId(task.getOrderId())
                    .stream()
                    .filter(orderHistoryEvent -> orderHistoryEvent.getType() == OrderEventType.ORDER_POSTPONED)
                    .collect(Collectors.toList());

            assertThat(orderHistoryEvents).hasSize(1);
            orderHistoryEvents.forEach(
                    historyEvent -> assertThat(historyEvent.getContext()).isEqualTo("Заказ перенесён на 6 ч. 30 мин.")
            );
        }));

        postponeOrderService.revertPostponeMultiOrder(multiOrderId, user);

        //В историю записали, что заказ восстановили
        callTasks.get(0).getOrderDeliveryTasks().forEach(task -> {
            List<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository.findAllByOrderId(task.getOrderId())
                    .stream()
                    .filter(orderHistoryEvent -> orderHistoryEvent.getType() == OrderEventType.ORDER_REVERT_POSTPONED)
                    .collect(Collectors.toList());

            assertThat(orderHistoryEvents).hasSize(1);
        });

    }

}
