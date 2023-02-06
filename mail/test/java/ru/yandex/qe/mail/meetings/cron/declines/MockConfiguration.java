package ru.yandex.qe.mail.meetings.cron.declines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.blamer.DeclineEvent;
import ru.yandex.qe.mail.meetings.blamer.DeclineEvents;
import ru.yandex.qe.mail.meetings.blamer.DeclinedEventsDao;
import ru.yandex.qe.mail.meetings.config.NotificationConfiguration;
import ru.yandex.qe.mail.meetings.mocks.CommonMockConfiguration;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Event;
import ru.yandex.qe.mail.meetings.ws.CalendarFacade;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
@Import({NotificationConfiguration.class, CommonMockConfiguration.class})
@ComponentScan(basePackages = "ru.yandex.qe.mail.meetings.cron.declines")
public class MockConfiguration {
    static final String USER_NAME = "Super Man";
    private static final String LOGIN = "suma";

    @Bean
    public DeclinedEventsDao declinedEventsDao() {
        DeclinedEventsDao dao = mock(DeclinedEventsDao.class);
        when(dao.getDeclineEvent(any(), anyInt()))
                .thenReturn(Collections.singletonList(getDeclineEvents()));
        List<DeclineEvent> event = getEvents();
        when(dao.getDeclineEvent(any(), any()))
                .thenReturn(event);
        return dao;
    }

    private DeclineEvents getDeclineEvents(){
        DeclineEvents events = new DeclineEvents();
        events.setName(USER_NAME);
        events.setEmail(LOGIN + "@yandex-team.ru");
        events.setCount(2);
        return events;
    }

    @Bean
    public CalendarFacade calendarFacade() {
        CalendarFacade calendar = mock(CalendarFacade.class);
        when(calendar.getBrief(any(), any()))
                .thenReturn(Collections.singletonMap(2, mock(Event.class)));
        return calendar;
    }

    static List<DeclineEvent> getEvents() {
        DeclineEvent deleted = buildDeclinedEvent(true, 1, "2019-08-29", USER_NAME, "Moscow");
        DeclineEvent existed = buildDeclinedEvent(false, 2, "2019-08-30", USER_NAME, null);
        DeclineEvent unknown = buildDeclinedEvent(false, 3, "2019-08-31", "UNKNOWN", null);
        return Arrays.asList(deleted, existed, unknown);
    }

    private static DeclineEvent buildDeclinedEvent(boolean deleted, int id, String date, String name, String resource) {
        DeclineEvent declinedEvent = new DeclineEvent();
        declinedEvent.setDeleted(deleted);
        declinedEvent.setEventId(id);
        declinedEvent.setEventDate(date);
        declinedEvent.setName(name);
        declinedEvent.setLogin(LOGIN);
        declinedEvent.setEmail(LOGIN + "@yandex-team.ru");
        declinedEvent.setResourceName(resource);
        return declinedEvent;
    }
}
