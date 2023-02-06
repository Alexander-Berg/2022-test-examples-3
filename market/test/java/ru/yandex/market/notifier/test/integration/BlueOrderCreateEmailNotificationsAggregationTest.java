package ru.yandex.market.notifier.test.integration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class BlueOrderCreateEmailNotificationsAggregationTest extends AbstractWebTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;

    @BeforeEach
    public void setUpEach() throws Exception {
        notifierProperties.setAggregateProcessingEmail(true);
        testableClock.setFixed(ZonedDateTime.of(
                2020, 10, 2, 15, 0, 0, 0, ZoneId.systemDefault()
        ).toInstant(), ZoneId.systemDefault());
    }

    @AfterEach
    public void tearDownEach() throws Exception {
        notifierProperties.setAggregateProcessingEmail(false);
    }

    @Test
    void shouldAggregateEmailsForStatusProcessings() throws Exception {
        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(5);
        allEvents.forEach(e -> {
            e.getOrderAfter().setProperty(OrderPropertyType.MARKET_REQUEST_ID, "my_multi_order_id");
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_ID, "my_multi_order_id");
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
        });
        eventTestUtils.mockEvents(allEvents);

        int totalNotifications = allEvents.size();
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        eventTestUtils.deliverNotifications();
        List<NotificationEventSource> emails = eventTestUtils.getSentEmails();
        MatcherAssert.assertThat(emails, hasSize(1));

        NotificationEventSource email = emails.get(0);
        Map<String, String> data = email.getData();
        MatcherAssert.assertThat(Arrays.asList(data.get("order_ids").split(",")), containsInAnyOrder("1", "2", "3",
                "4", "5"));
        MatcherAssert.assertThat(data.get("multiorder_id"), CoreMatchers.is("my_multi_order_id"));
    }

    @Test
    void shouldAggregateEmailsForStatusProcessingsPokupki() throws Exception {
        int multiOrderSize = 5;
        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(multiOrderSize);
        allEvents.forEach(e -> {
            e.getOrderAfter().setProperty(OrderPropertyType.MARKET_REQUEST_ID, "my_multi_order_id");
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_ID, "my_multi_order_id");
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_SIZE, multiOrderSize);
        });
        eventTestUtils.mockEvents(allEvents);

        int totalNotifications = allEvents.size();
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        eventTestUtils.deliverNotifications();
        List<NotificationEventSource> emails = eventTestUtils.getSentEmails();
        MatcherAssert.assertThat(emails, hasSize(1));

        NotificationEventSource email = emails.get(0);
        Map<String, String> data = email.getData();
        MatcherAssert.assertThat(Arrays.asList(data.get("order_ids").split(",")), containsInAnyOrder("1", "2", "3",
                "4", "5"));
        MatcherAssert.assertThat(data.get("multiorder_id"), CoreMatchers.is("my_multi_order_id"));

    }

    @Test
    void shouldNotAggregateSimpleOrderAndMultiOrder() {
        int multiOrderSize = 5;
        List<OrderHistoryEvent> multiOrderEvents =
                eventTestUtils.getAggregatedOrderHistoryEvents(multiOrderSize);
        multiOrderEvents.forEach(e -> {
            e.getOrderAfter().setProperty(OrderPropertyType.MARKET_REQUEST_ID, "multi_order_id");
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_ID, "multi_order_id");
            e.getOrderAfter().setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
        });

        List<OrderHistoryEvent> simpleOrderHistoryEvent = eventTestUtils.getOneOrderHistoryEvents(6, 1);
        simpleOrderHistoryEvent.forEach(e -> {
            e.getOrderAfter().setProperty(OrderPropertyType.MARKET_REQUEST_ID, "single_order_id");
        });
        multiOrderEvents.addAll(simpleOrderHistoryEvent);

        eventTestUtils.mockEvents(multiOrderEvents);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> sentEmails = eventTestUtils.getSentEmails();

        MatcherAssert.assertThat(sentEmails, hasSize(2));
        List<Map<String, String>> dataList =
                sentEmails.stream()
                        .map(NotificationEventSource::getData)
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(dataList, Matchers.allOf(
                hasItem(
                        Matchers.allOf(
                                Matchers.hasEntry(NotificationEventDataName.ORDER_ID, "6"),
                                Matchers.not(Matchers.hasEntry(Matchers.is(NotificationEventDataName.ORDER_IDS),
                                        Matchers.any(String.class))),
                                Matchers.hasEntry(NotificationEventDataName.MULTIORDER_ID, "single_order_id")
                        )
                ),
                hasItem(
                        Matchers.allOf(
                                Matchers.not(Matchers.hasEntry(Matchers.is(NotificationEventDataName.ORDER_ID),
                                        Matchers.any(String.class))),
                                Matchers.hasEntry(Matchers.is(NotificationEventDataName.ORDER_IDS),
                                        Matchers.any(String.class)),
                                Matchers.hasEntry(NotificationEventDataName.MULTIORDER_ID, "multi_order_id")
                        )
                )
        ));
    }
}
