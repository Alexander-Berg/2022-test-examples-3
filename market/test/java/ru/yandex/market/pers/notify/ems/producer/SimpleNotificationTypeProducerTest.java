package ru.yandex.market.pers.notify.ems.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 29.03.16
 */
public class SimpleNotificationTypeProducerTest extends MarketMailerMockedDbTest {
    @Autowired
    NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;

    @Test
    public void test() {
        notificationEventService.addEvent(NotificationEventSource
            .fromEmail("mymail@mymail.ru", NotificationSubtype.PA_WELCOME)
            .build());
        SimpleNotificationTypeProducer producer =
            new SimpleNotificationTypeProducer(100, mailerNotificationEventService);
        assertEquals(1, producer.apply(NotificationSubtype.PA_WELCOME).size());
    }
}
