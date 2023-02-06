package ru.yandex.market.notifier.test.integration;

import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;

import static org.hamcrest.Matchers.hasProperty;

public class BlueMultiOrderImportTest extends AbstractWebTestBase {
    @Test
    void shouldNotSaveMultiOrderIdAndMultiOrderSizeIfJustOrdersForSameUser() {
        int ordersCount = 5;

        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(ordersCount);

        eventTestUtils.mockEvents(allEvents);
        eventTestUtils.runImport();

        List<Notification> notifications = eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH);
        MatcherAssert.assertThat(notifications, Matchers.hasSize(ordersCount));
        MatcherAssert.assertThat(notifications, CoreMatchers.everyItem(CoreMatchers.allOf(
                hasProperty("multiOrderId", CoreMatchers.nullValue()),
                hasProperty("multiOrderSize", CoreMatchers.nullValue())
        )));
    }

    @Test
    void shouldSaveMultiOrderIdAndMultiOrderSize() {
        String multiOrderId = UUID.randomUUID().toString();
        int multiOrderSize = 5;

        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(multiOrderSize);
        allEvents.forEach(e -> {
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId);
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_SIZE, multiOrderSize);
        });

        eventTestUtils.mockEvents(allEvents);
        eventTestUtils.runImport();

        List<Notification> notifications = eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH);
        MatcherAssert.assertThat(notifications, Matchers.hasSize(multiOrderSize));
        MatcherAssert.assertThat(notifications, CoreMatchers.everyItem(CoreMatchers.allOf(
                hasProperty("multiOrderId", CoreMatchers.is(multiOrderId)),
                hasProperty("multiOrderSize", CoreMatchers.is(multiOrderSize))
        )));
    }
}
