package ru.yandex.market.tpl.core.domain.push_beru.listeners;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.push_beru.notification.PushBeruNotificationRepository;
import ru.yandex.market.tpl.core.domain.push_beru.notification.model.PushBeruNotification;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand.CheckIn;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;
import ru.yandex.market.tpl.core.external.xiva.PushSendService;
import ru.yandex.market.tpl.core.external.xiva.model.PushBeruEvent;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@Log4j2
@CoreTest
public class PushBeruListenerMultiOrderTest {

    private static final String TRANSIT_ID = "transitId";
    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long UID = 123L;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final PushBeruNotificationRepository pushBeruNotificationRepository;

    private long userShiftId;
    private Shift shift;
    private MultiOrder multiOrder;

    @MockBean
    @Qualifier("sendBeruXivaClient")
    private PushSendService pushSendService;
    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        User user = testUserHelper.findOrCreateUser(UID, LocalDate.now(clock));

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .buyerYandexUid(UID)
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .buyerYandexUid(UID)
                .build());

        multiOrder = MultiOrder.builder()
                .orders(List.of(order1, order2))
                .interval(RelativeTimeInterval.valueOf("10:00-14:00"))
                .build();

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress my_address = new RoutePointAddress("my_address", geoPoint);

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(my_address)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(my_address)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order2, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .routePoint(delivery2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        userShiftId = commandService.createUserShift(createCommand);

        when(pushSendService.send(any())).thenReturn(TRANSIT_ID);

        doReturn(deliveryTime).when(clock).instant();

        configurationServiceAdapter.insertValue(ConfigurationProperties.PUSH_BERU_NOTIFICATION_ENABLED, true);
        configurationServiceAdapter.insertValue(ConfigurationProperties.PUSH_BERU_NOTIFICATION_UIDS, UID);

        commandService.checkin(user, new CheckIn(userShiftId));

    }

    @Test
    void pushAfterPickUpWithSuitableExpectedDeliveryTime() {
        doReturn(clock.instant().minus(5, ChronoUnit.MINUTES)).when(clock).instant();
        testUserHelper.checkinAndFinishPickup(userShiftRepository.findByIdOrThrow(userShiftId));

        var notifications = pushBeruNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        Long presentOrderId = multiOrder.getOrders().stream()
                .sorted(Comparator.comparingLong(Order::getId))
                .map(Order::getId)
                .findFirst()
                .orElseThrow();
        assertThat(notifications.iterator().next()).isEqualToIgnoringGivenFields(
                PushBeruNotification.builder()
                        .event(PushBeruEvent.COURIER_GO_TO_CLIENT)
                        .xivaUserId(UID + "")
                        .title("Отслеживайте заказ на карте")
                        .body("В течение 15 минут он будет у вас")
                        .ttlSec(-1)
                        .orderId(presentOrderId)
                        .shiftId(shift.getId())
                        .payload(PushNotificationPayload.EMPTY)
                        .multiOrderId(multiOrder.getMultiOrderId())
                        .build(), "ttlSec", "payload", "id", "createdAt", "updatedAt"
        );
    }

    @Test
    void pushAfterPickUpWithSuitableExpectedDeliveryTimeWhenOneOrderIsDeliveryFailed() {
        doReturn(clock.instant().minus(5, ChronoUnit.MINUTES)).when(clock).instant();

        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        OrderDeliveryTask taskForFail = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
        Order orderToProcess = multiOrder.getOrders().stream()
                .filter(o -> !o.getId().equals(taskForFail.getOrderId()))
                .findFirst()
                .orElseThrow();

        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShiftId, taskForFail.getRoutePoint().getId(), taskForFail.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED, null,
                                null, Source.DELIVERY)
                ));

        testUserHelper.checkinAndFinishPickup(userShiftRepository.findByIdOrThrow(userShiftId));

        var notifications = pushBeruNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        var notification = notifications.iterator().next();
        assertThat(orderToProcess.getId()).isEqualTo(notification.getOrderId());
        assertThat(orderToProcess.getId() + "").isEqualTo(notification.getMultiOrderId());
    }
}
