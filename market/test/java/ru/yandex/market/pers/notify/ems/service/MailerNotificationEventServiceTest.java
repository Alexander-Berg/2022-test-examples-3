package ru.yandex.market.pers.notify.ems.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.concurrent.Executors;
import ru.yandex.common.util.inbox.TransactionProcessor;
import ru.yandex.market.pers.notify.ems.configuration.NotificationConfigFactory;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.event.NotificationEventData;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventsLogEntity;
import ru.yandex.market.pers.notify.logging.TransactionalLogEvent;
import ru.yandex.market.pers.notify.logging.TransactionalLogEventTestConsumer;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static java.util.Comparator.comparing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 31.01.2018
 */
public class MailerNotificationEventServiceTest extends MarketMailerMockedDbTest {

    private static final Comparator<NotificationEvent> COMPARISON_ITEM_COMPARATOR =
            comparing(NotificationEvent::getAddress)
                    .thenComparing(NotificationEvent::getAddressType)
                    .thenComparing(NotificationEvent::getNotificationSubtype)
                    .thenComparing(NotificationEvent::getStatus);

    private static String EMAIL = "test@test.ru";


    private void checkLogEventsData(List<NotificationEvent> expected,
                                    List<NotificationEvent> actual) {
        List<HashMap<String, String>> expectedData = expected.stream()
                .map(NotificationEvent::getNotificationData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<HashMap<String, String>> actualData = actual.stream()
                .map(NotificationEvent::getNotificationData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertEquals(expectedData.size(), actualData.size());

        if (expectedData.size() > 0) {
            assertIterableEquals(expectedData, actualData);
        }
    }

    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private NotificationConfigFactory notificationConfigFactory;
    @Autowired
    private EventSourceDAO eventSourceDAO;
    @Autowired
    private TransactionProcessor transactionProcessor;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionalLogEventTestConsumer<NotificationEventsLogEntity> logEventTestConsumer;

    @Test
    public void archiveExpiredDeprecatedEventInNewStatus() {
        assertFalse(notificationConfigFactory.hasConfiguration(NotificationSubtype.ADVERTISING_1));
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail(EMAIL, NotificationSubtype.ADVERTISING_1)
            .build());
        MailerNotificationEventService archiveService = new MailerNotificationEventService(
            eventSourceDAO, transactionProcessor, notificationConfigFactory
        );

        archiveService.setArchiveTimeSec(-1000L);
        archiveService.archiveExpiredEvents();
        assertFalse(archiveService.mailEventsExist(event.getAddress(), event.getNotificationSubtype(),
            NotificationEventStatus.values()));

        NotificationEvent expectedEvent = new NotificationEvent();
        expectedEvent.setNotificationSubtype(NotificationSubtype.ADVERTISING_1);
        expectedEvent.setAddress(EMAIL);
        expectedEvent.setNotificationData(new NotificationEventData());
        expectedEvent.setStatus(NotificationEventStatus.NEW);

        List<TransactionalLogEvent<NotificationEventsLogEntity>> transactionalLogEvents =
                Collections.singletonList(TransactionalLogEvent.create(
                new NotificationEventsLogEntity(
                        Collections.singletonList(expectedEvent)
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
        ));

        checkLogEvents(transactionalLogEvents);
    }

    @Test
    public void updateEventsAddsNewDataForTerminalStatuses() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).build());
        String newKey = "newKey";
        String newValue = "newValue";
        event.getData().put(newKey, newValue);
        event.setStatus(NotificationEventStatus.SENT);
        mailerNotificationEventService.updateEvents(Collections.singletonList(event));
        assertEquals(newValue, mailerNotificationEventService.getEvent(event.getId()).getData().get(newKey));
    }

    @Test
    public void updateEventsDoNotAddNewDataForNonTerminalStatuses() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).build());
        String newKey = "newKey";
        String newValue = "newValue";
        event.getData().put(newKey, newValue);
        event.setStatus(NotificationEventStatus.IOERROR);
        mailerNotificationEventService.updateEvents(Collections.singletonList(event));
        assertFalse(mailerNotificationEventService.getEvent(event.getId()).getData().containsKey(newKey));
    }

    @Test
    public void testMultiEventAfterProcessing() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());
        event.setStatus(NotificationEventStatus.SENT);
        mailerNotificationEventService.updateEvents(Collections.singletonList(event));
        event = mailerNotificationEventService.getEvent(event.getId());
        assertEquals(NotificationEventStatus.SENT, event.getStatus());

        NotificationEvent eventSecond = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valeter@mail.ru", NotificationSubtype.CART_1).multiEvent().build());
        assertThat(event.getId(), IsNot.not(IsEqual.equalTo(eventSecond.getId())));
    }

    @Test
    public void testAddEvent() {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromEmail("valter@mail.ru", NotificationSubtype.CART_1).build());
        assertNotNull(event);
        NotificationEvent actual = mailerNotificationEventService.getEvent(event.getId());
        assertEquals("valter@mail.ru", actual.getAddress());
        assertEquals(NotificationSubtype.CART_1, actual.getNotificationSubtype());
    }

    @Test
    public void testAddEventNewModel() {
        String email = "someEmail@yandex.ru";
        Uid uid = new Uid(Integer.toUnsignedLong(RND.nextInt()));
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromIdentity(uid, email, NotificationSubtype.CART_1).build());
        assertNotNull(event);
        NotificationEvent actual = mailerNotificationEventService.getEvent(event.getId());
        assertEquals(email, actual.getAddress());
        assertEquals(NotificationSubtype.CART_1, actual.getNotificationSubtype());
        assertEquals(String.valueOf(uid.getValue()), actual.getData().get(NotificationEventDataName.UID));
    }

    @Test
    public void testGetEventsForProcessing() {
        int count = NotificationSubtype.values().length * 50;
        Set<Long> ids = createEvents(count);
        processEvents(ids);
        assertEquals(0, ids.size());
    }

    @Test
    public void testGetEventsForProcessingLocking() throws InterruptedException {
        int count = NotificationSubtype.values().length * 50;
        Set<Long> lockedIds = createEvents(count);
        Set<Long> ids = createEvents(count);
        Semaphore barrier = new Semaphore(0);
        Thread threadWithLock = lockIds(lockedIds, barrier);
        processEvents(ids);
        barrier.release();
        threadWithLock.join(1000L);
        assertEquals(lockedIds.size(), ids.size());
    }

    private Thread lockIds(Set<Long> lockedIds, Semaphore barrier) {
        Thread thread = new Thread(() -> transactionTemplate.execute(status -> {
            jdbcTemplate.execute("SELECT * FROM EVENT_SOURCE WHERE ID IN (" +
                lockedIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")) +
                " FOR UPDATE");
            try {
                barrier.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }));
        thread.start();
        return thread;
    }

    @Test
    public void testGetEventsForProcessingNewModel() {
        int count = NotificationSubtype.values().length * 50;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < count; i++) {
            List<NotificationSubtype> emails = new ArrayList<>(Arrays.asList(NotificationSubtype.values()));
            emails = emails.stream().filter(s -> s.getTransportType() == NotificationTransportType.MAIL).collect(Collectors.toList());
            NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
                .fromEmail("someEmail@yandex.ru", emails.get(i % emails.size()))
                .setSourceId(RND.nextLong()).build());
            assertNotNull(event);
            ids.add(event.getId());
            if ((i + 1) % 500 == 0) {
                System.out.println((i + 1) + " events added");
            }
        }
        processEvents(ids);
        assertEquals(0, ids.size());
    }

    private void processEvents(Set<Long> ids) {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < NotificationSubtype.values().length * 3; i++) {
            int current = i;
            pool.submit(() -> {
                outer:
                while (true) {
                    List<NotificationEvent> events = mailerNotificationEventService
                        .getEventsForProcessing(Collections.singletonList(NotificationSubtype.values()[current % NotificationSubtype.values().length]),
                            NotificationEventStatus.NEW);
                    if (events.size() == 0) {
                        break;
                    }
                    for (NotificationEvent event : events) {
                        if (!ids.remove(event.getId())) {
                            System.out.println("ERROR");
                            break outer;
                        }
                    }
                }
            });
        }
        pool.shutdown();
        while (true) {
            try {
                if (pool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException ignore) {
            }
        }
    }

    private Set<Long> createEvents(int count) {
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < count; i++) {
            List<NotificationSubtype> emails = new ArrayList<>(Arrays.asList(NotificationSubtype.values()));
            emails = emails.stream().filter(s -> s.getTransportType() == NotificationTransportType.MAIL).collect(Collectors.toList());
            NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
                .fromEmail("111valter@mail.ru", emails.get(i % emails.size()))
                .setSourceId(RND.nextLong()).build());
            assertNotNull(event);
            ids.add(event.getId());
            if ((i + 1) % 500 == 0) {
                System.out.println((i + 1) + " events added");
            }
        }
        return ids;
    }

    private void checkLogEvents(List<TransactionalLogEvent<NotificationEventsLogEntity>> expecteds) {
        assertEquals(expecteds.size(), logEventTestConsumer.getEvents().size());
        for (int i = 0; i < expecteds.size(); ++i) {
            TransactionalLogEvent<NotificationEventsLogEntity> expected = expecteds.get(i);
            TransactionalLogEvent<NotificationEventsLogEntity> actual = logEventTestConsumer.getEvents().get(i);
            checkLogEventsData(expected.getEntity().getItems(), actual.getEntity().getItems());
            assertEquals(expected.getAction(), actual.getAction());
            Set<NotificationEvent> expectedItems = new TreeSet<>(COMPARISON_ITEM_COMPARATOR);
            expectedItems.addAll(expected.getEntity().getItems());
            Set<NotificationEvent> actualItems = new TreeSet<>(COMPARISON_ITEM_COMPARATOR);
            actualItems.addAll(actual.getEntity().getItems());
            assertEquals(expectedItems, actualItems);
        }
    }
}
