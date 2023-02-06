package ru.yandex.market.tpl.core.domain.push.notification;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.notification.PushRedirect;
import ru.yandex.market.tpl.api.model.notification.PushRedirectType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotification;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.external.xiva.model.PushEvent;
import ru.yandex.market.tpl.core.external.xiva.model.PushSendRequest;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PushNotificationRepositoryTest {

    private static final long UID = 123L;

    private final PushNotificationRepository pushNotificationRepository;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;

    @Test
    void save() {
        LocalDate date = LocalDate.now();
        User user = testUserHelper.findOrCreateUser(UID, date);
        Shift shift = testUserHelper.findOrCreateOpenShift(date);
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId);
        OrderDeliveryTask task = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        PushNotification pushNotification = PushNotification.fromPushSendRequest(
                new PushSendRequest(
                        String.valueOf(user.getUid()), PushEvent.SYSTEM, "Заголовок", "Тело", 10,
                        new PushNotificationPayload(
                                new PushRedirect(PushRedirectType.ROUTE_POINT_PAGE, routePoint.getId(), task.getId(), null)
                        ), null
                ),
                userShiftId, task.getId(), user.getYaProId()
        );
        assertThat(pushNotificationRepository.save(pushNotification)).isEqualTo(pushNotification);
    }

}
