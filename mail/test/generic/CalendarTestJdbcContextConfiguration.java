package ru.yandex.calendar.test.generic;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.calendar.boot.CalendarJdbcContextConfiguration;

/**
 * @author dbrylev
 */
@Configuration
@Import({
        CalendarJdbcContextConfiguration.class,
        CalendarEmbeddedPgContextConfiguration.class,
})
public class CalendarTestJdbcContextConfiguration {
}
