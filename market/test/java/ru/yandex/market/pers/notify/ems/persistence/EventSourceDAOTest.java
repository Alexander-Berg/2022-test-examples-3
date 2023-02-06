package ru.yandex.market.pers.notify.ems.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 03.09.15
 */
public class EventSourceDAOTest extends MockedDbTest {
    @Autowired
    private EventSourceDAO eventSourceDAO;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private Throwable exception = null;

    @BeforeEach
    public void setUp() {
        exception = null;
    }

    @Test
    public void testAddNewData() {
        String oldDataKey = "oldDataKey";
        String oldDataValue = "oldDataValue";
        NotificationEvent event = eventSourceDAO
            .add(NotificationEventSource.fromEmail("my@email.com", NotificationSubtype.PA_EXIST_ON_SALE)
                .setSourceId(1L)
                .addDataParam(oldDataKey, oldDataValue)
                .build());
        String newDataKey = "newDataKey";
        String newDataValue = "newDataValue";
        event.getData().put(newDataKey, newDataValue);
        event.getData().put(oldDataKey, "unknown");
        eventSourceDAO.addNewEventsData(Collections.singletonList(event));
        event = eventSourceDAO.getMailEvent(event.getId());
        assertEquals(newDataValue, event.getData().get(newDataKey));
        assertEquals(oldDataValue, event.getData().get(oldDataKey));
    }

    @Test
    public void testUpdateSendTime() {
        NotificationEvent event = eventSourceDAO.add(new NotificationEventSource.Builder()
            .setEmail("valter@yandex-team.ru")
            .setNotificationSubtype(NotificationSubtype.ADVERTISING_1)
            .build());
        assertNotNull(event.getSendTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(event.getSendTime());
        calendar.add(Calendar.HOUR, 1);
        eventSourceDAO.updateSendTime(event.getId(), calendar.getTime());
        NotificationEvent actualEvent = eventSourceDAO.getMailEvent(event.getId());
        assertNotNull(actualEvent.getSendTime());
        assertTrue(actualEvent.getSendTime().getTime() - event.getSendTime().getTime() >= 1000 * 3600);
    }

    @Test
    public void testMultiEventNotProcessingWhenSelected() {
        String address = "shopId";

        NotificationEvent event = eventSourceDAO.add(new NotificationEventSource.Builder()
            .setMbiAddress(address)
            .setNotificationSubtype(NotificationSubtype.SHOP_GRADE)
            .multiEvent()
            .build()
        );
        assertNotNull(event);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicBoolean gotForUpdate = new AtomicBoolean(false);
        AtomicBoolean gotForProcessing = new AtomicBoolean(false);

        pool.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    List<NotificationEvent> events = eventSourceDAO.getEventsForUpdate(NotificationSubtype.SHOP_GRADE, null, address);
                    assertTrue(events.stream().allMatch(e -> e.getAddressType().equals(EventAddressType.MBI)));
                    assertEquals(1, events.size());
                    assertEquals(event.getId(), events.get(0).getId());
                    gotForUpdate.set(true);
                    System.out.println("1 finish");
                    while (!gotForProcessing.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    System.out.println("2 finish");
                    return null;
                });
            } catch (Throwable e) {
                System.out.println("1 exception");
                gotForUpdate.set(true);
                exception = e;
            }
        });

        pool.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    while (!gotForUpdate.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    System.out.println("3 finish");
                    List<NotificationEvent> events = eventSourceDAO.getMailEventsForProcessing(Collections.singletonList(
                        NotificationSubtype.SHOP_GRADE
                    ), 100, NotificationEventStatus.NEW);
                    assertTrue(events.stream().allMatch(e -> e.getAddressType().equals(EventAddressType.MBI)));
                    assertTrue(events.isEmpty());
                    gotForProcessing.set(true);
                    System.out.println("4 finish");
                    return null;
                });
            } catch (Throwable e) {
                System.out.println("2 exception");
                gotForProcessing.set(true);
                exception = e;
            }
        });

        System.out.println("5 finish");
        pool.shutdown();
        while (true) {
            try {
                if (pool.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {
            }
        }
        if (exception != null) {
            fail(exception.getMessage());
        }
        System.out.println("6 finish");
    }

    @Test
    public void testAddEventSource() {
        Calendar sendTime = Calendar.getInstance();
        sendTime.setTime(new Date());
        sendTime.add(Calendar.HOUR_OF_DAY, -1);
        NotificationEvent event = eventSourceDAO.add(
            NotificationSubtype.ADVERTISING_1, EventAddressType.MAIL, "valter@mail.ru",
            sendTime.getTime(),
            NotificationEventStatus.REJECTED_AS_UNPOLITE, 2L,
            new HashMap<String, String>() {{
                put("name", "valter");
                put("age", "24");
            }});
        assertNotNull(event);
        NotificationEvent actual = eventSourceDAO.getMailEvent(event.getId());
        List<NotificationEvent> actualList = eventSourceDAO.getMailEventsForProcessing(
            Collections.singletonList(NotificationSubtype.ADVERTISING_1), 100, NotificationEventStatus.REJECTED_AS_UNPOLITE
        );
        assertTrue(actualList.size() > 0);
        assertEquals(actual, actualList.get(0));
        assertEquals("valter", actualList.get(0).getData().get("name"));
        assertEquals("24", actualList.get(0).getData().get("age"));

        assertEquals("valter@mail.ru", actual.getAddress());
        assertEquals(NotificationSubtype.ADVERTISING_1, actual.getNotificationSubtype());
        assertEquals("valter", actual.getData().get("name"));
        assertEquals("24", actual.getData().get("age"));
        assertEquals(EventAddressType.MAIL, actual.getAddressType());
    }
}
