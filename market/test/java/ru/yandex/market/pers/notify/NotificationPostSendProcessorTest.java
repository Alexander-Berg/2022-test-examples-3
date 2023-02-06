package ru.yandex.market.pers.notify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.persistence.NotificationPostSendProcessor;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.mail.consumer.PriceAlertConsumer;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class NotificationPostSendProcessorTest extends MarketMailerMockedDbTest {
    @Autowired
    private PriceAlertConsumer priceAlertConsumer;

    @Autowired
    private NotificationPostSendProcessor notificationPostSendProcessor;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;

    @Test
    public void testProcess() throws Exception {
        String email = "valetr@yandex.ru";
        ru.yandex.market.pers.notify.ems.event.NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail(email, NotificationSubtype.PA_WELCOME)
            .addDataParam(NotificationEventDataName.MODEL_ID, "1")
            .setSourceId(1L).build());
		assertNotNull(event);
        NotificationEvent event1 = mailerNotificationEventService.getEvent(event.getId());
        event1.setStatus(NotificationEventStatus.SENT);
        event1.setRepeatPostAction(true);
        assertEquals(EventAddressType.MAIL, event.getAddressType());
        mailerNotificationEventService.updateEvents(Collections.singletonList(event1));
        event1 = mailerNotificationEventService.getEvent(event1.getId());
        assertTrue(event1.isRepeatPostAction());
        notificationPostSendProcessor.process(NotificationSubtype.PA_WELCOME, priceAlertConsumer);
        event1 = mailerNotificationEventService.getEvent(event1.getId());
        assertFalse(event1.isRepeatPostAction());
    }

}
