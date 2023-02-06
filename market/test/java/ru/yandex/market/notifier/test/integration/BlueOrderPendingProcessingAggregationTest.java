package ru.yandex.market.notifier.test.integration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.hamcrest.Matchers.hasSize;

public class BlueOrderPendingProcessingAggregationTest extends AbstractWebTestBase {

    @BeforeEach
    public void setUpEach() throws Exception {
        notifierProperties.setReplacePendingWithProcessingForMultiOrder(true);
        notifierProperties.setAggregateProcessingPush(true);
        notifierProperties.setAggregateProcessingEmail(true);
    }

    @Test
    void shouldAggregateIfAllPending() {
        List<OrderHistoryEvent> events = LongStream.rangeClosed(1, 3)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.RESERVED,
                        OrderStatus.PENDING,
                        ClientInfo.SYSTEM,
                        o -> {
                            o.setId(orderId);
                            o.setRgb(Color.BLUE);
                            o.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 3);
                        }
                ))
                .collect(Collectors.toList());

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentPushes = eventTestUtils.getSentPushes();
        List<NotificationEventSource> sentEmails = eventTestUtils.getSentEmails();

        MatcherAssert.assertThat(sentPushes, hasSize(1));
        MatcherAssert.assertThat(sentEmails, hasSize(1));

        MatcherAssert.assertThat(sentPushes, hasSize(1));
        NotificationEventSource push = sentPushes.get(0);
        MatcherAssert.assertThat(push.getNotificationSubtype(),
                CoreMatchers.is(NotificationSubtype.PUSH_STORE_PROCESSING));
    }

    @Test
    void shouldAggregateIfAllProcessing() {
        List<OrderHistoryEvent> events = LongStream.rangeClosed(1, 3)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.RESERVED,
                        OrderStatus.PROCESSING,
                        ClientInfo.SYSTEM,
                        o -> {
                            o.setId(orderId);
                            o.setRgb(Color.BLUE);
                            o.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 3);
                        }
                ))
                .collect(Collectors.toList());

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentPushes = eventTestUtils.getSentPushes();
        List<NotificationEventSource> sentEmails = eventTestUtils.getSentEmails();

        MatcherAssert.assertThat(sentPushes, hasSize(1));
        MatcherAssert.assertThat(sentEmails, hasSize(1));

        NotificationEventSource push = sentPushes.get(0);
        MatcherAssert.assertThat(push.getNotificationSubtype(),
                CoreMatchers.is(NotificationSubtype.PUSH_STORE_PROCESSING));
    }

    @Test
    void shouldAggregateIfAllPendingThenAllProcessing() {
        List<OrderHistoryEvent> events = LongStream.rangeClosed(1, 3)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.RESERVED,
                        OrderStatus.PENDING,
                        ClientInfo.SYSTEM,
                        o -> {
                            o.setId(orderId);
                            o.setRgb(Color.BLUE);
                            o.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 3);
                        }
                ))
                .collect(Collectors.toList());

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentPushes = eventTestUtils.getSentPushes();
        List<NotificationEventSource> sentEmails = eventTestUtils.getSentEmails();

        MatcherAssert.assertThat(sentPushes, hasSize(1));
        MatcherAssert.assertThat(sentEmails, hasSize(1));
        NotificationEventSource push = sentPushes.get(0);
        MatcherAssert.assertThat(push.getNotificationSubtype(),
                CoreMatchers.is(NotificationSubtype.PUSH_STORE_PROCESSING));

        events = LongStream.rangeClosed(1, 3)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.PENDING,
                        OrderStatus.PROCESSING,
                        ClientInfo.SYSTEM,
                        o -> {
                            o.setId(orderId);
                            o.setRgb(Color.BLUE);
                            o.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 3);
                        }
                ))
                .collect(Collectors.toList());

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();
        Mockito.reset(persNotifyClient);
        eventTestUtils.deliverNotifications();

        eventTestUtils.assertNoPushesSent();

    }

    @Test
    void shouldAggregateIfMixingPendingAndProcessing() {
        List<OrderHistoryEvent> events = LongStream.rangeClosed(1, 2)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.RESERVED,
                        OrderStatus.PENDING,
                        ClientInfo.SYSTEM,
                        o -> {
                            o.setId(orderId);
                            o.setRgb(Color.BLUE);
                            o.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 3);
                        }
                ))
                .collect(Collectors.toList());

        events.add(EventsProvider.orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.PROCESSING,
                ClientInfo.SYSTEM,
                o -> {
                    o.setId(3L);
                    o.setRgb(Color.BLUE);
                    o.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                    o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 3);
                }
        ));

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentPushes = eventTestUtils.getSentPushes();
        List<NotificationEventSource> sentEmails = eventTestUtils.getSentEmails();

        MatcherAssert.assertThat(sentPushes, hasSize(1));
        MatcherAssert.assertThat(sentEmails, hasSize(1));

        NotificationEventSource push = sentPushes.get(0);
        MatcherAssert.assertThat(push.getNotificationSubtype(),
                CoreMatchers.is(NotificationSubtype.PUSH_STORE_PROCESSING));
    }
}
