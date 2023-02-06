package ru.yandex.market.notifier.test.integration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.hamcrest.Matchers.hasSize;

public class BlueOrderCreationEmailTest extends AbstractServicesTestBase {
    @Test
    public void shouldAggregateEmailsForStatusProcessingsPokupki() throws Exception {
        testableClock.setFixed(ZonedDateTime.of(
                2020, 11, 20, 12, 0, 0, 0, ZoneId.systemDefault()
        ).toInstant(), ZoneId.systemDefault());

        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.PROCESSING,
                ClientInfo.SYSTEM,
                o -> {
                    o.setRgb(Color.BLUE);
                    o.setProperty(OrderPropertyType.MARKET_REQUEST_ID, "mrid");
                }
        );
        eventTestUtils.mockEvent(event);

        int totalNotifications = 1;
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        eventTestUtils.deliverNotifications();
        List<NotificationEventSource> emails = eventTestUtils.getSentEmails();
        MatcherAssert.assertThat(emails, hasSize(1));

        NotificationEventSource email = emails.get(0);
        Map<String, String> data = email.getData();
        MatcherAssert.assertThat(data.get("order_id"), CoreMatchers.is("1"));
        MatcherAssert.assertThat(data.get("multiorder_id"), CoreMatchers.is("mrid"));

    }
}
