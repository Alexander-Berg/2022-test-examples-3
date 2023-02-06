package ru.yandex.market.pers.notify.ems.persistence;

import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 04.09.15
 */
public class NotificationEventServiceTest extends MockedDbTest {
    private static final Random RND = new Random();

    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    @Test
    public void testMultiEventLeavesFirstEventData() {
        notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1)
            .addDataParam(NotificationEventDataName.ORDER_ID, "1")
            .multiEvent().build());

        NotificationEvent eventSecond = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1)
            .addDataParam(NotificationEventDataName.ORDER_ID, "2")
            .multiEvent().build());

        assertEquals("1", eventSecond.getData().get(NotificationEventDataName.ORDER_ID));
        assertEquals("1", eventSecond.getNotificationData().getMultiData().get(0).get(NotificationEventDataName.ORDER_ID));
        assertEquals("2", eventSecond.getNotificationData().getMultiData().get(1).get(NotificationEventDataName.ORDER_ID));
    }

    @Test
    public void testMultiEventDoNotResetTime() {
        NotificationEvent eventFirst = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());
        assertNotNull(eventFirst);
        assertTrue(eventFirst.getNotificationData().isMulti());

        NotificationEvent eventSecond = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());

        assertEquals(eventFirst.getId(), eventSecond.getId());
        assertEquals(eventFirst.getSendTime(), eventSecond.getSendTime());
    }

    @Test
    public void testMultiEventResetTime() {
        NotificationEvent eventFirst = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());
        assertNotNull(eventFirst);
        assertTrue(eventFirst.getNotificationData().isMulti());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventFirst.getSendTime());
        calendar.add(Calendar.HOUR, 1);
        NotificationEvent eventSecond = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1)
            .setSendTime(calendar.getTime())
            .multiEvent(true)
            .build());

        assertEquals(eventFirst.getId(), eventSecond.getId());
        assertTrue(eventSecond.getSendTime().getTime() - eventFirst.getSendTime().getTime() >= 1000 * 3600);
    }

    @Test
    public void testMultiEvent() {
        NotificationEvent eventFirst = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());
        assertNotNull(eventFirst);
        assertTrue(eventFirst.getNotificationData().isMulti());

        NotificationEvent eventSecond = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());
        assertEquals(eventFirst.getId(), eventSecond.getId());
    }

    @Test
    public void testMultiEventData() {
        NotificationEvent eventFirst = notificationEventService.addEvent(new NotificationEventSource.Builder<>()
            .setMbiAddress("sh... on a stick")
            .setNotificationSubtype(NotificationSubtype.SHOP_GRADE)
            .addDataParam("name1", "value1")
            .multiEvent().build());

        assertEquals(1, eventFirst.getNotificationData().getMultiData().size());
        assertEquals("name1", eventFirst.getNotificationData().getMultiData().get(0).keySet().iterator().next());
        assertEquals("value1", eventFirst.getNotificationData().getMultiData().get(0).values().iterator().next());

        NotificationEvent eventSecond = notificationEventService.addEvent(new NotificationEventSource.Builder<>()
            .setMbiAddress("sh... on a stick")
            .setNotificationSubtype(NotificationSubtype.SHOP_GRADE)
            .addDataParam("name2", "value2")
            .multiEvent().build());

        assertEquals(2, eventSecond.getNotificationData().getMultiData().size());
        assertEquals("name1", eventSecond.getNotificationData().getMultiData().get(0).keySet().iterator().next());
        assertEquals("value1", eventSecond.getNotificationData().getMultiData().get(0).values().iterator().next());
        assertEquals("name2", eventSecond.getNotificationData().getMultiData().get(1).keySet().iterator().next());
        assertEquals("value2", eventSecond.getNotificationData().getMultiData().get(1).values().iterator().next());
    }

    @Test
    public void testMultiEventDataSameKeys() {
        NotificationEvent eventFirst = notificationEventService.addEvent(new NotificationEventSource.Builder<>()
            .setMbiAddress("sh... on a stick")
            .setNotificationSubtype(NotificationSubtype.SHOP_GRADE)
            .addDataParam("name1", "value1")
            .multiEvent().build());

        assertEquals(1, eventFirst.getNotificationData().getMultiData().size());
        assertEquals("name1", eventFirst.getNotificationData().getMultiData().get(0).keySet().iterator().next());
        assertEquals("value1", eventFirst.getNotificationData().getMultiData().get(0).values().iterator().next());

        NotificationEvent eventSecond = notificationEventService.addEvent(new NotificationEventSource.Builder<>()
            .setMbiAddress("sh... on a stick")
            .setNotificationSubtype(NotificationSubtype.SHOP_GRADE)
            .addDataParam("name1", "value1")
            .multiEvent().build());

        assertEquals(2, eventSecond.getNotificationData().getMultiData().size());
        assertEquals("name1", eventSecond.getNotificationData().getMultiData().get(0).keySet().iterator().next());
        assertEquals("value1", eventSecond.getNotificationData().getMultiData().get(0).values().iterator().next());
        assertEquals("name1", eventSecond.getNotificationData().getMultiData().get(1).keySet().iterator().next());
        assertEquals("value1", eventSecond.getNotificationData().getMultiData().get(1).values().iterator().next());
    }

    @Test
    public void testAddEventRemovesUnicodeSurrogatesFromData() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valter@mail.ru", NotificationSubtype.CART_1)
            .addDataParam("data", "\uD800\uDC00\uDC01test")
            .build());
        assertEquals("test", event.getData().get("data"));
    }

    @Test
    public void testAddEventWithNullValue() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valter@mail.ru", NotificationSubtype.CART_1)
            .addDataParam("data", null)
            .build());
        assertFalse(event.getData().containsKey("data"));
    }

    @Test
    public void testAddEventBadEmail() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valte@r@mailru", NotificationSubtype.CART_1).build());
        assertEquals(null, event);
    }

    @Test
    public void testPopulateSubscriberInfoPush() {
        NotificationSubtype type = NotificationSubtype.PUSH_CART;
        NotificationEventSource source = NotificationEventSource.fromEmail("valter213512r@mail.ru", type).build();
        try {
            notificationEventService.addEventIfApplicable(source);
            fail("uuid or subscriberId must be defined for push");
        } catch (IllegalArgumentException ignored) {
        }

        long uid = Integer.toUnsignedLong(RND.nextInt());
        assertTrue(notificationEventService.populateSubscriberInfo(
            NotificationEventSource.fromUid(uid, type).build()
        ));

        String uuid = UUID.randomUUID().toString();
        assertTrue(notificationEventService.populateSubscriberInfo(
            NotificationEventSource.fromUuid(uuid, type).build()
        ));
    }

    @Test
    public void testPopulateSubscriberInfoMail() {
        NotificationSubtype type = NotificationSubtype.ORDER_PROCESSING;
        try {
            notificationEventService.addEventIfApplicable(
                NotificationEventSource.fromEmail("valter", type).build()
            );
            fail("email is invalid");
        } catch (IllegalArgumentException ignored) {
        }

        long uid = RND.nextLong();

        String newEmail = "valterNew@mail.ru";
        NotificationEventSource source = NotificationEventSource.fromUid(uid, type).setEmail(newEmail).build();
        assertTrue(notificationEventService.populateSubscriberInfo(source));
        assertEquals(newEmail, source.getEmail());

        String email = UUID.randomUUID().toString() + "@mail.ru";
        assertTrue(notificationEventService.populateSubscriberInfo(
            NotificationEventSource.fromEmail(email, type).build()
        ));

        String uuid = UUID.randomUUID().toString();
        assertTrue(notificationEventService.populateSubscriberInfo(
            NotificationEventSource.fromUuid(uuid, NotificationSubtype.PUSH_CART).build()
        ));

        assertFalse(notificationEventService.populateSubscriberInfo(
            NotificationEventSource.fromUuid(uuid, type).build()
        ));
    }

    @Test
    public void testPopulateSubscriberInfoMailForNewModel() {
        NotificationSubtype type = NotificationSubtype.ORDER_PROCESSING;
        try {
            notificationEventService.addEventIfApplicable(
                NotificationEventSource.fromEmail("valter", type).build()
            );
            fail("email is invalid");
        } catch (IllegalArgumentException ignored) {
        }

        long uid = Integer.toUnsignedLong(RND.nextInt());

        String email = "newEmail@yandex.ru";
        blackBoxPassportService.doReturn(uid, email);
        NotificationEventSource source = NotificationEventSource.fromIdentity(new Uid(uid), null, type).build();
        assertTrue(notificationEventService.populateSubscriberInfo(source));
        assertEquals(email, source.getEmail());
        assertEquals(email, source.getEventAddress());
        assertEquals(String.valueOf(uid), source.getData().get(NotificationEventDataName.UID));
    }
}
