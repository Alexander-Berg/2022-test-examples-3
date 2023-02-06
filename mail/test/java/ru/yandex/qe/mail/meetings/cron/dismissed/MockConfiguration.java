package ru.yandex.qe.mail.meetings.cron.dismissed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.config.NotificationConfiguration;
import ru.yandex.qe.mail.meetings.mocks.CommonMockConfiguration;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Decision;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Event;
import ru.yandex.qe.mail.meetings.services.calendar.dto.EventUser;
import ru.yandex.qe.mail.meetings.services.calendar.dto.User;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.services.staff.dto.Email;
import ru.yandex.qe.mail.meetings.services.staff.dto.Language;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.utils.DateRangeTest;
import ru.yandex.qe.mail.meetings.ws.CalendarFacade;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
@Import({NotificationConfiguration.class, CommonMockConfiguration.class})
@ComponentScan(basePackages = "ru.yandex.qe.mail.meetings.cron.dismissed")
public class MockConfiguration {
    private static final int OFFICE_ID = 1;

    @Bean
    public CalendarFacade calendarFacade(StaffClient staffClient) {
        CalendarFacade facade = mock(CalendarFacade.class);
        List<Pair<Event, List<User>>> events = new ArrayList<>();
        User organizer = toUser(staffClient.getByUid("2"));
        EventUser user = toEventUser(staffClient.getByUid("3"));
        EventUser subscriber = toEventUser(staffClient.getByUid("5"));
        List<User> users = Arrays.asList(toEventUser(staffClient.getByUid("1")), user);
        events.add(Pair.of(event(1, organizer, users, Collections.emptyList()), users));
        events.add(Pair.of(event(2, organizer, users, Collections.emptyList()), Collections.singletonList(user)));
        events.add(Pair.of(event(3, organizer, users, Collections.singletonList(subscriber)),
                Collections.singletonList(subscriber)));
        when(facade.findDismissedAttendees(any(), any(), any()))
                .thenReturn(events);
        return facade;
    }

    private Event event(int id, User u, List<User> attendees, List<User> subscribers) {
        EventUser eu = new EventUser(u.getName(), u.getEmail(), u.getLogin(), u.getOfficeId(), Decision.YES);
        Event event = mock(Event.class);
        when(event.getEventId()).thenReturn(id);
        when(event.getName()).thenReturn("Test name");
        when(event.getOrganizer()).thenReturn(eu);
        when(event.getAttendees()).thenReturn(attendees.stream()
                .map(user -> new EventUser(user.getName(), user.getEmail(), user.getLogin(), user.getOfficeId(), Decision.YES))
                .collect(Collectors.toList()));
        when(event.getSubscribers()).thenReturn(subscribers);
        when(event.getInstanceStartTs()).thenReturn("");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(DateRangeTest.AUG_26_2019_MON_12_00_MSK);
        c.add(Calendar.DAY_OF_MONTH, 1);
        when(event.getStart()).thenReturn(c.getTime());
        return event;
    }

    static EventUser toEventUser(Person person) {
        return new EventUser(person.getName().toString(Language.RUSSIAN), person.getEmail(Email.SourceType.STAFF), person.getLogin(), OFFICE_ID, Decision.YES);
    }

    static User toUser(Person person) {
        return new User(person.getName().toString(Language.RUSSIAN), person.getEmail(Email.SourceType.STAFF), person.getLogin(), OFFICE_ID);
    }

}
