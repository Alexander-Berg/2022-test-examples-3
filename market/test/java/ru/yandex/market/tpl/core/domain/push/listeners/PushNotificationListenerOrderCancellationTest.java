package ru.yandex.market.tpl.core.domain.push.listeners;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.push.notification.PushNotificationRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotification;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.xiva.XivaClient;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class PushNotificationListenerOrderCancellationTest {

    private static final String TRANSIT_ID = "myTransitId";

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final PushNotificationRepository pushNotificationRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService userShiftCommandService;


    private OrderDeliveryTask task1;
    private OrderDeliveryTask task2;
    private OrderDeliveryTask task3;
    private User user;
    private long userShiftId;
    private long routePointId;

    @MockBean
    private XivaClient xivaClient;
    @MockBean
    private Clock clock;

    @BeforeEach
    void initOrderDeliveryTask() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        user = testUserHelper.findOrCreateUser(1L, LocalDate.now(clock));
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        task1 = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        task2 = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        task3 = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        testUserHelper.checkinAndFinishPickup(userShiftRepository.findByIdOrThrow(userShiftId));
        doReturn(TRANSIT_ID).when(xivaClient).send(any());
    }

    @Test
    void processFailedEventByCoordinates() {
        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, routePointId, task1.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_COORDINATES, "", null, Source.DELIVERY)
        ));

        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, routePointId, task2.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS, "", null, Source.DELIVERY)
        ));

        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, routePointId, task3.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_COORDINATES, "", null, Source.DELIVERY)
        ));

        List<PushNotification> notifications = pushNotificationRepository.findAll();
        assertThat(notifications.size()).isEqualTo(3);
    }

    @Test
    void processFailedEventWrongAddressByClient() {
        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, routePointId, task1.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT, "", null, Source.DELIVERY)
        ));

        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, routePointId, task2.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS, "", null, Source.DELIVERY)
        ));

        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, routePointId, task3.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT, "", null, Source.DELIVERY)
        ));

        List<PushNotification> notifications = pushNotificationRepository.findAll();
        assertThat(notifications.size()).isEqualTo(3);
    }

}
