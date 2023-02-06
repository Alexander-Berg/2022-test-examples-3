package ru.yandex.market.pers.notify.ems.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.notify.ems.NotificationEventConsumer;
import ru.yandex.market.pers.notify.ems.consumer.ReturnStatusConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationSubtypeStatus;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 13.10.17
 */
public class NotificationSubtypeStatusFilterTest extends MarketMailerMockedDbTest {
    @Autowired
    private NotificationSubtypeStatusFilter notificationSubtypeStatusFilter;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void filterActiveSubtype() {
        NotificationSubtype subtype = NotificationSubtype.ADVERTISING_1;
        NotificationSubtypeStatus status = NotificationSubtypeStatus.ACTIVE;
        jdbcTemplate.update("UPDATE EMAIL_SUBTYPE SET STATUS_ID = ? WHERE ID = ?", status.getId(), subtype.getId());
        NotificationEventConsumer defaultConsumer = new ReturnStatusConsumer(NotificationEventStatus.SENT);
        NotificationEvent event = new NotificationEvent();
        event.setNotificationSubtype(subtype);
        assertEquals(defaultConsumer, notificationSubtypeStatusFilter.filter(event, defaultConsumer));
    }

    @Test
    public void filterNotActiveSubtype() {
        NotificationSubtype subtype = NotificationSubtype.ADVERTISING_1;
        NotificationSubtypeStatus status = NotificationSubtypeStatus.NOT_ACTIVE;
        jdbcTemplate.update("UPDATE EMAIL_SUBTYPE SET STATUS_ID = ? WHERE ID = ?", status.getId(), subtype.getId());
        NotificationEventConsumer defaultConsumer = new ReturnStatusConsumer(NotificationEventStatus.SENT);
        NotificationEvent event = new NotificationEvent();
        event.setNotificationSubtype(subtype);

        NotificationEventConsumer actualConsumer = notificationSubtypeStatusFilter.filter(event, defaultConsumer);
        assertNotEquals(defaultConsumer, actualConsumer);
        assertEquals(NotificationEventStatus.REJECTED_AS_UNNECESSARY, actualConsumer.processEvent(event).getStatus());
    }

    @Test
    public void filterTestingSubtypeTestingEmail() {
        String email = "dsfd@sdfsdf";
        NotificationSubtype subtype = NotificationSubtype.ADVERTISING_1;
        NotificationSubtypeStatus status = NotificationSubtypeStatus.ACTIVE_FOR_TEST;
        jdbcTemplate.update("UPDATE EMAIL_SUBTYPE SET STATUS_ID = ? WHERE ID = ?", status.getId(), subtype.getId());
        jdbcTemplate.update("INSERT INTO TEST_EMAIL (EMAIL) VALUES (?)", email);
        NotificationEventConsumer defaultConsumer = new ReturnStatusConsumer(NotificationEventStatus.SENT);
        NotificationEvent event = new NotificationEvent();
        event.setAddress(email);
        event.setNotificationSubtype(subtype);
        assertEquals(defaultConsumer, notificationSubtypeStatusFilter.filter(event, defaultConsumer));
    }

    @Test
    public void filterTestingSubtypeNotTestingEmail() {
        NotificationSubtype subtype = NotificationSubtype.ADVERTISING_1;
        NotificationSubtypeStatus status = NotificationSubtypeStatus.ACTIVE_FOR_TEST;
        jdbcTemplate.update("UPDATE EMAIL_SUBTYPE SET STATUS_ID = ? WHERE ID = ?", status.getId(), subtype.getId());
        NotificationEventConsumer defaultConsumer = new ReturnStatusConsumer(NotificationEventStatus.SENT);
        NotificationEvent event = new NotificationEvent();
        event.setNotificationSubtype(subtype);
        event.setAddress("dsfd@sdfsdf");
        event.setAddressType(EventAddressType.MAIL);
        NotificationEventConsumer actualConsumer = notificationSubtypeStatusFilter.filter(event, defaultConsumer);
        assertNotEquals(defaultConsumer, actualConsumer);
        assertEquals(NotificationEventStatus.REJECTED_AS_UNNECESSARY, actualConsumer.processEvent(event).getStatus());
    }
}
