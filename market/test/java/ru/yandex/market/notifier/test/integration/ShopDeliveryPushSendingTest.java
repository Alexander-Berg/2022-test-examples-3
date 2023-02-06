package ru.yandex.market.notifier.test.integration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.pers.notify.model.event.NotificationEventDataName.ORDER_ID;
import static ru.yandex.market.pers.notify.model.event.NotificationEventDataName.SHOP_DELIVERY;
import static ru.yandex.market.pers.notify.model.event.NotificationEventSource.PushBuilder.getTemplateParamName;

public class ShopDeliveryPushSendingTest extends AbstractWebTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        testableClock.setFixed(ZonedDateTime.of(
                2020, 9, 30, 17, 49, 0, 0, ZoneId.systemDefault()
        ).toInstant(), ZoneId.systemDefault());
    }

    @Test
    void shouldSendPushForCreatedOrder() {
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.PROCESSING,
                ClientInfo.SYSTEM,
                order -> order.getDelivery().setDeliveryPartnerType(SHOP)
        );
        eventTestUtils.mockEvent(event);

        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> pushes = eventTestUtils.getSentPushes();
        assertThat(pushes, hasSize(1));

        NotificationEventSource notificationEventSource = pushes.get(0);
        assertThat(notificationEventSource.getNotificationSubtype(),
                is(NotificationSubtype.PUSH_STORE_PROCESSING));
        assertThat(notificationEventSource.getData(),
                hasEntry(is(getTemplateParamName(ORDER_ID)), is("1")));
        assertThat(notificationEventSource.getData(),
                hasEntry(is(getTemplateParamName(SHOP_DELIVERY)), is("true")));
    }

    @Test
    void shouldSendPushForUnpaidOrder() {
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                ClientInfo.SYSTEM,
                order -> {
                    order.getDelivery().setDeliveryPartnerType(SHOP);
                    Date statusExpiryDate = Date.from(LocalDateTime.of(2020, 12, 11, 0, 0)
                            .atZone(ZoneId.systemDefault()).toInstant());
                    order.setStatusExpiryDate(statusExpiryDate);
                }
        );
        eventTestUtils.mockEvent(event);

        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<NotificationEventSource> pushes = eventTestUtils.getSentPushes();
        assertThat(pushes, hasSize(1));

        NotificationEventSource notificationEventSource = pushes.get(0);
        assertThat(notificationEventSource.getNotificationSubtype(), is(NotificationSubtype.PUSH_STORE_PREPAID_DELAY));
        assertThat(notificationEventSource.getData(),
                hasEntry(is(getTemplateParamName(ORDER_ID)), is("1")));
        assertThat(notificationEventSource.getData(),
                hasEntry(is(getTemplateParamName(SHOP_DELIVERY)), is("true")));
        assertThat(notificationEventSource.getData(),
                hasEntry(is(getTemplateParamName("expiration_time")), is("00:00 11.12.2020")));
    }

    @Test
    void shouldNotSendPushForDeliveryOrder() {
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.PROCESSING,
                OrderStatus.DELIVERY,
                ClientInfo.SYSTEM,
                order -> {
                    order.getDelivery().setDeliveryPartnerType(SHOP);
                }
        );
        eventTestUtils.mockEvent(event);

        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        eventTestUtils.assertNoPushesSent();
    }

}
