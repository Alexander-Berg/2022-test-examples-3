package ru.yandex.market.notifier.test.integration;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Multimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.core.LogicalStream;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.jobs.zk.CheckoutImportUtils;
import ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.request.trace.RequestContextHolder;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notifier.util.providers.EventsProvider.makeOrderForMock;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;

public class BlockNewNotificationByUnavailableDeliveryChannelTest extends AbstractServicesTestBase {

    @Autowired
    private BlueEventProcessor blueEventProcessor;
    @Autowired
    private InboxService inboxService;

    @BeforeEach
    public void init() throws Exception {
        doThrow(RuntimeException.class)
                .when(persNotifyClient)
                .createEvent(any());
    }

    @Test
    public void findUnavailableTest() {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> order.setRgb(Color.BLUE)
        );
        when(checkouterClient.getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(event, true));

        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.assertHasNewNotifications(1);
        verify(checkouterClient)
                .getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);

        // get notification from db
        Notification notification = eventTestUtils.getSingleEmailNotification();

        //try to deliver NEW notifications
        eventTestUtils.deliverNotifications();

        int stream = notification.getDeliveryChannels().iterator().next().getStream();

        Date sendAfterDate = new Date();
        Multimap<LogicalStream, ChannelType> failSet = inboxService.findUnavailableDeliveryChannel(stream, sendAfterDate);

        assertEquals(1, failSet.size());

        Collection<ChannelType> unavailableChannelTypes =
                failSet.get(LogicalStream.valueOf(notification, notification.getDeliveryChannels().iterator().next()));

        assertEquals(1, unavailableChannelTypes.size());
        ChannelType unavailableChannelType = unavailableChannelTypes.iterator().next();

        assertEquals(ChannelType.EMAIL, unavailableChannelType);
    }

    @Test
    public void skipBlockedNotificationsTest() {
        OrderHistoryEvent firstEvent = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> order.setRgb(Color.BLUE)
        );
        when(checkouterClient.getOrder(firstEvent.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(firstEvent, true));

        eventTestUtils.mockEvents(singletonList(firstEvent));
        eventTestUtils.assertHasNewNotifications(1);
        verify(checkouterClient)
                .getOrder(firstEvent.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);

        // get notification from db
        Notification notification = eventTestUtils.getSingleEmailNotification();

        // отправка сообщения должна быть failed
        eventTestUtils.deliverNotifications();

        int stream = notification.getDeliveryChannels().iterator().next().getStream();
        Date sendAfterDate = new Date();
        Multimap<LogicalStream, ChannelType> failSet = inboxService.findUnavailableDeliveryChannel(stream, sendAfterDate);
        assertEquals(1, failSet.size());

        // создаём еще один ивент для этого же заказа
        OrderHistoryEvent secondEvent = orderStatusUpdated(
                OrderStatus.UNPAID,
                OrderStatus.PROCESSING,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> order.setRgb(Color.BLUE)
        );
        when(checkouterClient.getOrder(secondEvent.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(secondEvent, true));

        RequestContextHolder.createNewContext();
        CheckoutImportUtils.processEvent(
                new HashSet<>(), secondEvent, List.of(blueEventProcessor)
        );
        verify(checkouterClient)
                .getOrder(firstEvent.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);

        List<Notification> newNonBlocked = inboxService.findNewNonBlocked(stream, 10, 0, null, sendAfterDate);

        assertTrue(newNonBlocked.isEmpty());
    }
}
