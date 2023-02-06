package ru.yandex.market.notifier.test.integration;

import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.util.providers.EventsProvider;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BlueOrderNoNotificationForOldOrderTest extends AbstractWebTestBase {

    @BeforeEach
    public void setUpThis() throws Exception {
        mockMvc.perform(put("/properties/disableUserNotificationsForTooOldOrders")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(Boolean.toString(true)))
                .andExpect(status().isOk());

        testableClock.setFixed(ZonedDateTime.of(
                2020, 10, 2, 15, 0, 0, 0, ZoneId.systemDefault()
        ).toInstant(), ZoneId.systemDefault());

    }

    @Test
    void shouldNotSendNotificationsForCancelledIfOrderIsTooOld() {
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.DELIVERY,
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_REFUSED_DELIVERY,
                ClientInfo.SYSTEM,
                o -> {
                    o.setRgb(Color.BLUE);
                    o.setPaymentType(PaymentType.POSTPAID);
                    o.setCreationDate(Date.from(Instant.now(testableClock).minus(60, ChronoUnit.DAYS)));
                }
        );

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        List<Notification> emailNotifications = eventTestUtils.getNotifications(ChannelType.EMAIL);
        MatcherAssert.assertThat(emailNotifications, hasSize(0));

        List<Notification> pushNotifications = eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH);
        MatcherAssert.assertThat(pushNotifications, hasSize(0));
    }

    @Test
    void shouldNotSendNotificationsForDeliveredIfOrderIsTooOld() {
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.DELIVERY,
                OrderStatus.DELIVERED,
                ClientInfo.SYSTEM,
                o -> {
                    o.setRgb(Color.BLUE);
                    o.setPaymentType(PaymentType.POSTPAID);
                    o.setCreationDate(Date.from(Instant.now(testableClock).minus(60, ChronoUnit.DAYS)));
                }
        );

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();
//
        List<Notification> emailNotifications = eventTestUtils.getNotifications(ChannelType.EMAIL);
        MatcherAssert.assertThat(emailNotifications, hasSize(0));

        List<Notification> pushNotifications = eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH);
        MatcherAssert.assertThat(pushNotifications, hasSize(0));
    }
}
