package ru.yandex.market.pers.notify.ems.filter;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.notify.ems.NotificationEventConsumer;
import ru.yandex.market.pers.notify.ems.NotificationPostSendAction;
import ru.yandex.market.pers.notify.ems.consumer.ReturnStatusConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEventPayload;
import ru.yandex.market.pers.notify.ems.event.NotificationEventProcessingResult;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.mock.MockFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         13.05.16
 */
public class NotificationPriorityFilterTest extends MarketMailerMockedDbTest {
    @Autowired
    private NotificationPriorityFilter notificationPriorityFilter;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        jdbcTemplate.update("TRUNCATE TABLE EVENT_SOURCE");
    }

    @Test
    public void testFilterSimple() throws Exception {
        assertNull(notificationPriorityFilter.filter(null, null));
    }

    @Test
    public void testFilterSendEvent() throws Exception {
        NotificationPriorityFilter realFilter = notificationPriorityFilter;
        notificationPriorityFilter = spy(notificationPriorityFilter);
        when(
            notificationPriorityFilter.highPriorityEventsExists(any(ru.yandex.market.pers.notify.ems.event.NotificationEvent.class),
                any(Date.class), any(Date.class))
        ).thenReturn(false);
        assertEquals(defaultConsumer, notificationPriorityFilter.filter(MockFactory.generateNotificationPushEvent(),
            defaultConsumer));
        notificationPriorityFilter = realFilter;
    }

    @Test
    public void testFilterNotSendEvent() throws Exception {
        NotificationPriorityFilter realFilter = notificationPriorityFilter;
        notificationPriorityFilter = spy(notificationPriorityFilter);
        when(
            notificationPriorityFilter.highPriorityEventsExists(any(ru.yandex.market.pers.notify.ems.event.NotificationEvent.class),
                any(Date.class), any(Date.class))
        ).thenReturn(true);
        ru.yandex.market.pers.notify.ems.event.NotificationEvent event = MockFactory.generateNotificationPushEvent();
        event.setNotificationSubtype(NotificationSubtype.PUSH_WISHLIST_DISCOUNT_SINGLE);
        NotificationEventConsumer consumer = notificationPriorityFilter.filter(event,
            defaultConsumer);
        assertThat(defaultConsumer, IsNot.not(IsEqual.equalTo(consumer)));
        assertEquals(ReturnStatusConsumer.class, consumer.getClass());
        assertEquals(NotificationEventStatus.REJECTED_LOW_PRIORITY, consumer.processEvent(event).getStatus());
        notificationPriorityFilter = realFilter;
    }

    @Test
    public void testGetCurrentWindow() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        Pair<Date, Date> window = notificationPriorityFilter.getCurrentWindow(calendar.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        assertEquals(calendar.getTime(), window.getFirst());

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        assertEquals(calendar.getTime(), window.getSecond());

        Calendar december31 = Calendar.getInstance();
        december31.set(Calendar.YEAR, 2016);
        december31.set(Calendar.MONTH, Calendar.DECEMBER);
        december31.set(Calendar.DAY_OF_MONTH, 31);
        december31.set(Calendar.HOUR_OF_DAY, 0);
        december31.set(Calendar.MINUTE, 0);
        december31.set(Calendar.SECOND, 0);
        december31.set(Calendar.MILLISECOND, 0);

        Calendar january1 = Calendar.getInstance();
        january1.set(Calendar.YEAR, 2017);
        january1.set(Calendar.MONTH, Calendar.JANUARY);
        january1.set(Calendar.DAY_OF_MONTH, 1);
        january1.set(Calendar.HOUR_OF_DAY, 0);
        january1.set(Calendar.MINUTE, 0);
        january1.set(Calendar.SECOND, 0);
        january1.set(Calendar.MILLISECOND, 0);

        window = notificationPriorityFilter.getCurrentWindow(december31.getTime());
        assertEquals(december31.getTime(), window.getFirst());
        assertEquals(january1.getTime(), window.getSecond());

        for (int i = 0; i < 24 * 365; i++) {
            december31.add(Calendar.DAY_OF_YEAR, 1);
            window = notificationPriorityFilter.getCurrentWindow(december31.getTime());
            assertTrue(window.getFirst().getTime() < window.getSecond().getTime());
            assertEquals(24 * 3600 * 1000, window.getSecond().getTime() - window.getFirst().getTime());
        }
    }

    @Test
    public void testFilterSendInWindow() throws Exception {
        NotificationPriorityFilter realFilter = notificationPriorityFilter;
        Pair<Date, Date> window = notificationPriorityFilter.getCurrentWindow(new Date());
        notificationPriorityFilter = spy(notificationPriorityFilter);
        when(
            notificationPriorityFilter.highPriorityEventsExists(any(ru.yandex.market.pers.notify.ems.event.NotificationEvent.class),
                any(Date.class), any(Date.class))
        ).thenAnswer((Answer<Integer>) invocation -> {
            Date from = (Date) invocation.getArguments()[1];
            Date to = (Date) invocation.getArguments()[2];
            assertEquals(window.getFirst(), from);
            assertEquals(window.getSecond(), to);
            return null;
        });
        assertEquals(defaultConsumer, notificationPriorityFilter.filter(MockFactory.generateNotificationPushEvent(), defaultConsumer));
        notificationPriorityFilter = realFilter;
    }

    @Test
    public void testGetHighPriorityEventsCountZeroIfNoEvents() throws Exception {
        Pair<Date, Date> window = notificationPriorityFilter.getCurrentWindow(new Date());
        assertEquals(false, notificationPriorityFilter.highPriorityEventsExists(MockFactory.generateNotificationPushEvent(),
            window.getFirst(), window.getSecond()));
    }

    @Disabled
    @Test
    public void testGetHighPriorityEventsCountZeroNotInWindow() throws Exception {
        Pair<Date, Date> window = notificationPriorityFilter.getCurrentWindow(new Date());
        NotificationEventSource source = MockFactory.generateNotificationPushEventSource();
        source.setSendTime(new Date(window.getFirst().getTime() - 1000));

        ru.yandex.market.pers.notify.ems.event.NotificationEvent event = notificationEventService.addEvent(source);
        assertNotNull(event);
        assertEquals(false, notificationPriorityFilter.highPriorityEventsExists(MockFactory.generateNotificationPushEvent(),
            window.getFirst(), window.getSecond()));

        assertEquals(defaultConsumer, notificationPriorityFilter.filter(event, defaultConsumer));
    }

    @Disabled
    @Test
    public void testHighPriorityEventsFoundInWindow() throws Exception {
        List<NotificationSubtype> types = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.MAIL)
            .collect(Collectors.toList());
        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            0, 1, "valeter1@rambler.ru", "valeter1@rambler.ru", false, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            0, 1, "valeter2@rambler.ru", "valeter2@yandex.ru", true, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            4, 5, "valeter3@rambler.ru", "valeter3@rambler.ru", false, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            4, 5, "valeter4@rambler.ru", "valeter4@yandex.ru", true, null);
    }

    @Disabled // ignored because the test is broken and sparkling
    @Test
    public void testHighPriorityEventsFoundInWindowDifferentStatuses() throws Exception {
        List<NotificationSubtype> types = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.MAIL)
            .collect(Collectors.toList());

        for (NotificationEventStatus status : NotificationEventStatus.values()) {
            testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
                0, 1, "valeter1@rambler.ru", "valeter1@rambler.ru", status.isTerminal(), status);

            testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
                0, 1, "valeter2@rambler.ru", "valeter2@yandex.ru", true, status);
        }
    }

    private void testTwoEventsInOneWindow(NotificationSubtype type1, NotificationSubtype type2,
                                          Integer priority1, Integer priority2,
                                          String email1, String email2,
                                          boolean needSend, NotificationEventStatus firstStatus) throws Exception {
        jdbcTemplate.update("TRUNCATE TABLE EVENT_SOURCE");
        jdbcTemplate.update("TRUNCATE TABLE EVENT_SOURCE_DATA");

        Pair<Date, Date> window = notificationPriorityFilter.getCurrentWindow(new Date());
        NotificationEventSource source = MockFactory.generateNotificationPushEventSource();
        source.setSendTime(new Date(window.getFirst().getTime() + 1000));

        jdbcTemplate.update("UPDATE EMAIL_SUBTYPE SET PRIORITY = ? WHERE ID = ?", priority1, type1.getId());
        notificationPriorityFilter.loadPriorities();
        source.setNotificationSubtype(type1);
        source.setEmail(email1);

        ru.yandex.market.pers.notify.ems.event.NotificationEvent event1 = notificationEventService.addEvent(source);
        assertNotNull(event1);
        assertTrue(Objects.equals(priority1, notificationPriorityFilter.getEventPriority(event1)));

        if (firstStatus != null) {
            event1.setStatus(firstStatus);
            mailerNotificationEventService.updateEvents(Collections.singletonList(event1));
        }

        ru.yandex.market.pers.notify.ems.event.NotificationEvent event2 = MockFactory.generateNotificationPushEvent();
        jdbcTemplate.update("UPDATE EMAIL_SUBTYPE SET PRIORITY = ? WHERE ID = ?", priority2, type2.getId());
        notificationPriorityFilter.loadPriorities();
        event2.setNotificationSubtype(type2);
        event2.setAddress(email2);

        assertTrue(Objects.equals(priority2, notificationPriorityFilter.getEventPriority(event2)));

        assertEquals(!needSend, notificationPriorityFilter.highPriorityEventsExists(event2,
            window.getFirst(), window.getSecond()));

        if (needSend) {
            assertEquals(defaultConsumer, notificationPriorityFilter.filter(event2, defaultConsumer));
        } else {
            assertThat(defaultConsumer, IsNot.not(IsEqual.equalTo(notificationPriorityFilter.filter(event2, defaultConsumer))));
        }
    }

    @Test
    public void testLowPriorityEventsFoundInWindow() throws Exception {
        List<NotificationSubtype> types = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.MAIL)
            .collect(Collectors.toList());
        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            2, 1, "valeter1@rambler.ru", "valeter1@rambler.ru", true, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            2, 1, "valeter2@rambler.ru", "valeter2@yandex.ru", true, null);
    }

    @Test
    public void testNullPriorityEventsFoundInWindow() throws Exception {
        List<NotificationSubtype> types = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.MAIL)
            .collect(Collectors.toList());
        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            null, 1, "valeter1@rambler.ru", "valeter1@rambler.ru", true, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            null, 1, "valeter2@rambler.ru", "valeter2@yandex.ru", true, null);
    }

    @Test
    public void testHighPriorityEventsFoundInWindowButStillSend() throws Exception {
        List<NotificationSubtype> types = Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.MAIL)
            .collect(Collectors.toList());
        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            0, 0, "valeter1@rambler.ru", "valeter1@rambler.ru", true, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            1, null, "valeter2@rambler.ru", "valeter2@rambler.ru", true, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            0, 0, "valeter3@rambler.ru", "valeter3@rambler.ru", true, null);

        testTwoEventsInOneWindow(types.get(RND.nextInt(types.size())), types.get(RND.nextInt(types.size())),
            1, null, "valeter4@rambler.ru", "valeter4@rambler.ru", true, null);
    }

    @Test
    public void testGetPrioritySimple() throws Exception {
        assertNull(notificationPriorityFilter.getEventPriority(null));
    }

    @Test
    public void testGetPriorityDependsOfEventSubtype() throws Exception {
        for (NotificationSubtype notificationSubtype : NotificationSubtype.values()) {
            ru.yandex.market.pers.notify.ems.event.NotificationEvent event = MockFactory.generateNotificationPushEvent();
            event.setNotificationSubtype(notificationSubtype);
            Integer expected = notificationPriorityFilter.getEventPriority(event);

            event = MockFactory.generateNotificationPushEvent();
            event.setNotificationSubtype(notificationSubtype);
            Integer actual = notificationPriorityFilter.getEventPriority(event);
            assertEquals(expected, actual);
        }
    }

    private NotificationEventConsumer defaultConsumer = new NotificationEventConsumer<Void>() {
        @Override
        public NotificationEventProcessingResult processEvent(ru.yandex.market.pers.notify.ems.event.NotificationEvent event) {
            return null;
        }

        @Override
        public NotificationPostSendAction getPostSendAction(NotificationEventPayload<Void> event,
                                                            NotificationEventStatus status) {
            return null;
        }
    };
}
