package ru.yandex.market.notifier.test.integration;

import java.time.Clock;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.NotificationStatus;

import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderStatusUpdatePendingToCancelledPushTest extends AbstractServicesTestBase {

    @Value("${market.notifier.pushapi.order.status.pending_to_cancelled.retry.count:3}")
    private int pendingToCancelledRetryCount;

    @BeforeEach
    public void init(){
        mockFactory.mockPushApiWithBadRequestRespOnOrderStatus(pushClient);
    }

    @Test
    public void shouldRetryOrderStatusFromPendingToCancelledLimitedTimes() {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.PENDING,
                OrderStatus.CANCELLED,
                OrderSubstatus.PENDING_CANCELLED,
                ClientInfo.SYSTEM,
                order -> {
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                    order.setFulfilment(false);
                }
        );
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);

        for (int retry = 1; retry < pendingToCancelledRetryCount; retry++) {
            deliverNotificationAndCheckRetryCountAndStatus(retry, NotificationStatus.FAILED);
        }
        // достижение порога ретраев
        deliverNotificationAndCheckRetryCountAndStatus(pendingToCancelledRetryCount, NotificationStatus.DELETED);
    }

    private void deliverNotificationAndCheckRetryCountAndStatus(int retryCount, NotificationStatus status) {
        //симулируем, что прошло 10 минут, иначе ретрай посыла нотификаций будет запрещен
        setFixedTime(Clock.systemDefaultZone().instant()
                .plus(retryCount*10, ChronoUnit.MINUTES), ZoneId.systemDefault());

        eventTestUtils.deliverNotifications();

        DeliveryChannel deliveryChannel = eventTestUtils.getSingleNotification(ChannelType.PUSH).getDeliveryChannels()
                .stream().findFirst().orElseThrow();
        assertEquals(retryCount, deliveryChannel.getRetryCount());
        assertEquals(status, deliveryChannel.getStatus());
    }
}
