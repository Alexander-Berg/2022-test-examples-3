package ru.yandex.market.notifier.test.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.pers.notify.model.event.NotificationEventDataName.ORDER_IDS;
import static ru.yandex.market.pers.notify.model.event.NotificationEventSource.PushBuilder.getTemplateParamName;

public class BlueOrderMultiOrderNotificationsTest extends AbstractWebTestBase {

    @BeforeEach
    void setUpThis() throws Exception {
        notifierProperties.setAggregateProcessingPush(true);
    }

    @Test
    void shouldSendOrderCreatedIfMultiOrderWasPartiallyCancelled() throws Exception {

        List<OrderHistoryEvent> eventList = new ArrayList<>();

        LongStream.rangeClosed(1, 3)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.PENDING,
                        OrderStatus.PROCESSING,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(orderId);
                            order.setRgb(Color.BLUE);
                            order.setFulfilment(true);
                            order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                        })).forEach(eventList::add);

        eventTestUtils.mockEvents(eventList);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        eventTestUtils.assertNoPushesSent();

        List<OrderHistoryEvent> additionalEventList = new ArrayList<>();
        LongStream.rangeClosed(4, 5)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.UNPAID,
                        OrderStatus.CANCELLED,
                        OrderSubstatus.USER_NOT_PAID,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(orderId);
                            order.setRgb(Color.BLUE);
                            order.setFulfilment(true);
                            order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                        })).forEach(additionalEventList::add);

        eventTestUtils.mockEvents(additionalEventList);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentPushes = eventTestUtils.getSentPushes()
                .stream()
                .filter(push -> push.getNotificationSubtype() == NotificationSubtype.PUSH_STORE_PROCESSING)
                .collect(Collectors.toList());
        MatcherAssert.assertThat(sentPushes, hasSize(1));

        NotificationEventSource push = sentPushes.get(0);

        MatcherAssert.assertThat(push.getData(), hasEntry(Matchers.is(getTemplateParamName(ORDER_IDS)), Matchers.is(
                "1,2,3")));


    }

    @Test
    void shouldSendOrderCreatedIfMultiOrderWasPartiallyCancelledBeforeProcessing() throws Exception {
        List<OrderHistoryEvent> additionalEventList = new ArrayList<>();
        LongStream.rangeClosed(4, 5)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.UNPAID,
                        OrderStatus.CANCELLED,
                        OrderSubstatus.USER_NOT_PAID,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(orderId);
                            order.setRgb(Color.BLUE);
                            order.setFulfilment(true);
                            order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                        })).forEach(additionalEventList::add);

        eventTestUtils.mockEvents(additionalEventList);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<OrderHistoryEvent> eventList = new ArrayList<>();

        LongStream.rangeClosed(1, 3)
                .mapToObj(orderId -> EventsProvider.orderStatusUpdated(
                        OrderStatus.PENDING,
                        OrderStatus.PROCESSING,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(orderId);
                            order.setRgb(Color.BLUE);
                            order.setFulfilment(true);
                            order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
                            order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                        })).forEach(eventList::add);

        eventTestUtils.mockEvents(eventList);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentPushes = eventTestUtils.getSentPushes()
                .stream()
                .filter(push -> push.getNotificationSubtype() == NotificationSubtype.PUSH_STORE_PROCESSING)
                .collect(Collectors.toList());
        MatcherAssert.assertThat(sentPushes, hasSize(1));

        NotificationEventSource push = sentPushes.get(0);

        MatcherAssert.assertThat(push.getData(), hasEntry(Matchers.is(getTemplateParamName(ORDER_IDS)), Matchers.is(
                "1,2,3")));


    }
}
