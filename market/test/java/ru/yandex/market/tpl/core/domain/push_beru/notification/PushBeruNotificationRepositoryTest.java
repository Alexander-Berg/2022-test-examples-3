package ru.yandex.market.tpl.core.domain.push_beru.notification;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.push_beru.notification.model.PushBeruNotification;
import ru.yandex.market.tpl.core.domain.push_beru.notification.model.PushBeruPayload;
import ru.yandex.market.tpl.core.domain.push_beru.notification.model.PushBeruPayloadType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.xiva.model.PushBeruEvent;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class PushBeruNotificationRepositoryTest {
    private static final long UID = 123L;

    private final PushBeruNotificationRepository pushBeruNotificationRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderRepository orderRepository;

    @MockBean
    private Clock clock;

    @Test
    void testSave() {
        var notification = pushBeruNotification();
        assertThat(save(notification)).isEqualTo(notification);
    }

    @Transactional
    public PushBeruNotification save(PushBeruNotification notification) {
        return pushBeruNotificationRepository.saveAndFlush(notification);
    }

    private PushBeruNotification pushBeruNotification() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        var user = testUserHelper.findOrCreateUser(UID, LocalDate.now(clock));
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        var task = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .build());
        var order = orderRepository.getOne(task.getOrderId());

        var notification = PushBeruNotification.builder()
                .orderId(order.getId())
                .shiftId(shift.getId())
                .xivaUserId(UID + "")
                .event(PushBeruEvent.COURIER_GO_TO_CLIENT)
                .body("some text")
                .ttlSec(1)
                .payload(new PushNotificationPayload(
                        new PushBeruPayload("deepLink", PushBeruPayloadType.COURIER_GO_TO_CLIENT, "message"))
                )
                .multiOrderId(order.getId() + "")
                .build();
        return save(notification);
    }

}
