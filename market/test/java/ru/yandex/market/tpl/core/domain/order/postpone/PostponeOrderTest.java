package ru.yandex.market.tpl.core.domain.order.postpone;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.PostponedOrderDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType;
import ru.yandex.market.tpl.api.model.task.MultiOrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.sms.SmsClient;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderService;
import ru.yandex.market.tpl.core.service.order.PostponeOrderService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SEND_SMS_WHEN_POSTPONE_ORDER_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PostponeOrderTest extends TplAbstractTest {
    private static final String EXTERNAL_ORDER_ID_1 = "231432";
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftQueryService userShiftQueryService;
    private final PostponeOrderService postponeOrderService;
    private final OrderManager orderManager;
    private final OrderRepository orderRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final PartnerReportOrderService partnerReportOrderService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandService userShiftCommandService;
    private final ClientReturnHistoryEventRepository clientReturnHistoryEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;

    private static final Pageable PAGE_REQUEST = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "date"));

    @Autowired
    @Qualifier("yaSmsClient")
    private SmsClient yaSmsClient;

    @Captor
    private ArgumentCaptor<String> smsTextCaptor;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order1;
    private Order order3;
    private CallToRecipientTask callTask;

    @BeforeEach
    void init() {
        transactionTemplate.executeWithoutResult(
                ts -> {
                    Mockito.clearInvocations(yaSmsClient);
                    user = testUserHelper.findOrCreateUser(1L);
                    shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                            sortingCenterService.findSortCenterForDs(239).getId());
                    userShift = userShiftRepository
                            .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
                    configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                            OrderGenerateService.DEFAULT_PHONE);
                    configurationServiceAdapter.insertValue(SEND_SMS_WHEN_POSTPONE_ORDER_ENABLED, true);

                    order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .externalOrderId(EXTERNAL_ORDER_ID_1)
                            .buyerYandexUid(1L)
                            .deliveryDate(LocalDate.now(clock))
                            .deliveryServiceId(239L)
                            .deliveryInterval(LocalTimeInterval.valueOf("09:00-14:00"))
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(GeoPointGenerator.generateLonLat())
                                    .build())
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                            .build());

                    Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .externalOrderId("3245234")
                            .deliveryDate(LocalDate.now(clock))
                            .deliveryServiceId(239L)
                            .deliveryInterval(LocalTimeInterval.valueOf("11:00-18:00"))
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(GeoPointGenerator.generateLonLat())
                                    .build())
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                            .build());

                    order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .externalOrderId("23445322")
                            .deliveryDate(LocalDate.now(clock))
                            .deliveryServiceId(239L)
                            .deliveryInterval(LocalTimeInterval.valueOf("19:00-22:00"))
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(GeoPointGenerator.generateLonLat())
                                    .build())
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                            .build());

                    userShiftReassignManager.assign(userShift, order1);
                    userShiftReassignManager.assign(userShift, order2);
                    userShiftReassignManager.assign(userShift, order3);
                    callTask = userShift.streamCallTasks().findFirst().orElseThrow();
                    testUserHelper.checkinAndFinishPickup(userShift);

                    configurationServiceAdapter.insertValue(
                            ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
                }
        );
    }


    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(yaSmsClient);
    }

    @Test
    @Transactional
    void testPostponeOrderAndDeliverPostponedOrder() {
        List<RoutePoint> routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        assertThat(routePoints).hasSize(5);
        RoutePoint routePointOrderDelivery1 = routePoints.get(1);
        RoutePoint routePointOrderDelivery2 = routePoints.get(2);
        RoutePoint routePointOrderDelivery3 = routePoints.get(3);
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isEqualTo(routePointOrderDelivery1);

        //отложили
        postponeOrderService.postponeMultiOrder(callTask.getId().toString(), Duration.ofHours(6), user);
        //не показываем отложенные задания на экране "Завершенные"
        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, true);
        assertThat(tasksInfo.getTasks()).hasSize(0);

        //проверяем, что доставляем второй по очереди
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isEqualTo(routePointOrderDelivery2);
        testUserHelper.finishDelivery(currentRoutePoint, false);

        //проверяем, что восстановили отложенный и доставляем его
        currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isEqualTo(routePointOrderDelivery1);
        MultiOrderDeliveryTaskDto orderDeliveryTaskDto =
                userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTask.getId().toString());
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(1);
        orderDeliveryTaskDto.getTasks().forEach(task -> assertThat(task.getPostponed()).isNotNull());
        testUserHelper.finishDelivery(currentRoutePoint, false);

        //доставляем последний
        currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isEqualTo(routePointOrderDelivery3);
        testUserHelper.finishDelivery(currentRoutePoint, false);
    }

    @Test
    @DisplayName("Тест, чтобы была возможность отменить отложенный заказ")
    void tryCancelPostponedOrder() {
        RoutePoint routePointOrderDelivery1 = userShift.streamRoutePoints().collect(Collectors.toList()).get(1);

        postponeOrderService.postponeMultiOrder(callTask.getId().toString(), Duration.ofHours(6), user);

        long orderId =
                routePointOrderDelivery1.streamOrderDeliveryTasks().collect(Collectors.toList()).get(0).getOrderId();

        transactionTemplate.executeWithoutResult(
                ts -> {
                    orderManager.cancelOrder(orderRepository.findByIdOrThrow(orderId));
                    userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTask.getId().toString())
                            .getTasks()
                            .forEach(task -> assertThat(task.getPostponed().isActive()).isFalse());
                }
        );
    }

    @Test
    @DisplayName("Тест, чтобы была возможность перенести отложенный заказ")
    void tryReschedulePostponedOrder() {
        RoutePoint routePointOrderDelivery1 = userShift.streamRoutePoints().collect(Collectors.toList()).get(1);

        postponeOrderService.postponeMultiOrder(callTask.getId().toString(), Duration.ofHours(6), user);

        long orderId =
                routePointOrderDelivery1.streamOrderDeliveryTasks().collect(Collectors.toList()).get(0).getOrderId();
        orderManager.rescheduleOrder(
                orderRepository.findByIdOrThrow(orderId),
                new Interval(tomorrowAtHour(14, clock), tomorrowAtHour(16, clock)),
                Source.CLIENT
        );

        userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTask.getId().toString())
                .getTasks()
                .forEach(task -> assertThat(task.getPostponed().isActive()).isFalse());
    }

    @DisplayName("Тест, чтобы была возможность возобновить отложенный заказ, если звонки отключены")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void tryReopenPostponedOrder_WhenDoNotCallIs(boolean isDoNotCallEnabled) {
        configurationServiceAdapter.insertValue(DO_NOT_CALL_ENABLED, isDoNotCallEnabled);
        OrderDeliveryTask orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();

        postponeOrderService.postponeMultiOrder(callTask.getId().toString(), Duration.ofHours(6), user);

        Order order = orderRepository.findByIdOrThrow(orderDeliveryTask.getOrderId());
        orderManager.reopenTask(order.getExternalOrderId());

        userShiftQueryService.getMultiOrderDeliveryTaskDto(user, callTask.getId().toString())
                .getTasks()
                .forEach(task -> {
                    assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
                    PostponedOrderDto postponed = Objects.requireNonNull(task.getPostponed());
                    assertThat(postponed.isActive()).isTrue();
                });
    }

    @Test
    @DisplayName("Отправляем смс, когда отложили заказ")
    void sendSms_WhenPostponeOrder() {
        postponeOrderService.postponeMultiOrder(callTask.getId().toString(), Duration.ofHours(6), user);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);
        Mockito.verify(yaSmsClient, Mockito.atLeastOnce()).send(Mockito.anyString(), smsTextCaptor.capture());

        boolean containsPostponeSms = smsTextCaptor.getAllValues().stream()
                .anyMatch(messageText -> messageText.contains("Время доставки заказа"));
        assertThat(containsPostponeSms).isTrue();
    }

    @Test
    @DisplayName("Корректно показывать в партнерке отложенный заказ")
    void showPartnerOrder_WhenPostpone() {
        postponeOrderService.postponeMultiOrder(callTask.getId().toString(), Duration.ofHours(6), user);

        assertThatCode(() -> partnerReportOrderService.findOrder(EXTERNAL_ORDER_ID_1))
                .doesNotThrowAnyException();
    }

    @Test
    @Transactional
    void testPostponeClientReturnAndRevertPostpone() {
        var crRoutePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = crRoutePoint.getId();
        Instant deliveryTime = Instant.now(clock);

        // костыль. Т.к. в тестовых данных создаются возвраты и заказа с нулевого счетчика,
        // то при создании заказов и возвратов в 1 тесте будет пересечение по ид.
        // В данном тесте это выливается в то что у двух тасок на доставку создается одинаковый мульт
        // (у одной от заказа, у другой от возврата)
        long biggestOrderId =
                userShift.streamOrderDeliveryTasks()
                        .map(OrderDeliveryTask::getOrderIds)
                        .flatMap(Collection::stream)
                        .max(Comparator.naturalOrder()).get();

        ClientReturn clientReturn = null;
        biggestOrderId++;
        for (int i = 0; i < biggestOrderId; i++) {
            clientReturn = clientReturnGenerator.generateReturnFromClient();
        }

        var tod = userShiftCommandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        List<RoutePoint> routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        RoutePoint routePointOrderDelivery1 = routePoints.get(1);
        userShiftCommandService.switchOpenRoutePoint(userShift.getUser(),
                new UserShiftCommand.SwitchOpenRoutePoint(userShift.getId(),
                        routePointId));
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint.getId()).isEqualTo(crRoutePoint.getId());


        //отложили
        var postponeDuration = Duration.ofHours(6);
        postponeOrderService.postponeMultiOrder(tod.getParentId(), postponeDuration, user);

        //не показываем отложенные задания на экране "Завершенные"
        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, true);
        assertThat(tasksInfo.getTasks()).hasSize(0);

        //проверяем, что заполняется поле postponed
        RemainingOrderDeliveryTasksDto remainingTasksDto = userShiftQueryService.getRemainingTasksInfo(user);
        var postponedTasks =
                remainingTasksDto.getOrders().stream().map(OrderSummaryDto::getPostponed).filter(Objects::nonNull).collect(Collectors.toList());
        assertThat(postponedTasks).hasSize(1);
        assertThat(postponedTasks.get(0).getDuration()).isEqualTo(postponeDuration);

        //проверяем, что доставляем второй по очереди
        currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isEqualTo(routePointOrderDelivery1);
        testUserHelper.finishDelivery(currentRoutePoint, false);

        postponeOrderService.revertPostponeMultiOrder(tod.getParentId(), user);
        //проверяем, что восстановили отложенный и доставляем его
        currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isEqualTo(crRoutePoint);
        MultiOrderDeliveryTaskDto orderDeliveryTaskDto =
                userShiftQueryService.getMultiOrderDeliveryTaskDto(user, tod.getParentId());
        assertThat(orderDeliveryTaskDto.getTasks()).hasSize(1);
        orderDeliveryTaskDto.getTasks().forEach(task -> assertThat(task.getPostponed()).isNotNull());
        testUserHelper.finishDelivery(currentRoutePoint, false);

        var events = clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), PAGE_REQUEST);
        var postponedEvent = events.stream()
                .filter(event -> event.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_POSTPONED)
                .findAny();
        var postponeRevertedEvent = events.stream()
                .filter(e -> e.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_REVERT_POSTPONED)
                .findAny();

        assertThat(postponedEvent).isPresent();
        assertThat(postponedEvent.get().getSource()).isEqualTo(Source.COURIER);
        assertThat(postponedEvent.get().getContext()).contains(postponeDuration.toString(), user.getName());

        assertThat(postponeRevertedEvent).isPresent();
        assertThat(postponeRevertedEvent.get().getSource()).isEqualTo(Source.COURIER);
        assertThat(postponeRevertedEvent.get().getContext()).contains(user.getName());
    }

}
