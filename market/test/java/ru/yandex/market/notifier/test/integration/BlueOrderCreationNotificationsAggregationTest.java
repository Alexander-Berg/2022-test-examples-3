package ru.yandex.market.notifier.test.integration;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.notify.model.event.NotificationEventDataName.ORDER_ID;
import static ru.yandex.market.pers.notify.model.event.NotificationEventDataName.ORDER_IDS;
import static ru.yandex.market.pers.notify.model.event.NotificationEventSource.PushBuilder.getTemplateParamName;

public class BlueOrderCreationNotificationsAggregationTest extends AbstractWebTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;

    @BeforeEach
    public void setUp() {
        super.setUp();
        testableClock.setFixed(ZonedDateTime.of(
                2020, 9, 30, 17, 49, 0, 0, ZoneId.systemDefault()
        ).toInstant(), ZoneId.systemDefault());
    }

    @Test
    public void shouldAggregatedSmsForStatus() {
        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(5);
        eventTestUtils.mockEvents(allEvents);

        sendNotifications(allEvents);
    }

    @Test
    public void shouldNotPassOrderIdsForSingleOrder() throws Exception {
        notifierProperties.setAggregateProcessingPush(true);

        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.PENDING,
                OrderStatus.PROCESSING,
                ClientInfo.SYSTEM,
                o -> {
                    o.setRgb(Color.BLUE);
                }
        );
        eventTestUtils.mockEvent(event);

        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> pushes = eventTestUtils.getSentPushes();
        MatcherAssert.assertThat(pushes, hasSize(1));

        NotificationEventSource notificationEventSource = pushes.get(0);
        MatcherAssert.assertThat(notificationEventSource.getData(),
                hasEntry(Matchers.is(getTemplateParamName(ORDER_ID)), Matchers.is("1")));
        MatcherAssert.assertThat(notificationEventSource.getData(),
                not(hasEntry(Matchers.is(getTemplateParamName(ORDER_IDS)), Matchers.any(String.class))));
    }

    @Test
    public void shouldAggregatePushForStatus() throws Exception {
        notifierProperties.setAggregateProcessingPush(true);

        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(5);
        allEvents.forEach(
                e -> {
                    e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                    e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                }
        );
        eventTestUtils.mockEvents(allEvents);

        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> pushes = eventTestUtils.getSentPushes();
        MatcherAssert.assertThat(pushes, hasSize(1));

        NotificationEventSource notificationEventSource = pushes.get(0);
        MatcherAssert.assertThat(notificationEventSource.getData(),
                not(hasEntry(Matchers.is(getTemplateParamName(ORDER_ID)), Matchers.any(String.class))));
        MatcherAssert.assertThat(notificationEventSource.getData(),
                hasEntry(Matchers.is(getTemplateParamName(ORDER_IDS)), Matchers.is("1,2,3,4,5")));
    }

    @Test
    public void shouldNotSendPushIfNotReady() throws Exception {
        notifierProperties.setAggregateProcessingPush(true);

        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(3);
        allEvents.forEach(
                e -> {
                    e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                    e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                }
        );
        eventTestUtils.mockEvents(allEvents);

        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        eventTestUtils.assertNoPushesSent();
    }

    @Test
    public void shouldIncRetryCountForFailedAggregatedPushes() throws Exception {
        notifierProperties.setAggregateProcessingPush(true);
        mockFactory.mockPersNotifyClientToThrowForPush(persNotifyClient);
        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(5);
        eventTestUtils.mockEvents(allEvents);

        sendNotifications(allEvents);

        assertAllNotificationsIncRetryCountAndBeFailed();
    }

    @Test
    public void shouldRetryAllNotifications() throws Exception {
        int notificationNumber = 200;
        notifierProperties.setAggregateProcessingPush(true);
        mockFactory.mockPersNotifyClientToThrowForPush(persNotifyClient);
        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(notificationNumber);
        eventTestUtils.mockEvents(allEvents);

        sendNotifications(allEvents);

        assertAllNotificationsIncRetryCountAndBeFailed();

        //симулируем, что прошло 10 минут, иначе ретрай посыла нотификаций будет запрещен
        setFixedTime(Clock.systemDefaultZone().instant().plus(10, ChronoUnit.MINUTES), ZoneId.systemDefault());

        for (int i = 1; i < 4; ++i) {
            assertThatNotificationsWasRetriedTwice(i * 50);
        }
    }

    @Test
    public void shouldRetryNotificationsGroupedByOrderIdAndChannel() throws Exception {
        int sameGroupNotificationsNumber = 30;
        notifierProperties.setAggregateProcessingPush(true);
        mockFactory.mockPersNotifyClientToThrowForPush(persNotifyClient);
        List<OrderHistoryEvent> firstOrderEvents = eventTestUtils.getOneOrderHistoryEvents(1L,
                sameGroupNotificationsNumber);
        List<OrderHistoryEvent> secondOrderEvents = eventTestUtils.getOneOrderHistoryEvents(2L,
                sameGroupNotificationsNumber);

        firstOrderEvents.addAll(secondOrderEvents);

        eventTestUtils.mockEvents(firstOrderEvents);

        sendNotifications(firstOrderEvents);

        assertAllNotificationsIncRetryCountAndBeFailed();

        //симулируем, что прошло 10 минут, иначе ретрай посыла нотификаций будет запрещен
        setFixedTime(Clock.systemDefaultZone().instant().plus(10, ChronoUnit.MINUTES), ZoneId.systemDefault());

        for (int i = 1; i < 2; ++i) {
            assertThatNotificationsWasRetriedTwice(i * sameGroupNotificationsNumber);
        }
    }

    private void sendNotifications(List<OrderHistoryEvent> allEvents) {
        int totalNotifications = allEvents.size();
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        eventTestUtils.deliverNotifications();
        List<NotificationEventSource> emails = eventTestUtils.getSentEmails();
        MatcherAssert.assertThat(emails, hasSize(allEvents.size()));
    }

    private void assertAllNotificationsIncRetryCountAndBeFailed() {
        List<DeliveryChannel> deliveries = eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH)
                .stream().map(Notification::getDeliveryChannels).flatMap(Collection::stream).collect(toList());
        assertThat(deliveries, everyItem(hasProperty("retryCount", is(1))));
        assertThat(deliveries, everyItem(hasProperty("status", Matchers.is(NotificationStatus.FAILED))));
    }

    private void assertThatNotificationsWasRetriedTwice(int notificationsNumber) {
        eventTestUtils.deliverNotifications();
        assertEquals(notificationsNumber, eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH)
                .stream()
                .map(Notification::getDeliveryChannels)
                .flatMap(Collection::stream)
                .filter(dn -> dn.getRetryCount() == 2)
                .count()
        );
    }
}
