package ru.yandex.market.tpl.api.facade;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.events.history.ClientReturnHistoryEvent;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ClientReturnReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.service.notification.NotificationService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType.CLIENT_RETURN_RESCHEDULED;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.GET_MULTI_INTERVAL_BY_ALL_ORDERS;

@RequiredArgsConstructor
class CallTaskFacadeTest extends TplAbstractTest {


    private static final long UID = 1L;
    private static final String INTERVAL_9_TO_18 = "09:00-18:00";
    private static final String PHONE = OrderGenerateService.DEFAULT_PHONE;


    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long YANDEX_BUYER_ID = 1L;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandService commandService;
    private final NotificationService notificationService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final ClientReturnHistoryEventRepository clientReturnHistoryEventRepository;

    private final Clock clock;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;

    private LocalDate now;
    private Order order;
    private User user;
    private NewDeliveryRoutePointData delivery;
    private NewDeliveryRoutePointData clientReturnDelivery;
    private Shift shift;
    private AddressGenerator.AddressGenerateParam addressGenerateParam;
    private GeoPoint geoPoint;
    private ClientReturn clientReturn;
    private RoutePointAddress myAddress;
    private Instant clientReturnDeliveryTime;
    private RoutePoint routePoint;
    private UserShift userShift;


    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0)));
        now = LocalDate.now();
        geoPoint = GeoPointGenerator.generateLonLat();
        shift = testUserHelper.findOrCreateOpenShift(now);
        user = testUserHelper.findOrCreateUser(UID);

        addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(geoPoint)
                .street("Колотушкина")
                .house("1")
                .build();

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_9_TO_18))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .recipientPhone(PHONE)
                .buyerYandexUid(YANDEX_BUYER_ID)
                .build());
        clientReturn =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(PHONE)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0)))
                        .build());

        clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();
        myAddress = new RoutePointAddress("my_address", geoPoint);

        delivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order, false, false)
                .build();

        clientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturn.getId()).build())
                .build();
    }

    @Test
    @DisplayName("Проверка, что если в кол таске есть клиентский возврат, то он переносится вместе с заказом")
    void clientReturnRescheduledByCallTask() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER, true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery)
                .routePoint(clientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        var callTasks = transactionTemplate.execute(cmd -> {
                    List<CallToRecipientTask> callTasksTmp =
                            userShiftRepository.findByIdOrThrow(userShiftId).streamCallTasks().collect(Collectors.toList());
                    assertThat(callTasksTmp).hasSize(1);
                    userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    routePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();
                    return callTasksTmp;
                }
        );
        var callTask = callTasks.get(0);
        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        userShiftCommandService.rescheduleDeliveryAfterCall(
                user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        routePoint.getId(),
                        callTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY, "COURIER_NOTES"),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 20))),
                        userShift.getZoneId()
                ), intervals);

        transactionTemplate.executeWithoutResult(cmd -> {
            var us = userShiftRepository.findByIdOrThrow(userShiftId);
            OrderDeliveryTask clientReturnTask =
                    us.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();
            OrderDeliveryTask deliveryTask =
                    us.streamOrderDeliveryTasks().remove(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();


            var historyEvents = clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(),
                    Pageable.unpaged());
            Optional<ClientReturnHistoryEvent> clientReturnRescheduleO =
                    historyEvents.stream().filter(e -> e.getType() == CLIENT_RETURN_RESCHEDULED).findFirst();

            assertThat(clientReturnRescheduleO).isPresent();
            assertThat(clientReturnTask.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
            assertThat(deliveryTask.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);

        });
    }
}
