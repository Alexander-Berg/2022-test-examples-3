package ru.yandex.qe.mail.meetings.mocks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;

import static org.mockito.Mockito.mock;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
public class MockCalendarWebConfiguration {
    @Bean
    public CalendarWeb calendarWeb() {
        return mock(CalendarWeb.class);
    }
}
