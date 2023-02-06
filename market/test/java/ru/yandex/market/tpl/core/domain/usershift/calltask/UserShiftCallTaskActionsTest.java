package ru.yandex.market.tpl.core.domain.usershift.calltask;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.call.CallTaskDto;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tvm.service.ServiceTicketRequest;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallTaskLogEntry;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.CALL_REQUIRED;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.DO_NOT_CALL;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftCallTaskActionsTest {

    private static final String COURIER_NOTES = "test";
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftRepository repository;
    private final UserPropertyService userPropertyService;
    private final Clock clock;
    private final TrackingService trackingService;
    private final TrackingRepository trackingRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    @Value("${tpl.callInMinutesBeforeDelivery:60}")
    private int callInMinutesBeforeDelivery;
    private User user;
    private Shift shift;
    private UserShift userShift;
    private RoutePoint routePoint;
    private Order order;
    private ClientReturn clientReturn;
    private ServiceTicketRequest serviceTicket;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);

        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        clientReturn = clientReturnGenerator.generateReturnFromClient();

        clientReturn.getLogisticRequestPointFrom().setOriginalLatitude(order.getDelivery().getDeliveryAddress().getLatitude());
        clientReturn.getLogisticRequestPointFrom().setOriginalLongitude(order.getDelivery().getDeliveryAddress().getLongitude());
        clientReturn.getClient().getClientData().setPhone(order.getDelivery().getRecipientPhone());
        clientReturnRepository.save(clientReturn);

        userShift = createUserShift();
        commandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);
        routePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();
        serviceTicket = new ServiceTicketRequest();
    }

    @Test
    public void shouldIncrementAttemptCountAtSuccessAttemptCall() {
        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(routePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        assertThatAddCallLogEntry(callTask);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(routePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.DELIVERY);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
    }

    @Test
    public void shouldIncrementAttemptCountAtSuccessAttemptCallTwice() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        callTask.getId(),
                        COURIER_NOTES));


        assertThat(callTask.getAttemptCount()).isEqualTo(2);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    public void shouldReopenCallTaskAtFailAttemptCall() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        commandService.incrementAttemptCallCount(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(2);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    public void shouldIncrementAttemptCountAtFailAttemptCall() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.incrementAttemptCallCount(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);

        assertThatAddCallLogEntry(callTask);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        CallTaskDto callTaskDto = userShiftQueryService.getCallTask(user, callTask.getId());
        assertThat(callTaskDto.getCourierComment()).isEqualTo(COURIER_NOTES);
    }

    @Test
    public void shouldUpdateToDoNotCallAndBack() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.incrementAttemptCallCount(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);
        assertThatAddCallLogEntry(callTask);
        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        CallTaskDto callTaskDto = userShiftQueryService.getCallTask(user, callTask.getId());
        assertThat(callTaskDto.getCourierComment()).isEqualTo(COURIER_NOTES);

        Tracking tracking = trackingRepository.findByOrderIdOrThrow(order.getId());
        TrackingDto trackingDto = trackingService.updateCallRequirement(tracking.getId(), DO_NOT_CALL, serviceTicket);
        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(DO_NOT_CALL);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL);

        trackingDto = trackingService.updateCallRequirement(tracking.getId(), CALL_REQUIRED, serviceTicket);
        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(CALL_REQUIRED);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);

        List<OrderHistoryEvent> clientEvents = orderHistoryEventRepository.findAllByOrderId(order.getId()).stream()
                .filter(e -> e.getType() == OrderEventType.CLIENT_MESSAGE)
                .collect(Collectors.toList());

        assertThat(clientEvents).hasSize(2)
                .extracting(OrderHistoryEvent::getContext)
                .containsOnly("Клиент изменил необходимость звонка с: Не задано на: Клиент просил не звонить",
                        "Клиент изменил необходимость звонка с: Клиент просил не звонить на: Звонок нужен");

        assertThat(clientEvents).hasSize(2)
                .extracting(OrderHistoryEvent::getDifference)
                .extracting("deliveryTaskId", "callTaskId")
                .containsOnly(Tuple.tuple(tracking.getOrderDeliveryTask().getId(), callTask.getId()));
    }

    @Test
    public void shouldUpdateToDoNotCall() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        assertThat(callTask.getAttemptCount()).isEqualTo(0);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);
        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(routePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        Tracking tracking = trackingRepository.findByOrderIdOrThrow(order.getId());
        TrackingDto trackingDto = trackingService.updateCallRequirement(tracking.getId(), DO_NOT_CALL, serviceTicket);

        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(routePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.DELIVERY);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(DO_NOT_CALL);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL);

        trackingDto = trackingService.updateCallRequirement(tracking.getId(), CALL_REQUIRED, serviceTicket);

        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(CALL_REQUIRED);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.DELIVERY);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
    }

    @Test
    void shouldEditCourierNotes() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        String multiOrderId = String.valueOf(callTask.getId());

        CallTaskDto callTaskBeforeAction = userShiftQueryService.getCallTask(user, callTask.getId());
        assertThat(callTaskBeforeAction.getCourierComment()).isEqualTo("");

        commandService.editCourierNotes(
                user,
                new UserShiftCommand.EditCourierNotes(
                        userShift.getId(),
                        multiOrderId,
                        COURIER_NOTES));

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        CallTaskDto callTaskDto = userShiftQueryService.getCallTask(user, callTask.getId());
        assertThat(callTaskDto.getCourierComment()).isEqualTo(COURIER_NOTES);
    }

    @Test
    public void shouldCancelOrderAfterCall() {
        var rp = userShift.streamDeliveryRoutePoints().collect(Collectors.toList()).get(0);
        var clientReturnDeliveryTask = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), rp.getId(), clientReturn.getId(), order.getDelivery().getDeliveryIntervalFrom()
                )
        );

        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.cancelOrdersAfterCall(
                user,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                                COURIER_NOTES))
        );

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        OrderDeliveryTask deliveryTask = callTask.getOrderDeliveryTasks().iterator().next();

        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(clientReturnDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = deliveryTask.getFailReason();
        OrderDeliveryFailReason crFailReason = clientReturnDeliveryTask.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(crFailReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED);
        assertThat(crFailReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED);
        assertThat(failReason.getComment()).isEqualTo(COURIER_NOTES);
        assertThat(crFailReason.getComment()).isEqualTo(COURIER_NOTES);
        assertThat(deliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(clientReturnDeliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        assertThatAddCallLogEntry(callTask);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    public void shouldCancelOrderAfterCallAfterSuccessAttempt() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        commandService.cancelOrdersAfterCall(
                user,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        callTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                                COURIER_NOTES))
        );

        assertThat(callTask.getAttemptCount()).isEqualTo(2);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        OrderDeliveryTask deliveryTask = callTask.getOrderDeliveryTasks().iterator().next();

        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = deliveryTask.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.CANCEL_ORDER);
        assertThat(failReason.getComment()).isEqualTo(COURIER_NOTES);
        assertThat(deliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.DO_NOT_CALL_ENABLED, true);
        var tracking = trackingRepository.findByOrderIdOrThrow(order.getId());
        var trackingDto = trackingService.updateCallRequirement(tracking.getId(), DO_NOT_CALL, serviceTicket);

        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(CALL_REQUIRED);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);
        assertThat(userShift.getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);
        assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
    }

    @Test
    public void shouldRescheduleAfterCallToFutureDay() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        commandService.rescheduleDeliveryAfterCall(
                user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 20))),
                        userShift.getZoneId()
                ), intervals);

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        OrderDeliveryTask deliveryTask = callTask.getOrderDeliveryTasks().iterator().next();

        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = deliveryTask.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
        assertThat(deliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        assertThatAddCallLogEntry(callTask);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);
    }

    @Test
    public void shouldRescheduleAfterCallToFutureDayAfter() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        COURIER_NOTES));

        assertThat(callTask.getAttemptCount()).isEqualTo(1);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        commandService.rescheduleDeliveryAfterCall(
                user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        callTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 20))),
                        userShift.getZoneId()
                ), intervals);

        assertThat(callTask.getAttemptCount()).isEqualTo(2);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);

        OrderDeliveryTask deliveryTask = callTask.getOrderDeliveryTasks().iterator().next();

        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = deliveryTask.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
        assertThat(deliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);
    }

    @Test
    void shouldRescheduleAfterCallToCurrentDay() {
        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        Instant from = todayAtHour(18, clock);
        Instant to = todayAtHour(20, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        commandService.rescheduleDeliveryAfterCall(
                user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 20))),
                        userShift.getZoneId()
                ), intervals);

        assertThat(callTask.getAttemptCount()).isEqualTo(1);

        OrderDeliveryTask deliveryTask = callTask.getOrderDeliveryTasks().iterator().next();

        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(deliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);


        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.RECALL_REQUIRED);
        Instant expectedCallTime = deliveryTask.getExpectedDeliveryTime().minus(callInMinutesBeforeDelivery,
                ChronoUnit.MINUTES);
        assertThat(callTask.getExpectedCallTime()).isEqualTo(expectedCallTime);

        assertThatAddCallLogEntry(callTask);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);
    }

    @Test
    void shouldChangeFromRecallRequiredToDoNotCall() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        Instant from = todayAtHour(18, clock);
        Instant to = todayAtHour(20, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        commandService.rescheduleDeliveryAfterCall(
                user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 20))),
                        userShift.getZoneId()
                ), intervals);

        assertThat(callTask.getAttemptCount()).isEqualTo(1);

        OrderDeliveryTask deliveryTask = callTask.getOrderDeliveryTasks().iterator().next();

        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(deliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);


        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.RECALL_REQUIRED);
        Instant expectedCallTime = deliveryTask.getExpectedDeliveryTime().minus(callInMinutesBeforeDelivery,
                ChronoUnit.MINUTES);
        assertThat(callTask.getExpectedCallTime()).isEqualTo(expectedCallTime);

        assertThatAddCallLogEntry(callTask);

        assertThat(order.getDelivery().getCourierNotes()).isEqualTo(COURIER_NOTES);

        Tracking tracking = trackingRepository.findByOrderIdOrThrow(order.getId());
        TrackingDto trackingDto = trackingService.updateCallRequirement(tracking.getId(), DO_NOT_CALL, serviceTicket);
        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(DO_NOT_CALL);
        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL);

        List<OrderHistoryEvent> clientEvents = orderHistoryEventRepository.findAllByOrderId(order.getId()).stream()
                .filter(e -> e.getType() == OrderEventType.CLIENT_MESSAGE)
                .collect(Collectors.toList());

        assertThat(clientEvents).hasSize(1)
                .extracting(OrderHistoryEvent::getContext)
                .containsOnly("Клиент изменил необходимость звонка с: Не задано на: Клиент просил не звонить");

        assertThat(clientEvents).hasSize(1)
                .extracting(OrderHistoryEvent::getDifference)
                .extracting("deliveryTaskId", "callTaskId")
                .containsOnly(Tuple.tuple(deliveryTask.getId(), callTask.getId()));
    }

    private void assertThatAddCallLogEntry(CallToRecipientTask callTask) {
        List<CallTaskLogEntry> callTaskLog = callTask.getCallTaskLog();
        assertThat(callTaskLog).isNotNull();
        assertThat(callTaskLog).hasSize(1);
        CallTaskLogEntry logEntry = callTaskLog.iterator().next();
        assertThat(logEntry.getRoutePoint()).isEqualTo(routePoint);
        assertThat(logEntry.getCallTaskStatus()).isEqualTo(callTask.getStatus());
    }


    private UserShift createUserShift() {
        var deliveryTask = helper.taskUnpaid("addr1", 12, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryTask)
                .active(true)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(userShiftId).orElseThrow();

        return userShift;
    }

}
