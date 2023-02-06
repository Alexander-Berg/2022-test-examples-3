package ru.yandex.market.notifier.test.integration;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.NotificationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;

@Disabled
public class OrderStatusUpdateUnpaidToCancelledPushTest extends AbstractServicesTestBase {
    @SpyBean
    PushApi pushApi;

    @Test
    public void unpaidToCancelSkip500Answer() {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.UNPAID,
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_NOT_PAID,
                ClientInfo.SYSTEM,
                order -> {
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                    order.setFulfilment(false);
                }
        );
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);

        //бросаем исключение, будто получаем ответ с кодом 500
        Mockito.doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(pushApi).orderStatus(
                Mockito.any(Long.class),
                Mockito.any(PushApiOrder.class),
                Mockito.any(Boolean.class),
                Mockito.any(Context.class),
                Mockito.any(ApiSettings.class),
                Mockito.nullable(String.class)
        );

        // если не будет проглатывания ошибок, то статус будет FAILED, а не PROCESSED
        deliverNotification(NotificationStatus.PROCESSED);
    }

    @Test
    public void anyOtherStatusToCancelDoesNotSkip500Answer() {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.PROCESSING,
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_NOT_PAID,
                ClientInfo.SYSTEM,
                order -> {
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                    order.setFulfilment(false);
                }
        );
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);

        //бросаем исключение, будто получаем ответ с кодом 500
        Mockito.doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(pushApi).orderStatus(
                Mockito.any(Long.class),
                Mockito.any(PushApiOrder.class),
                Mockito.any(Boolean.class),
                Mockito.any(Context.class),
                Mockito.any(ApiSettings.class),
                Mockito.nullable(String.class)
        );

        deliverNotification(NotificationStatus.FAILED);
    }

    private void deliverNotification(NotificationStatus status) {
        eventTestUtils.deliverNotifications();

        DeliveryChannel deliveryChannel = eventTestUtils.getSingleNotification(ChannelType.PUSH).getDeliveryChannels()
                .stream().findFirst().orElseThrow();

        assertEquals(status, deliveryChannel.getStatus());
    }
}
