package ru.yandex.market.notifier.test.integration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.hamcrest.MockitoHamcrest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.util.NotifierTestUtils;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import java.util.Date;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;

/**
 * @author mmetlov
 */
public class UserReceivedTest extends AbstractServicesTestBase {

    @Test
    public void shouldSendEmail() throws PersNotifyClientException {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, DELIVERY, null);
        event.getOrderAfter().setSubstatus(OrderSubstatus.USER_RECEIVED);
        event.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.getOrderBefore().setSubstatus(OrderSubstatus.DELIVERY_SERVICE_RECEIVED);
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());

        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.assertHasNewNotifications(1);
        Notification notification = eventTestUtils.getSingleEmailNotification();
        assertThat(notification.getDeliveryChannels(), contains(
            hasProperty("type", is(ChannelType.EMAIL))
        ));

        eventTestUtils.deliverNotifications();
        verify(persNotifyClient).createEvent(MockitoHamcrest.argThat(Matchers.allOf(
            instanceOf(NotificationEventSource.class),
            hasProperty("email", is(event.getOrderAfter().getBuyer().getEmail())),
            hasProperty("notificationSubtype", is(NotificationSubtype.BLUE_ORDER_USER_RECEIVED))
        )));
    }
}
